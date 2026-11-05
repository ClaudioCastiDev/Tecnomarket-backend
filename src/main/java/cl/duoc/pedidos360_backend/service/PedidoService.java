package cl.duoc.pedidos360_backend.service;

import cl.duoc.pedidos360_backend.dto.PedidoRequest;
import cl.duoc.pedidos360_backend.messaging.event.PedidoCreadoEvent;
import cl.duoc.pedidos360_backend.messaging.producer.PedidoEventPublisher;
import cl.duoc.pedidos360_backend.model.Pedido;
import cl.duoc.pedidos360_backend.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Lógica de negocio de pedidos. Separa responsabilidades:
 * <ul>
 * <li>el controlador solo atiende HTTP,</li>
 * <li>este servicio decide qué es un pedido nuevo y lo guarda,</li>
 * <li>el publicador ({@link PedidoEventPublisher}) es el único que habla con
 * RabbitMQ.</li>
 * </ul>
 * El evento se publica DESPUÉS de guardar, para que lleve el id real del
 * pedido y nunca se avise de un pedido que no quedó en la base de datos.
 */
@Service
public class PedidoService {

    private final PedidoRepository repository;
    private final PedidoEventPublisher publisher;

    public PedidoService(PedidoRepository repository, PedidoEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    public Pedido crear(PedidoRequest request, String clienteSub) {
        Pedido pedido = new Pedido(Pedido.ESTADO_PENDIENTE, request.producto(), request.cantidad(),
                clienteSub, LocalDateTime.now());
        Pedido guardado = repository.save(pedido);
        publisher.publicarPedidoCreado(PedidoCreadoEvent.from(guardado));
        return guardado;
    }
}
