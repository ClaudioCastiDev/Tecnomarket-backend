package cl.duoc.pedidos360_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PedidoRequest(
        @NotBlank @Size(max = 120) String producto,
        @NotNull @Min(1) Integer cantidad) {
}