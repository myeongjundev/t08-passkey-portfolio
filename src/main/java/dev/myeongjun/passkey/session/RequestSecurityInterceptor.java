package dev.myeongjun.passkey.session;

import dev.myeongjun.passkey.config.WebAuthnProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class RequestSecurityInterceptor implements HandlerInterceptor {

    private static final String CSRF_HEADER = "X-CSRF-Token";

    private final WebAuthnProperties properties;

    public RequestSecurityInterceptor(WebAuthnProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (isSafeMethod(request.getMethod())) {
            return true;
        }
        response.setHeader("Cache-Control", "no-store");

        if (!isJson(request.getContentType())) {
            reject(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "json_required");
            return false;
        }
        if (!properties.origin().toString().equals(request.getHeader("Origin"))) {
            reject(response, HttpServletResponse.SC_FORBIDDEN, "request_rejected");
            return false;
        }

        HttpSession session = request.getSession(false);
        Object stored = session == null ? null : session.getAttribute(SessionSecurityContext.CSRF_ATTRIBUTE);
        String supplied = request.getHeader(CSRF_HEADER);
        if (!(stored instanceof String expected) || supplied == null || !constantTimeEquals(expected, supplied)) {
            reject(response, HttpServletResponse.SC_FORBIDDEN, "request_rejected");
            return false;
        }
        return true;
    }

    private boolean isSafeMethod(String method) {
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }

    private boolean isJson(String contentType) {
        if (contentType == null) {
            return false;
        }
        try {
            return MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(contentType));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean constantTimeEquals(String expected, String supplied) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void reject(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + code + "\"}");
    }
}
