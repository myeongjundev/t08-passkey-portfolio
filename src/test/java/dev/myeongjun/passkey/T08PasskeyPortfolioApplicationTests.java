package dev.myeongjun.passkey;

import dev.myeongjun.passkey.session.SessionAuthentication;
import dev.myeongjun.passkey.session.SessionPrincipal;
import dev.myeongjun.passkey.webauthn.WebAuthnVerificationAdapter;
import dev.myeongjun.passkey.account.Account;
import dev.myeongjun.passkey.account.AccountRepository;
import dev.myeongjun.passkey.privatearea.PrivateItem;
import dev.myeongjun.passkey.privatearea.PrivateItemCategory;
import dev.myeongjun.passkey.privatearea.PrivateItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class T08PasskeyPortfolioApplicationTests {

	private static final String PRIVATE_TITLE_1 = "합성 프로젝트 알파 출시 메모";
	private static final String PRIVATE_TITLE_2 = "합성 지원 기업 베타 목록";
	private static final String PRIVATE_TITLE_3 = "합성 주간 회고 감마";
	private static final String PRIVATE_BODY_1 = "샘플 API 계약 검토와 화면 점검 순서를 기록했습니다.";
	private static final String PRIVATE_BODY_2 = "가상 기업의 기술 스택과 지원 준비 항목을 정리했습니다.";
	private static final String PRIVATE_BODY_3 = "샘플 일정에서 배운 점과 다음 실험을 적었습니다.";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WebAuthnVerificationAdapter webAuthnVerificationAdapter;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private PrivateItemRepository privateItemRepository;

	@Test
	void webAuthn4jAdapterLoadsWithSpringBoot4Jackson3() {
		org.assertj.core.api.Assertions.assertThat(webAuthnVerificationAdapter).isNotNull();
	}

	@Test
	void hstsIsOnlyAddedToSecureResponses() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(header().doesNotExist("Strict-Transport-Security"));

		mockMvc.perform(get("/").secure(true))
				.andExpect(header().string(
						"Strict-Transport-Security",
						"max-age=31536000; includeSubDomains"
				));
	}

	@Test
	void publicPagesStayOpenAndContainNoPrivateItemsOrPasswordField() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
				.andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
				.andExpect(header().string("Referrer-Policy", "no-referrer"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("Cross-Origin-Opener-Policy", "same-origin"))
				.andExpect(header().string("Permissions-Policy", containsString("publickey-credentials-get=(self)")))
				.andExpect(content().string(containsString("백엔드의 구조를 설계하고")))
				.andExpect(content().string(containsString("공개 소개는 여기까지")))
				.andExpect(content().string(not(containsString(PRIVATE_TITLE_1))))
				.andExpect(content().string(not(containsString(PRIVATE_TITLE_2))))
				.andExpect(content().string(not(containsString(PRIVATE_TITLE_3))))
				.andExpect(content().string(not(containsString(PRIVATE_BODY_1))))
				.andExpect(content().string(not(containsString(PRIVATE_BODY_2))))
				.andExpect(content().string(not(containsString(PRIVATE_BODY_3))))
				.andExpect(content().string(not(containsString("type=\"password\""))));

		mockMvc.perform(get("/access"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("비밀번호 없는")))
				.andExpect(content().string(not(containsString("type=\"password\""))));
	}

	@Test
	void unauthenticatedRequestsAreRejectedWithoutLeakingPrivateContent() throws Exception {
		mockMvc.perform(get("/private"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(content().string(not(containsString(PRIVATE_TITLE_1))))
				.andExpect(content().string(not(containsString(PRIVATE_TITLE_2))))
				.andExpect(content().string(not(containsString(PRIVATE_TITLE_3))));

		mockMvc.perform(get("/api/private-items"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(content().json("{\"error\":\"passkey_session_required\"}"));
	}

	@Test
	void authenticatedSessionCanReadThreeSyntheticPrivateItems() throws Exception {
		Account account = accountRepository.save(Account.create(new byte[32], "합성 사용자", now()));
		privateItemRepository.saveAll(java.util.List.of(
				PrivateItem.create(account.id(), PrivateItemCategory.PROJECT, PRIVATE_TITLE_1, PRIVATE_BODY_1, 0, now()),
				PrivateItem.create(account.id(), PrivateItemCategory.TARGET, PRIVATE_TITLE_2, PRIVATE_BODY_2, 1, now()),
				PrivateItem.create(account.id(), PrivateItemCategory.RETROSPECTIVE, PRIVATE_TITLE_3, PRIVATE_BODY_3, 2, now())
		));
		MockHttpSession session = authenticatedSession(account.id());

		mockMvc.perform(get("/private").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(PRIVATE_TITLE_1)))
				.andExpect(content().string(containsString(PRIVATE_TITLE_2)))
				.andExpect(content().string(containsString(PRIVATE_TITLE_3)));

		mockMvc.perform(get("/api/private-items").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].title").value(PRIVATE_TITLE_1));
	}

	private MockHttpSession authenticatedSession(UUID accountId) {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(
				SessionAuthentication.PRINCIPAL_ATTRIBUTE,
				new SessionPrincipal(accountId, "합성 사용자")
		);
		return session;
	}

	private OffsetDateTime now() {
		return OffsetDateTime.now(ZoneOffset.UTC);
	}
}
