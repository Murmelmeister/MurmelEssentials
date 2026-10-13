package de.murmelmeister.essentials.redis;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import de.murmelmeister.murmelapi.utils.update.RefreshOrigin;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisRefreshTest {
    private final Gson gson = new Gson();

    @Test
    @SuppressWarnings("unchecked")
    void bridgePublishesLocalEventsAndAppliesRemoteEventsWithoutLoops() throws Exception {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> publisher = mock(StatefulRedisConnection.class);
        StatefulRedisPubSubConnection<String, String> subscriber = mock(StatefulRedisPubSubConnection.class);
        RedisCommands<String, String> commands = mock(RedisCommands.class);
        RedisPubSubCommands<String, String> subscription = mock(RedisPubSubCommands.class);
        when(client.connect()).thenReturn(publisher);
        when(client.connectPubSub()).thenReturn(subscriber);
        when(publisher.sync()).thenReturn(commands);
        when(subscriber.sync()).thenReturn(subscription);
        RedisConfig config = new RedisConfig(true, "localhost", 6379, "default", "", false, "refresh", "lobby");
        RefreshProvider provider = RefreshProvider.of();
        CountDownLatch applied = new CountDownLatch(1);
        try (RedisRefreshBridge bridge = new RedisRefreshBridge(config, provider, mock(Logger.class), gson,
                message -> applied.countDown(), () -> client)) {
            bridge.register();
            ArgumentCaptor<RedisPubSubListener<String, String>> callbacks = ArgumentCaptor.forClass(RedisPubSubListener.class);
            verify(subscriber, timeout(3000)).addListener(callbacks.capture());
            verify(subscription, timeout(3000)).subscribe("refresh");
            RedisPubSubListener<String, String> callback = callbacks.getValue();
            provider.fireCache("parents");
            verify(commands, timeout(3000)).publish(eq("refresh"), anyString());
            ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
            verify(commands).publish(eq("refresh"), payload.capture());
            assertEquals("lobby", RefreshMessage.decode(payload.getValue(), gson).sender());

            CountDownLatch refreshed = new CountDownLatch(1);
            provider.register(event -> {
                if (event.origin() == RefreshOrigin.REMOTE && event.type().equals("all")) refreshed.countDown();
            });
            callback.subscribed("refresh", 1);
            assertTrue(refreshed.await(3, TimeUnit.SECONDS));
            callback.message("refresh", "{\"sender\":\"lobby\",\"type\":\"parents\"}");
            callback.message("refresh", "{\"sender\":\"proxy\",\"type\":\"parents\"}");
            assertTrue(applied.await(3, TimeUnit.SECONDS));
            verify(commands, times(1)).publish(eq("refresh"), anyString());
        }
        provider.fireCache("parents");
        verify(commands, times(1)).publish(eq("refresh"), anyString());
        verify(client).shutdown();
    }

    @Test
    void structuredParentKeyKeepsItsJsonRepresentationAndRemoteOrigin() {
        RefreshProvider provider = mock(RefreshProvider.class);
        String key = "{\"targetId\":42,\"groupId\":3}";
        RefreshMessage original = new RefreshMessage("citybuild", "single_parent", JsonParser.parseString(key));
        RefreshMessage decoded = RefreshMessage.decode(gson.toJson(original), gson);
        decoded.apply(provider, gson);
        verify(provider).fireSingle("single_parent", key, RefreshOrigin.REMOTE);
        verifyNoMoreInteractions(provider);
    }

    @Test
    void absentAndNullKeysRefreshTheWholeCache() {
        RefreshProvider provider = mock(RefreshProvider.class);
        RefreshMessage.decode("{\"sender\":\"proxy\",\"type\":\"parents\"}", gson).apply(provider, gson);
        RefreshMessage.decode("{\"sender\":\"proxy\",\"type\":\"parents\",\"key\":null}", gson).apply(provider, gson);
        verify(provider, times(2)).fireCache("parents", RefreshOrigin.REMOTE);
    }

    @Test
    void invalidMessagesCannotReachTheProviders() {
        for (String json : new String[]{"null", "[]", "{}", "{\"sender\":2,\"type\":\"parents\"}",
                "{\"sender\":\"\",\"type\":\"parents\"}"}) {
            assertThrows(RuntimeException.class, () -> RefreshMessage.decode(json, gson));
        }
    }

    @Test
    void unknownFieldsAreIgnoredForForwardCompatibility() {
        RefreshMessage message = RefreshMessage.decode(
                "{\"sender\":\"server-1\",\"type\":\"parents\",\"version\":2}", gson);
        assertEquals("parents", message.type());
    }

    @Test
    void invalidBooleansAreRejectedInsteadOfDisablingSecurity(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("redis.properties"), "enabled=true\nssl=treu\n");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> RedisConfig.load(directory));
        assertTrue(exception.getMessage().contains("ssl must be true or false"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shutdownDrainsPublicationsAndCleansAllResourcesEvenWhenOneCloseFails() throws Exception {
        RedisClient client = mock(RedisClient.class);
        StatefulRedisConnection<String, String> publisher = mock(StatefulRedisConnection.class);
        StatefulRedisPubSubConnection<String, String> subscriber = mock(StatefulRedisPubSubConnection.class);
        RedisCommands<String, String> commands = mock(RedisCommands.class);
        RedisPubSubCommands<String, String> subscription = mock(RedisPubSubCommands.class);
        when(client.connect()).thenReturn(publisher);
        when(client.connectPubSub()).thenReturn(subscriber);
        when(publisher.sync()).thenReturn(commands);
        when(subscriber.sync()).thenReturn(subscription);
        doThrow(new IllegalStateException("test")).when(subscriber).close();
        RefreshProvider provider = mock(RefreshProvider.class);
        RedisConfig config = new RedisConfig(true, "localhost", 6379, "default", "", false, "refresh", "server-1");
        RedisRefreshBridge bridge = new RedisRefreshBridge(config, provider, mock(Logger.class), gson,
                message -> {}, () -> client);
        CountDownLatch publishing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(commands.publish(anyString(), anyString())).thenAnswer(invocation -> {
            publishing.countDown();
            assertTrue(release.await(3, TimeUnit.SECONDS));
            return 1L;
        });
        try {
            bridge.register();
            verify(subscription, timeout(3000)).subscribe("refresh");
            var listeners = ArgumentCaptor.forClass(de.murmelmeister.murmelapi.utils.update.RefreshListener.class);
            verify(provider).register(listeners.capture());
            var event = mock(de.murmelmeister.murmelapi.utils.update.RefreshEvent.class);
            when(event.origin()).thenReturn(RefreshOrigin.LOCAL);
            when(event.type()).thenReturn("parents");
            listeners.getValue().onRefresh(event);
            assertTrue(publishing.await(3, TimeUnit.SECONDS));
            listeners.getValue().onRefresh(event);
            Thread releaser = Thread.ofVirtual().start(() -> {
                try { verify(provider, timeout(3000)).unregister(any()); }
                finally { release.countDown(); }
            });
            Thread.currentThread().interrupt();
            bridge.close();
            assertTrue(Thread.interrupted(), "Shutdown must restore the caller's interrupt status.");
            releaser.join();
            verify(commands, times(2)).publish(eq("refresh"), anyString());
            verify(publisher).close();
            verify(client).shutdown();
            bridge.close();
            verify(client, times(1)).shutdown();
            assertThrows(IllegalStateException.class, bridge::register);
        } finally {
            Thread.interrupted();
            release.countDown();
            bridge.close();
        }
    }

    @Test
    void generatedIdentityIsPersistentAndDifferentPerServer(@TempDir Path directory) {
        RedisConfig first = RedisConfig.load(directory.resolve("lobby"));
        assertFalse(first.enabled());
        assertEquals(first.serverId(), RedisConfig.load(directory.resolve("lobby")).serverId());
        assertNotEquals(first.serverId(), RedisConfig.load(directory.resolve("citybuild")).serverId());
    }

    @Test
    void credentialsAndTlsAreLoadedWithoutLeakingThePassword(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("redis.properties"), "enabled=true\nhost=redis.internal\nport=6380\n"
                + "username=murmel\npassword=secret\nssl=true\nchannel=network:refresh\nserver-id=lobby\n");
        RedisConfig config = RedisConfig.load(directory);
        assertTrue(config.enabled());
        assertTrue(config.uri().isSsl());
        assertEquals(6380, config.uri().getPort());
        var credentials = config.uri().getCredentialsProvider().resolveCredentials().block();
        assertNotNull(credentials);
        assertEquals("murmel", credentials.getUsername());
        assertArrayEquals("secret".toCharArray(), credentials.getPassword());
        assertFalse(config.toString().contains("secret"));
    }
}
