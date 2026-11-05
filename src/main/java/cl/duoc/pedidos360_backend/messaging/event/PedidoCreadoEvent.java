package cl.duoc.pedidos360_backend.messaging.event;

import cl.duoc.pedidos360_backend.model.Pedido;

import java.time.LocalDateTime;

/**
 * Evento que se publica cuando se crea un pedido. Es el "contrato" entre
 * pedidos y sus consumidores:
 * <ul>
 * <li>productos: descuenta {@code cantidad} del stock de {@code producto}</li>
 * <li>mensajería: genera el ticket y la notificación</li>
 * </ul>
 * Viaja como JSON. Si se cambian los campos, hay que cambiarlos también en los
 * consumidores.
 */
public record PedidoCreadoEvent(
        Long pedidoId,
        String producto,
        Integer cantidad,
        String clienteSub,
        LocalDateTime fecha) {

    public static PedidoCreadoEvent from(Pedido p) {
        return new PedidoCreadoEvent(p.getId(), p.getProducto(), p.getCantidad(),
                p.getClienteSub(), p.getFecha());
    }
}
