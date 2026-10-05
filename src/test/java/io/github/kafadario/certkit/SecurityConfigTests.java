package io.github.kafadario.certkit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTests {

	@Autowired
	MockMvc mvc;

	@Test
	void healthIsPublicAndReportsDatabaseUp() throws Exception {
		mvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void responsesCarryTheContentSecurityPolicy() throws Exception {
		mvc.perform(get("/actuator/health"))
				.andExpect(header().string("Content-Security-Policy", SecurityConfig.CONTENT_SECURITY_POLICY));
	}

	@Test
	void stateChangingRequestWithoutCsrfTokenIsRejected() throws Exception {
		mvc.perform(post("/actuator/health"))
				.andExpect(status().isForbidden());
	}

}
