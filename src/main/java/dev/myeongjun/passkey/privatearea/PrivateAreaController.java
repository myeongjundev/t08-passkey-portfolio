package dev.myeongjun.passkey.privatearea;

import dev.myeongjun.passkey.session.SessionAuthentication;
import dev.myeongjun.passkey.session.SessionPrincipal;
import dev.myeongjun.passkey.session.SessionSecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import dev.myeongjun.passkey.credential.PasskeyManagementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@Controller
public class PrivateAreaController {

    private final PrivateAreaService privateAreaService;
    private final SessionSecurityContext sessionSecurityContext;
    private final PasskeyManagementService passkeyManagementService;

    public PrivateAreaController(PrivateAreaService privateAreaService,
                                 SessionSecurityContext sessionSecurityContext,
                                 PasskeyManagementService passkeyManagementService) {
        this.privateAreaService = privateAreaService;
        this.sessionSecurityContext = sessionSecurityContext;
        this.passkeyManagementService = passkeyManagementService;
    }

    @GetMapping("/private")
    public String privatePage(HttpSession session, HttpServletRequest request, Model model) {
        SessionPrincipal principal = SessionAuthentication.current(session).orElseThrow();
        model.addAttribute("displayName", principal.displayName());
        model.addAttribute("items", privateAreaService.findForAccount(principal.accountId()));
        model.addAttribute("csrfToken", sessionSecurityContext.csrfToken(request));
        model.addAttribute("passkeys", passkeyManagementService.list(principal.accountId()));
        return "private";
    }

    @ResponseBody
    @GetMapping("/api/private-items")
    public List<PrivateItemView> privateItems(HttpSession session) {
        SessionPrincipal principal = SessionAuthentication.current(session).orElseThrow();
        return privateAreaService.findForAccount(principal.accountId());
    }

    @ResponseBody
    @GetMapping("/api/private-items/{itemId}")
    public PrivateItemView privateItem(@PathVariable UUID itemId, HttpSession session) {
        SessionPrincipal principal = SessionAuthentication.current(session).orElseThrow();
        return privateAreaService.findOneForAccount(principal.accountId(), itemId);
    }
}
