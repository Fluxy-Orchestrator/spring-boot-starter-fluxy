package org.fluxy.starter.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades para configurar el bus de eventos de Fluxy.
 *
 * <p>Permite seleccionar la implementación del bus y configurar los parámetros
 * específicos de cada proveedor (SQS, RabbitMQ). Cuando no se define ningún
 * tipo, se usa {@link EventBusType#SPRING} (Spring Application Events) como
 * fallback sin necesidad de configuración adicional.</p>
 *
 * <p>Ejemplo en {@code application.yml}:</p>
 * <pre>{@code
 * fluxy:
 *   eventbus:
 *     type: RABBIT
 *     rabbit:
 *       host: localhost
 *       port: 5672
 *       username: guest
 *       password: guest
 *       queue: fluxy-events
 *       exchange: fluxy-exchange
 *       routing-key: fluxy.events
 * }</pre>
 */
@Data
@ConfigurationProperties(prefix = "fluxy.eventbus")
public class FluxyEventBusProperties {

    /**
     * Tipo de implementación del bus de eventos.
     * <ul>
     *   <li>{@code SPRING} — usa Spring Application Events (por defecto, sin dependencias extra)</li>
     *   <li>{@code SQS} — usa Amazon SQS (requiere {@code io.awspring.cloud:spring-cloud-aws-sqs} en el classpath)</li>
     *   <li>{@code RABBIT} — usa RabbitMQ (requiere {@code org.springframework.amqp:spring-rabbit} en el classpath)</li>
     * </ul>
     *
     * <p>Default: {@code SPRING}.</p>
     */
    private EventBusType type = EventBusType.SPRING;

    /** Configuración específica de AWS SQS. Solo tiene efecto cuando {@code type = SQS}. */
    private Sqs sqs = new Sqs();

    /** Configuración específica de RabbitMQ. Solo tiene efecto cuando {@code type = RABBIT}. */
    private Rabbit rabbit = new Rabbit();

    // ── Tipos internos ────────────────────────────────────────────────────────

    /**
     * Tipo de implementación del bus de eventos de Fluxy.
     */
    public enum EventBusType {
        /** Spring Application Events — comunicación local en la misma JVM. */
        SPRING,
        /** Amazon Simple Queue Service — comunicación distribuida vía SQS. */
        SQS,
        /** RabbitMQ — comunicación distribuida vía AMQP. */
        RABBIT
    }

    /**
     * Propiedades de conexión y configuración para Amazon SQS.
     */
    @Data
    public static class Sqs {

        /**
         * URL completa de la cola SQS donde se publican y consumen los eventos.
         * Ejemplo: {@code https://sqs.us-east-1.amazonaws.com/123456789012/fluxy-events}
         */
        private String queueUrl;

        /**
         * Región de AWS donde reside la cola SQS.
         * Ejemplo: {@code us-east-1}
         */
        private String region;
    }

    /**
     * Propiedades de conexión y configuración para RabbitMQ.
     */
    @Data
    public static class Rabbit {

        /** Host del servidor RabbitMQ. Default: {@code localhost}. */
        private String host = "localhost";

        /** Puerto AMQP del servidor RabbitMQ. Default: {@code 5672}. */
        private int port = 5672;

        /** Usuario de autenticación en RabbitMQ. Default: {@code guest}. */
        private String username = "guest";

        /** Contraseña de autenticación en RabbitMQ. Default: {@code guest}. */
        private String password = "guest";

        /** Virtual host de RabbitMQ. Default: {@code /}. */
        private String virtualHost = "/";

        /** Nombre de la cola donde se consumen los eventos. Default: {@code fluxy-events}. */
        private String queue = "fluxy-events";

        /** Exchange donde se publican los eventos. Default: {@code fluxy-exchange}. */
        private String exchange = "fluxy-exchange";

        /** Routing key utilizada para publicar y enlazar la cola. Default: {@code fluxy.events}. */
        private String routingKey = "fluxy.events";
    }
}

