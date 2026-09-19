package cl.duoc.pedidos360_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
public class Pedido {

    public static final String ESTADO_PENDIENTE = "PENDIENTE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(nullable = false, length = 120)
    private String producto;

    @Column(nullable = false)
    private Integer cantidad;

    /** Claim "sub" del usuario de Cognito que creó el pedido. */
    @Column(name = "cliente_sub", nullable = false, length = 64)
    private String clienteSub;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected Pedido() {
        // requerido por JPA
    }

    public Pedido(String estado, String producto, Integer cantidad, String clienteSub, LocalDateTime fecha) {
        this.estado = estado;
        this.producto = producto;
        this.cantidad = cantidad;
        this.clienteSub = clienteSub;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public String getEstado() {
        return estado;
    }

    public String getProducto() {
        return producto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public String getClienteSub() {
        return clienteSub;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}