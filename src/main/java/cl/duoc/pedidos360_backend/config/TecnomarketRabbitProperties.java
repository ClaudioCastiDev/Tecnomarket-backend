package cl.duoc.pedidos360_backend.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nombres de exchanges y routing keys leídos desde el bloque {@code tecnomarket.rabbit}
 * del application.yml (los mismos que usan productos y mensajería).
 * <p>
 * Así ningún nombre de RabbitMQ queda escrito a mano en el código. Si falta un valor,
 * la aplicación no arranca (@Validated + @NotBlank).
 */
@Validated
@ConfigurationProperties(prefix = "tecnomarket.rabbit")
public record TecnomarketRabbitProperties(
        @Valid @NotNull Exchanges exchanges,
        @Valid @NotNull RoutingKeys routingKeys) {

    /** pedidos: exchange direct (stock, ticket). eventos: exchange topic (notificaciones). */
    public record Exchanges(@NotBlank String pedidos, @NotBlank String eventos) {
    }

    public record RoutingKeys(@NotBlank String stock, @NotBlank String ticket, @NotBlank String pedidoCreado) {
    }
}
