package cl.duoc.pedidos360_backend.service;

import cl.duoc.pedidos360_backend.dto.PedidoRequest;
import cl.duoc.pedidos360_backend.messaging.event.PedidoCreadoEvent;
import cl.duoc.pedidos360_backend.messaging.producer.PedidoEventPublisher;
import cl.duoc.pedidos360_backend.model.Pedido;
import cl.duoc.pedidos360_backend.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Prueba la lógica de negocio aislada de la base de datos y de RabbitMQ. */
class PedidoServiceTest {

    private final PedidoRepository repository = mock(PedidoRepository.class);
    private final PedidoEventPublisher publisher = mock(PedidoEventPublisher.class);
    private final PedidoService service = new PedidoService(repository, publisher);

    @Test
    void creaPedidoPendiente_loGuarda_yLuegoPublicaElEvento() {
        when(repository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido creado = service.crear(new PedidoRequest("Notebook", 2), "user-sub");

        assertThat(creado.getEstado()).isEqualTo(Pedido.ESTADO_PENDIENTE);
        assertThat(creado.getClienteSub()).isEqualTo("user-sub");

        // Primero se guarda y después se publica (nunca se avisa de un pedido no guardado).
        InOrder orden = inOrder(repository, publisher);
        orden.verify(repository).save(any(Pedido.class));
        ArgumentCaptor<PedidoCreadoEvent> evento = ArgumentCaptor.forClass(PedidoCreadoEvent.class);
        orden.verify(publisher).publicarPedidoCreado(evento.capture());

        assertThat(evento.getValue().producto()).isEqualTo("Notebook");
        assertThat(evento.getValue().cantidad()).isEqualTo(2);
        assertThat(evento.getValue().clienteSub()).isEqualTo("user-sub");
    }
}
