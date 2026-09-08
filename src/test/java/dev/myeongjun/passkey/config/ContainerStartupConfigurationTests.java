package dev.myeongjun.passkey.config;

import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "t08.server.bind-on-init=true",
        "spring.datasource.url=jdbc:h2:mem:early-bind;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@Import(ContainerStartupConfigurationTests.StartupProbe.class)
class ContainerStartupConfigurationTests {

    static final AtomicBoolean observedBoundButNotServing = new AtomicBoolean();

    @Autowired
    WebServerApplicationContext context;

    @Test
    void earlySocketDoesNotServePrivateContentBeforeOrAfterStartup() throws Exception {
        assertThat(observedBoundButNotServing).isTrue();
        try (HttpClient client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder(
                    URI.create("http://localhost:" + context.getWebServer().getPort() + "/private"))
                    .timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class StartupProbe {
        @Bean
        @Lazy(false)
        SmartInitializingSingleton socketProbe(WebServerApplicationContext context) {
            return () -> {
                int port = context.getWebServer().getPort();
                try {
                    // Before Spring is ready, the socket is reachable but only the
                    // startup gate may respond, never application/private content.
                    String api = earlyRequest(port, "GET /private HTTP/1.1\r\n"
                            + "Host: localhost\r\nConnection: close\r\n\r\n");
                    assertThat(api).startsWith("HTTP/1.1 503")
                            .contains("Cache-Control: no-store")
                            .endsWith("Service is starting. Please retry shortly.");

                    // A browser gets a page that reloads itself rather than that line, so a
                    // visitor arriving during a cold start is not left deciding whether the
                    // site is broken. It stays a 503 and still carries no private content.
                    String browser = earlyRequest(port, "GET / HTTP/1.1\r\n"
                            + "Host: localhost\r\nAccept: text/html,application/xhtml+xml\r\n"
                            + "Connection: close\r\n\r\n");
                    assertThat(browser).startsWith("HTTP/1.1 503")
                            .contains("Cache-Control: no-store")
                            .contains("Content-Type: text/html;charset=UTF-8")
                            .contains("http-equiv=\"refresh\"")
                            .doesNotContain("Service is starting. Please retry shortly.");

                    observedBoundButNotServing.set(true);
                } catch (Exception exception) {
                    throw new IllegalStateException("Early socket boundary check failed", exception);
                }
            };
        }

        /** Speaks HTTP over a raw socket: the gate only answers before Spring is ready. */
        private static String earlyRequest(int port, String request) throws Exception {
            try (Socket socket = new Socket("localhost", port)) {
                socket.setSoTimeout(2000);
                socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().flush();
                return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }
}
