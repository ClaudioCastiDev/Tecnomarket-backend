package cl.duoc.pedidos360_backend.dto;

import cl.duoc.pedidos360_backend.model.Pedido;

import java.time.LocalDateTime;

/**
 * Forma del JSON que consume el frontend (id, estado y producto son los campos
 * que muestra hoy).
 */
public record PedidoResponse(
        Long id,
        String estado,
        String producto,
        Integer cantidad,
        String clienteSub,
        LocalDateTime fecha) {

    public static PedidoResponse from(Pedido p) {
        return new PedidoResponse(p.getId(), p.getEstado(), p.getProducto(),
                p.getCantidad(), p.getClienteSub(), p.getFecha());
    }
}