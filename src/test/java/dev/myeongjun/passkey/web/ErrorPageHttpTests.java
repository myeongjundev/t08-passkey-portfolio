package dev.myeongjun.passkey.web;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A 500 was observed in production right after a real-device registration: the
 * ceremony had committed, but rendering /private failed once and a refresh
 * recovered it. Without a view of its own that surfaced Spring's Whitelabel page.
 * These tests fix the replacement's two obligations -- say nothing about the
 * server, and leave the JSON boundary the API callers rely on untouched.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorPageHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void browsersGetThePageInsteadOfTheWhitelabelScreen() throws Exception {
        String body = mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("잠시 문제가");
        assertThat(body).contains("새로고침");
        assertThat(body).doesNotContain("Whitelabel");
    }

    @Test
    void thePageNamesNoServerInternals() throws Exception {
        String body = mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("db down"))
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "db down"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("db down");
        assertThat(body).doesNotContain("IllegalStateException");
        assertThat(body).doesNotContain("dev.myeongjun.passkey");
        assertThat(body).doesNotContain("java.");
    }

    @Test
    void theErrorViewCarriesNoPrivateContent() throws Exception {
        String body = mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("합성 프로젝트");
        assertThat(body).doesNotContain("합성 지원 기업");
        assertThat(body).doesNotContain("합성 주간 회고");
    }

    @Test
    void theUnauthenticatedPrivateBoundaryStaysJson() throws Exception {
        mockMvc.perform(get("/private").accept(MediaType.TEXT_HTML))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("passkey_session_required"));

        mockMvc.perform(get("/api/private-items").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("passkey_session_required"));
    }
}
