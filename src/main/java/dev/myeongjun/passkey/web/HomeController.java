package dev.myeongjun.passkey.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the public intro page carried over from T01.
 *
 * <p>This page is open to everyone: no account, no sign-in, no invite (T08-C03,
 * T08-C10). The private area is rendered server-side into this same template only
 * once a passkey session exists, so an unauthenticated response never carries
 * private content in its source (T08-C18).
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
