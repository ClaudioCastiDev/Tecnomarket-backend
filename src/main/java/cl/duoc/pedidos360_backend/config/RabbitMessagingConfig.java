package cl.duoc.pedidos360_backend.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración técnica de mensajería (no de negocio).
 * <ul>
 * <li>Activa {@link TecnomarketRabbitProperties} (nombres desde el YAML).</li>
 * <li>Envía los eventos como JSON en vez de serialización Java, para que
 * productos y mensajería los lean aunque tengan su propia clase de evento.</li>
 * <li>Define el RabbitAdmin que declara los exchanges al conectarse.</li>
 * </ul>
 * Spring Boot usa automáticamente este MessageConverter en el RabbitTemplate.
 */
@Configuration
@EnableConfigurationProperties(TecnomarketRabbitProperties.class)
public class RabbitMessagingConfig {

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * Si otro servicio ya declaró un exchange con parámetros distintos, RabbitMQ rechaza la
     * nueva declaración. Con ignoreDeclarationExceptions el error queda en el log y pedidos
     * sigue publicando en el exchange existente, en vez de fallar cada publicación.
     */
    @Bean
    RabbitAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setIgnoreDeclarationExceptions(true);
        return admin;
    }
}
