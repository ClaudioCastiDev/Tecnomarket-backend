package cl.duoc.pedidos360_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CognitoAuthoritiesConverterTest {

    private final CognitoAuthoritiesConverter converter = new CognitoAuthoritiesConverter();

    @Test
    void convierteScopesYGruposEnAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("scope", "openid rs-api-pedidos/pedidos-read")
                .claim("cognito:groups", List.of("admin"))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .contains("SCOPE_rs-api-pedidos/pedidos-read", "ROLE_ADMIN");
    }

    @Test
    void sinGruposSoloDevuelveScopes() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("scope", "rs-api-pedidos/pedidos-read")
                .build();

        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactly("SCOPE_rs-api-pedidos/pedidos-read");
    }
}