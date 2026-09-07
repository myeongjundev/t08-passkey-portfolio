package dev.myeongjun.passkey.config;

import java.io.IOException;
import jakarta.servlet.ServletException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reserve the socket early, while rejecting all HTTP until Spring is ready. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "t08.server.bind-on-init", havingValue = "true")
public class ContainerStartupConfiguration implements ApplicationListener<ApplicationReadyEvent> {

    private final StartupGate gate = new StartupGate();

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> bindContainerSocketOnInit() {
        return factory -> {
            factory.addConnectorCustomizers(connector -> connector.setProperty("bindOnInit", "true"));
            factory.addEngineValves(gate);
        };
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        gate.ready = true;
    }

    static final class StartupGate extends ValveBase {
        private volatile boolean ready;

        StartupGate() {
            super(true);
        }

        @Override
        public void invoke(Request request, Response response) throws IOException, ServletException {
            if (!ready) {
                response.setStatus(503);
                response.setHeader("Cache-Control", "no-store");
                response.setHeader("Retry-After", "2");
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("Service is starting. Please retry shortly.");
                return;
            }
            getNext().invoke(request, response);
        }
    }
}
