package org.fluxy.starter.eventbus.rabbit;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fluxy.core.model.FluxyEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

/**
 * Listener de RabbitMQ que consume mensajes de la cola configurada en
 * {@code fluxy.eventbus.rabbit.queue}, los deserializa a {@link FluxyEvent}
 * y delega a {@link RabbitFluxyEventBus#listen} para distribuir a los handlers.
 *
 * <p>Se activa automáticamente cuando la implementación de event bus
 * seleccionada es {@code RABBIT}.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class FluxyRabbitListener {

    private final RabbitFluxyEventBus bus;
    private final ObjectMapper objectMapper;

    /**
     * Recibe un mensaje JSON de la cola RabbitMQ, lo deserializa a
     * {@link FluxyEvent} y lo delega a {@code bus.listen()}.
     *
     * @param message el cuerpo del mensaje (JSON)
     */
    @RabbitListener(queues = "${fluxy.eventbus.rabbit.queue}")
    public void onMessage(String message) {
        try {
            FluxyEvent<?, ?> event = objectMapper.readValue(message, FluxyEvent.class);
            log.debug("Recibido FluxyEvent vía RabbitMQ, delegando a bus.listen()");
            bus.listen(event);
        } catch (Exception ex) {
            log.error("Error procesando mensaje RabbitMQ: {}", ex.getMessage(), ex);
        }
    }
}
