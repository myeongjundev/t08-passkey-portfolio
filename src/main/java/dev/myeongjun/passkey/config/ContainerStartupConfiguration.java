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
                if (wantsHtml(request)) {
                    // A visitor arriving during a cold start would otherwise read a line of
                    // plain text and decide the site is broken. The page reloads itself, so
                    // the wait costs them nothing but the seconds the JVM needs.
                    response.setContentType("text/html;charset=UTF-8");
                    response.getWriter().write(STARTING_PAGE);
                } else {
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().write("Service is starting. Please retry shortly.");
                }
                return;
            }
            getNext().invoke(request, response);
        }

        /** Only a browser navigation gets the page; fetch, curl and the APIs keep the text. */
        private static boolean wantsHtml(Request request) {
            String accept = request.getHeader("Accept");
            return accept != null && accept.contains("text/html");
        }
    }

    /**
     * Self-refreshing so the visitor lands on the real page without touching anything.
     * The interval matches the Retry-After header; startup measured 3.8s packaged, and
     * container provisioning adds to that, so a couple of reloads is the normal case.
     */
    private static final String STARTING_PAGE = """
            <!doctype html>
            <html lang="ko">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta http-equiv="refresh" content="2">
            <meta name="theme-color" content="#f3f0e9">
            <title>깨우는 중 — 김명준</title>
            <style>
              :root { color-scheme: light dark; }
              body { margin: 0; min-height: 100vh; display: grid; place-items: center;
                     background: #f3f0e9; color: #2b2823;
                     font: 400 16px/1.7 system-ui, -apple-system, "Segoe UI", sans-serif; }
              main { max-width: 30rem; padding: 2rem; text-align: center; }
              h1 { font-size: 1.25rem; font-weight: 600; margin: 0 0 .75rem; letter-spacing: -.01em; }
              p { margin: 0; color: #6b6459; }
              .dot { display: inline-block; width: .5rem; height: .5rem; margin-right: .5rem;
                     border-radius: 50%; background: #b8863b; animation: pulse 1.4s ease-in-out infinite; }
              @keyframes pulse { 0%, 100% { opacity: .3 } 50% { opacity: 1 } }
              @media (prefers-color-scheme: dark) {
                body { background: #1a1815; color: #e8e3d9; }
                p { color: #9a9285; }
              }
              @media (prefers-reduced-motion: reduce) { .dot { animation: none; opacity: .8 } }
            </style>
            </head>
            <body>
            <main>
              <h1><span class="dot"></span>서버를 깨우는 중입니다</h1>
              <p>이 페이지는 몇 초 뒤 자동으로 다시 열립니다. 그대로 두세요.</p>
            </main>
            </body>
            </html>
            """;
}
