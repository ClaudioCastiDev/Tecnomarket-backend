package cl.duoc.pedidos360_backend.config;

import cl.duoc.pedidos360_backend.model.Pedido;
import cl.duoc.pedidos360_backend.repository.PedidoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Inserta datos de ejemplo si la tabla está vacía, para que la demo muestre
 * resultados.
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedPedidos(PedidoRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                LocalDateTime ahora = LocalDateTime.now();
                repository.saveAll(List.of(
                        new Pedido("PENDIENTE", "Teclado mecánico", 1, "seed", ahora),
                        new Pedido("EN_PREPARACION", "Monitor 27 pulgadas", 2, "seed", ahora),
                        new Pedido("ENTREGADO", "Mouse inalámbrico", 3, "seed", ahora)));
            }
        };
    }
}