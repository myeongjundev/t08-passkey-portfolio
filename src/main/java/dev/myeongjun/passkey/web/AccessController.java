package dev.myeongjun.passkey.web;

import dev.myeongjun.passkey.session.SessionSecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccessController {

    private final SessionSecurityContext sessionSecurityContext;

    public AccessController(SessionSecurityContext sessionSecurityContext) {
        this.sessionSecurityContext = sessionSecurityContext;
    }

    @GetMapping("/access")
    public String access(HttpServletRequest request, HttpServletResponse response, Model model) {
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("csrfToken", sessionSecurityContext.csrfToken(request));
        return "access";
    }
}
