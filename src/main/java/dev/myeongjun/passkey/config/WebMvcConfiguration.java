package dev.myeongjun.passkey.config;

import dev.myeongjun.passkey.session.AuthenticationInterceptor;
import dev.myeongjun.passkey.session.RequestSecurityInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;
    private final RequestSecurityInterceptor requestSecurityInterceptor;

    public WebMvcConfiguration(
            AuthenticationInterceptor authenticationInterceptor,
            RequestSecurityInterceptor requestSecurityInterceptor
    ) {
        this.authenticationInterceptor = authenticationInterceptor;
        this.requestSecurityInterceptor = requestSecurityInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestSecurityInterceptor)
                .addPathPatterns("/api/**")
                .order(0);
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/private", "/private/**", "/api/private-items", "/api/private-items/**", "/api/session/**", "/api/passkeys/**");
    }
}
