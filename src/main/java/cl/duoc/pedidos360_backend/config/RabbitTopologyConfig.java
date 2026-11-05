package cl.duoc.pedidos360_backend.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología de RabbitMQ que necesita el microservicio de PEDIDOS.
 *
 * <pre>
 *                          ┌─(pedido.stock)──> q.stock           (productos)
 *  pedidos ─> [pedidos.direct]
 *                          └─(pedido.ticket)─> q.ticket          (mensajería)
 *         ─> [eventos.topic] ─(pedido.creado)─> q.notificaciones (mensajería, patrón pedido.#)
 * </pre>
 *
 * Pedidos es solo PRODUCTOR: declara los dos exchanges en los que publica. Las colas, sus
 * DLQ y los bindings los declaran los consumidores (productos y mensajería). Declarar el
 * mismo exchange en varios servicios es seguro: RabbitMQ lo acepta si los parámetros
 * coinciden (tipo, durable, sin auto-delete).
 */
@Configuration
public class RabbitTopologyConfig {

    /** Direct: cada routing key llega exactamente a una cola (mensajes dirigidos). */
    @Bean
    DirectExchange pedidosExchange(TecnomarketRabbitProperties props) {
        return ExchangeBuilder.directExchange(props.exchanges().pedidos()).durable(true).build();
    }

    /** Topic: los consumidores se suscriben por patrón (ej. "pedido.#"), útil para difusión. */
    @Bean
    TopicExchange eventosExchange(TecnomarketRabbitProperties props) {
        return ExchangeBuilder.topicExchange(props.exchanges().eventos()).durable(true).build();
    }
}
