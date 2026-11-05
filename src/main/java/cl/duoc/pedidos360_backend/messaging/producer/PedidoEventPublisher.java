package cl.duoc.pedidos360_backend.messaging.producer;

import cl.duoc.pedidos360_backend.config.TecnomarketRabbitProperties;
import cl.duoc.pedidos360_backend.messaging.event.PedidoCreadoEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Productor de eventos del dominio pedidos. Es la ÚNICA clase de pedidos que
 * conoce RabbitMQ: el servicio de negocio solo llama a
 * {@link #publicarPedidoCreado}.
 * <p>
 * Un pedido creado se publica tres veces, una por caso de uso:
 * <table>
 * <tr><th>Exchange</th><th>Routing key</th><th>Cola</th><th>Quién la consume</th></tr>
 * <tr><td>pedidos (direct)</td><td>pedido.stock</td><td>q.stock</td><td>productos</td></tr>
 * <tr><td>pedidos (direct)</td><td>pedido.ticket</td><td>q.ticket</td><td>mensajería</td></tr>
 * <tr><td>eventos (topic)</td><td>pedido.creado</td><td>q.notificaciones</td><td>mensajería</td></tr>
 * </table>
 * Las tres copias llevan el mismo messageId, porque representan el mismo evento.
 * <p>
 * Manejo de errores al publicar:
 * <ul>
 * <li>Cada evento lleva un messageId único (UUID). Los consumidores pueden
 * usarlo para detectar duplicados.</li>
 * <li>Confirmación del broker (publisher confirm): si RabbitMQ rechaza el
 * mensaje (nack), se registra en el log con el id del pedido.</li>
 * <li>Mensaje sin destino (publisher return): si ninguna cola está enlazada
 * al exchange con esa routing key, el broker lo devuelve y se registra. Así se
 * detecta, por ejemplo, un nombre mal escrito en el YAML.</li>
 * <li>Broker caído: se registra el error y NO se propaga. El pedido ya quedó
 * guardado, y fallar la petición HTTP haría creer al cliente que no se creó.
 * Mejora futura: patrón outbox para reenviar los eventos pendientes.</li>
 * </ul>
 */
@Component
public class PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final TecnomarketRabbitProperties props;

    public PedidoEventPublisher(RabbitTemplate rabbitTemplate, TecnomarketRabbitProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    @PostConstruct
    void registrarCallbacks() {
        rabbitTemplate.setReturnsCallback(returned -> log.warn(
                "Evento sin cola de destino: exchange={} routingKey={} motivo={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        rabbitTemplate.setConfirmCallback((correlation, ack, causa) -> {
            String id = correlation != null ? correlation.getId() : "?";
            if (ack) {
                log.debug("Broker confirmó el evento {}", id);
            } else {
                log.error("Broker rechazó el evento {}: {}", id, causa);
            }
        });
    }

    public void publicarPedidoCreado(PedidoCreadoEvent evento) {
        String messageId = UUID.randomUUID().toString();
        var exchanges = props.exchanges();
        var keys = props.routingKeys();
        enviar(exchanges.pedidos(), keys.stock(), evento, messageId);
        enviar(exchanges.pedidos(), keys.ticket(), evento, messageId);
        enviar(exchanges.eventos(), keys.pedidoCreado(), evento, messageId);
    }

    /** Cada envío es independiente: si falla uno, los demás se intentan igual. */
    private void enviar(String exchange, String routingKey, PedidoCreadoEvent evento, String messageId) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, evento,
                    mensaje -> {
                        mensaje.getMessageProperties().setMessageId(messageId);
                        return mensaje;
                    },
                    new CorrelationData("pedido-" + evento.pedidoId() + "/" + routingKey + "/" + messageId));
            log.info("Evento publicado: exchange={} routingKey={} pedidoId={} messageId={}",
                    exchange, routingKey, evento.pedidoId(), messageId);
        } catch (AmqpException e) {
            log.error("No se pudo publicar {} del pedido {}: {}", routingKey, evento.pedidoId(), e.getMessage());
        }
    }
}
