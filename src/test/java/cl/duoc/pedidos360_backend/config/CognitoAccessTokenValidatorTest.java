package cl.duoc.pedidos360_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class CognitoAccessTokenValidatorTest {

    private static final String CLIENT_ID = "mi-client-id";

    private final CognitoAccessTokenValidator validator = new CognitoAccessTokenValidator(CLIENT_ID);

    private Jwt token(String tokenUse, String clientId) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("token_use", tokenUse)
                .claim("client_id", clientId)
                .build();
    }

    @Test
    void aceptaAccessTokenConClientIdCorrecto() {
        assertThat(validator.validate(token("access", CLIENT_ID)).hasErrors()).isFalse();
    }

    @Test
    void rechazaClientIdDeOtraAplicacion() {
        assertThat(validator.validate(token("access", "otro-client")).hasErrors()).isTrue();
    }

    @Test
    void rechazaIdTokenUsadoComoAccessToken() {
        assertThat(validator.validate(token("id", CLIENT_ID)).hasErrors()).isTrue();
    }
}