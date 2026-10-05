package io.github.kafadario.certkit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * certkit has no login (see docs/security.md). Spring Security is here for
 * CSRF protection and security headers only.
 */
@Configuration
class SecurityConfig {

	static final String CONTENT_SECURITY_POLICY =
			"default-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// Replaces Boot's default chain, which would demand a login for every request.
				.authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
				// CSRF protection is left at its default: required on every state-changing request.
				.headers(headers -> headers
						.contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY)));
		return http.build();
	}

	/**
	 * Without a UserDetailsService, Boot creates a default user and logs its generated
	 * password. certkit has no users, so it gets an empty one instead.
	 */
	@Bean
	UserDetailsService noUsers() {
		return new InMemoryUserDetailsManager();
	}

}
