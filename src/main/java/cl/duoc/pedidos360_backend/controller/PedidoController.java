package cl.duoc.pedidos360_backend.controller;

import cl.duoc.pedidos360_backend.dto.PedidoRequest;
import cl.duoc.pedidos360_backend.dto.PedidoResponse;
import cl.duoc.pedidos360_backend.model.Pedido;
import cl.duoc.pedidos360_backend.repository.PedidoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * La autorización (scopes y roles) se define en SecurityConfig:
 * GET -> scope pedidos-read | POST -> scope pedidos-write | DELETE -> rol
 * ADMIN.
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoRepository repository;

    public PedidoController(PedidoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PedidoResponse> listar() {
        return repository.findAll().stream().map(PedidoResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id) {
        return repository.findById(id)
                .map(PedidoResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(@Valid @RequestBody PedidoRequest request, @AuthenticationPrincipal Jwt jwt) {
        Pedido pedido = new Pedido(Pedido.ESTADO_PENDIENTE, request.producto(), request.cantidad(),
                jwt.getSubject(), LocalDateTime.now());
        return PedidoResponse.from(repository.save(pedido));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado");
        }
        repository.deleteById(id);
    }
}