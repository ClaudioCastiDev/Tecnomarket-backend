package cl.duoc.pedidos360_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
public class SecurityConfig {

    /** Scopes definidos en el resource server "rs-api-pedidos" de Cognito. */
    static final String SCOPE_READ = "SCOPE_rs-api-pedidos/pedidos-read";
    static final String SCOPE_WRITE = "SCOPE_rs-api-pedidos/pedidos-write";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter,
            RestSecurityErrorHandler errorHandler) throws Exception {
        http
                // API stateless con Bearer token: no hay sesión ni cookies, por lo que CSRF no
                // aplica.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pedidos", "/api/pedidos/**").hasAuthority(SCOPE_READ)
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").hasAuthority(SCOPE_WRITE)
                        .requestMatchers(HttpMethod.DELETE, "/api/pedidos/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> {
                    oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter));
                    oauth.authenticationEntryPoint(errorHandler);
                    oauth.accessDeniedHandler(errorHandler);
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));
        return http.build();
    }

    /**
     * Decoder del JWT de Cognito: verifica la firma con las claves públicas (JWKS)
     * del user pool
     * y valida vigencia, issuer, token_use y client_id. Las claves se descargan al
     * recibir el primer token.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${app.security.issuer-uri}") String issuerUri,
            @Value("${app.security.client-id}") String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(issuerUri + "/.well-known/jwks.json").build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(List.<OAuth2TokenValidator<Jwt>>of(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                new CognitoAccessTokenValidator(clientId))));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new CognitoAuthoritiesConverter());
        return converter;
    }
}