package cl.duoc.pedidos360_backend.controller;

import cl.duoc.pedidos360_backend.model.Pedido;
import cl.duoc.pedidos360_backend.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la matriz 200 / 201 / 204 / 400 / 401 / 403 del microservicio.
 * jwt() simula un token ya validado, así que aquí se prueba la AUTORIZACIÓN;
 * la validación de firma/issuer/client_id se cubre en
 * CognitoAccessTokenValidatorTest
 * y en la prueba manual con un token real de Cognito.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class PedidoSecurityTest {

    private static final GrantedAuthority READ = new SimpleGrantedAuthority("SCOPE_rs-api-pedidos/pedidos-read");
    private static final GrantedAuthority WRITE = new SimpleGrantedAuthority("SCOPE_rs-api-pedidos/pedidos-write");
    private static final GrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
    private static final String BODY_VALIDO = "{\"producto\":\"Notebook\",\"cantidad\":2}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PedidoRepository repository;

    @Test
    void sinToken_devuelve401ConCuerpoJson() throws Exception {
        mvc.perform(get("/api/pedidos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void conScopeRead_listaPedidos200() throws Exception {
        mvc.perform(get("/api/pedidos").with(jwt().authorities(READ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].estado").exists())
                .andExpect(jsonPath("$[0].producto").exists());
    }

    @Test
    void tokenValidoSinScope_devuelve403() throws Exception {
        mvc.perform(get("/api/pedidos").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void postConSoloScopeRead_devuelve403() throws Exception {
        mvc.perform(post("/api/pedidos").with(jwt().authorities(READ))
                .contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void postConScopeWrite_crea201YGuardaElSubDelToken() throws Exception {
        mvc.perform(post("/api/pedidos").with(jwt().authorities(WRITE))
                .contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.producto").value("Notebook"))
                .andExpect(jsonPath("$.clienteSub").value("user"));
    }

    @Test
    void postConCantidadInvalida_devuelve400() throws Exception {
        mvc.perform(post("/api/pedidos").with(jwt().authorities(WRITE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"producto\":\"Notebook\",\"cantidad\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteSinRolAdmin_devuelve403() throws Exception {
        Pedido pedido = repository.save(new Pedido("PENDIENTE", "Cable HDMI", 1, "test", LocalDateTime.now()));

        mvc.perform(delete("/api/pedidos/" + pedido.getId()).with(jwt().authorities(READ, WRITE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteConRolAdmin_devuelve204() throws Exception {
        Pedido pedido = repository.save(new Pedido("PENDIENTE", "Cable USB", 1, "test", LocalDateTime.now()));

        mvc.perform(delete("/api/pedidos/" + pedido.getId()).with(jwt().authorities(ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void healthEsPublico() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}