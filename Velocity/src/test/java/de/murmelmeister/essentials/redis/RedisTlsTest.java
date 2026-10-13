package de.murmelmeister.essentials.redis;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.protocol.ProtocolVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class RedisTlsTest {
    @TempDir
    static Path directory;
    private static Path store;
    private static Path certificate;
    private static Path key;
    private static final String PASSWORD = "test-password";

    @BeforeAll
    static void certificates() throws Exception {
        store = directory.resolve("client.p12");
        Path keytool = Path.of(System.getProperty("java.home"), "bin", "keytool"
                + (System.getProperty("os.name").startsWith("Windows") ? ".exe" : ""));
        Process process = new ProcessBuilder(keytool.toString(), "-genkeypair", "-alias", "test",
                "-keyalg", "RSA", "-keysize", "2048", "-storetype", "PKCS12", "-keystore", store.toString(),
                "-storepass", PASSWORD, "-keypass", PASSWORD, "-dname", "CN=localhost",
                "-ext", "SAN=dns:localhost", "-validity", "2", "-noprompt").redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
        KeyStore keys = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(store)) { keys.load(input, PASSWORD.toCharArray()); }
        certificate = directory.resolve("ca.crt");
        key = directory.resolve("client.key");
        Files.writeString(certificate, pem("CERTIFICATE", keys.getCertificate("test").getEncoded()));
        Files.writeString(key, pem("PRIVATE KEY", keys.getKey("test", PASSWORD.toCharArray()).getEncoded()));
    }

    private static String pem(String type, byte[] encoded) {
        return "-----BEGIN " + type + "-----\n" + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded)
                + "\n-----END " + type + "-----\n";
    }

    private RedisConfig config(String host, int port, boolean pem, boolean identity, boolean trust) {
        RedisConfig.TlsSettings settings = new RedisConfig.TlsSettings(
                pem && trust ? certificate : null, pem && identity ? certificate : null,
                pem && identity ? key : null, "", !pem && trust ? store : null, PASSWORD,
                !pem && identity ? store : null, PASSWORD, "PKCS12");
        return new RedisConfig(true, host, port, "default", "", true, "refresh", "lobby", settings);
    }

    private RedisClient client(RedisConfig config) {
        RedisClient client = RedisClient.create(config.uri());
        client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2)
                .autoReconnect(false).sslOptions(config.sslOptions()).build());
        return client;
    }

    @Test
    void mutualTlsWithPkcs12AndPem() throws Exception {
        for (boolean pem : new boolean[]{false, true}) {
            try (TlsEndpoint endpoint = new TlsEndpoint(true)) {
                RedisClient client = client(config("localhost", endpoint.port(), pem, true, true));
                try (var connection = client.connect()) {
                    assertEquals("PONG", connection.sync().ping());
                    assertTrue(endpoint.clientAuthenticated.get());
                } finally { client.shutdown(); }
            }
        }
    }

    @Test
    void serverTlsWorksWithoutClientCertificateWhenServerAllowsIt() throws Exception {
        try (TlsEndpoint endpoint = new TlsEndpoint(false)) {
            RedisClient client = client(config("localhost", endpoint.port(), true, false, true));
            try (var connection = client.connect()) { assertEquals("PONG", connection.sync().ping()); }
            finally { client.shutdown(); }
        }
    }

    @Test
    void missingIdentityWrongHostnameAndUntrustedCertificateAreRejected() throws Exception {
        for (int scenario = 0; scenario < 3; scenario++) {
            try (TlsEndpoint endpoint = new TlsEndpoint(true)) {
                RedisConfig config = config(scenario == 1 ? "127.0.0.1" : "localhost", endpoint.port(),
                        true, scenario != 0, scenario != 2);
                RedisClient client = client(config);
                try { assertThrows(RuntimeException.class, client::connect); }
                finally { client.shutdown(); }
            }
        }
    }

    @Test
    void relativePathsAndPasswordsLoadAndInvalidPairsFail() throws Exception {
        Files.writeString(directory.resolve("redis.properties"), "ssl=true\ntls.ca-cert=ca.crt\n"
                + "tls.client-cert=ca.crt\ntls.client-key=client.key\ntls.client-key-password=key-secret\n");
        RedisConfig config = RedisConfig.load(directory);
        assertEquals(certificate.toAbsolutePath(), config.tls().caCert());
        assertEquals(key.toAbsolutePath(), config.tls().clientKey());
        assertFalse(config.tls().toString().contains("key-secret"));
        var bad = new RedisConfig.TlsSettings(certificate, certificate, null, "", null, "", null, "", "PKCS12");
        assertThrows(IllegalArgumentException.class, bad::validate);
        var conflicting = new RedisConfig.TlsSettings(certificate, null, null, "", store, PASSWORD, null, "", "PKCS12");
        assertThrows(IllegalArgumentException.class, conflicting::validate);
    }

    /** Minimal RESP endpoint: actual TLS handshake and client authentication, no external Redis needed. */
    private static final class TlsEndpoint implements AutoCloseable {
        private final SSLServerSocket server;
        private volatile SSLSocket socket;
        private final Thread thread;
        private final AtomicBoolean clientAuthenticated = new AtomicBoolean();

        TlsEndpoint(boolean mutual) throws Exception {
            KeyStore keys = KeyStore.getInstance("PKCS12");
            try (InputStream input = Files.newInputStream(store)) { keys.load(input, PASSWORD.toCharArray()); }
            KeyManagerFactory km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            km.init(keys, PASSWORD.toCharArray());
            TrustManagerFactory tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tm.init(keys);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(km.getKeyManagers(), tm.getTrustManagers(), null);
            server = (SSLServerSocket) context.getServerSocketFactory().createServerSocket(0, 1,
                    InetAddress.getByName("127.0.0.1"));
            server.setNeedClientAuth(mutual);
            server.setSoTimeout(10000);
            thread = new Thread(() -> {
                try {
                    socket = (SSLSocket) server.accept();
                    socket.setSoTimeout(10000);
                    socket.startHandshake();
                    if (mutual) clientAuthenticated.set(socket.getSession().getPeerPrincipal() != null);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        int count = Integer.parseInt(line.substring(1));
                        String command = null;
                        for (int index = 0; index < count; index++) {
                            reader.readLine();
                            String argument = reader.readLine();
                            if (index == 0) command = argument;
                        }
                        socket.getOutputStream().write(("PING".equalsIgnoreCase(command) ? "+PONG\r\n" : "+OK\r\n")
                                .getBytes(StandardCharsets.UTF_8));
                        socket.getOutputStream().flush();
                    }
                } catch (IOException ignored) {
                    // Rejected TLS handshakes and socket closure are expected in the negative cases.
                }
            }, "redis-tls-test");
            thread.setDaemon(true);
            thread.start();
        }

        int port() { return server.getLocalPort(); }

        @Override
        public void close() throws Exception {
            server.close();
            if (socket != null) socket.close();
            thread.join(Duration.ofSeconds(3).toMillis());
            assertFalse(thread.isAlive());
        }
    }
}
