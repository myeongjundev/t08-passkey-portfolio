package dev.myeongjun.passkey.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the public intro page carried over from T01.
 *
 * <p>This page is open to everyone: no account, no sign-in, no invite (T08-C03,
 * T08-C10). Private content has a separate protected route and template, so an
 * unauthenticated public response never carries it in its source (T08-C18).
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
