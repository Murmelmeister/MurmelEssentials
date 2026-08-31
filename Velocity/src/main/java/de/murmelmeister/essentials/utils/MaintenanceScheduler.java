package de.murmelmeister.essentials.utils;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;
import de.murmelmeister.essentials.MurmelEssentials;
import de.murmelmeister.murmelapi.maintenance.Maintenance;
import de.murmelmeister.murmelapi.maintenance.MaintenanceProvider;
import de.murmelmeister.murmelapi.maintenance.MaintenanceType;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class MaintenanceScheduler {
    private final MurmelEssentials plugin;
    private final ProxyServer server;
    private final Logger logger;
    private final MaintenanceProvider maintenanceProvider;
    private final AtomicBoolean updating = new AtomicBoolean();

    private ScheduledTask task;

    public MaintenanceScheduler(MurmelEssentials plugin, ProxyServer server, Logger logger) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.maintenanceProvider = plugin.getMaintenanceProvider();
    }

    public void start() {
        stop();
        task = server.getScheduler()
                .buildTask(plugin, this::updateStatuses)
                .repeat(1, TimeUnit.SECONDS)
                .schedule();
    }

    public void stop() {
        if (task != null && task.status() != TaskStatus.CANCELLED)
            task.cancel();
        task = null;
    }

    private void updateStatuses() {
        if (!updating.compareAndSet(false, true))
            return;

        try {
            LocalDateTime now = LocalDateTime.now();

            maintenanceProvider.findAll().stream()
                    .filter(maintenance -> maintenance.status() == MaintenanceType.ACTIVE)
                    .filter(maintenance -> !now.isBefore(maintenance.endAt()))
                    .forEach(maintenance -> updateStatus(maintenance, MaintenanceType.ENDED));

            boolean activeMaintenanceExists = maintenanceProvider.findAll().stream()
                    .anyMatch(maintenance -> maintenance.status() == MaintenanceType.ACTIVE
                            && now.isBefore(maintenance.endAt()));

            maintenanceProvider.findAll().stream()
                    .filter(maintenance -> maintenance.status() == MaintenanceType.PLANNED)
                    .filter(maintenance -> !now.isBefore(maintenance.endAt()))
                    .forEach(maintenance -> updateStatus(maintenance, MaintenanceType.ENDED));

            if (activeMaintenanceExists)
                return;

            maintenanceProvider.findAll().stream()
                    .filter(maintenance -> maintenance.status() == MaintenanceType.PLANNED)
                    .filter(maintenance -> !now.isBefore(maintenance.startAt()))
                    .filter(maintenance -> now.isBefore(maintenance.endAt()))
                    .min(Comparator.comparing(Maintenance::startAt))
                    .ifPresent(maintenance -> updateStatus(maintenance, MaintenanceType.ACTIVE));
        } catch (RuntimeException exception) {
            logger.error("Failed to update maintenance statuses.", exception);
        } finally {
            updating.set(false);
        }
    }

    private void updateStatus(Maintenance maintenance, MaintenanceType status) {
        maintenanceProvider.update(
                maintenance.id(),
                CONSOLE_USER_ID,
                builder -> builder.status(status)
        ).orElseThrow(() -> new IllegalStateException(
                "Could not update maintenance #" + maintenance.id() + " to " + status
        ));

        logger.info("Updated maintenance #{} from {} to {}.", maintenance.id(), maintenance.status(), status);
    }
}
