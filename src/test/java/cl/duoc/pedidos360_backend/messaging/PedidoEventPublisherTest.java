package cl.duoc.pedidos360_backend.messaging;

import cl.duoc.pedidos360_backend.config.TecnomarketRabbitProperties;
import cl.duoc.pedidos360_backend.messaging.event.PedidoCreadoEvent;
import cl.duoc.pedidos360_backend.messaging.producer.PedidoEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Prueba el productor sin un RabbitMQ real (RabbitTemplate simulado). */
class PedidoEventPublisherTest {

    private static final String DIRECT = "tecnomarket.pedidos.direct";
    private static final String TOPIC = "tecnomarket.eventos.topic";

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final PedidoEventPublisher publisher = new PedidoEventPublisher(rabbitTemplate,
            new TecnomarketRabbitProperties(
                    new TecnomarketRabbitProperties.Exchanges(DIRECT, TOPIC),
                    new TecnomarketRabbitProperties.RoutingKeys("pedido.stock", "pedido.ticket", "pedido.creado")));

    private final PedidoCreadoEvent evento =
            new PedidoCreadoEvent(10L, "Notebook", 2, "user-sub", LocalDateTime.now());

    @Test
    void publicaUnaVezPorCasoDeUso_conLosNombresDelYaml() {
        publisher.publicarPedidoCreado(evento);

        verify(rabbitTemplate).convertAndSend(eq(DIRECT), eq("pedido.stock"), eq(evento),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        verify(rabbitTemplate).convertAndSend(eq(DIRECT), eq("pedido.ticket"), eq(evento),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        verify(rabbitTemplate).convertAndSend(eq(TOPIC), eq("pedido.creado"), eq(evento),
                any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    void siElBrokerEstaCaido_noPropagaElError_eIntentaLosTresEnvios() {
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class),
                        any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThatCode(() -> publisher.publicarPedidoCreado(evento)).doesNotThrowAnyException();
        verify(rabbitTemplate, times(3)).convertAndSend(any(String.class), any(String.class), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));
    }
}
