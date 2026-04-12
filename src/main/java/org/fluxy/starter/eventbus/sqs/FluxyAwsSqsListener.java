package org.fluxy.starter.eventbus.sqs;

import tools.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fluxy.core.model.FluxyEvent;

/**
 * Listener de SQS que consume mensajes de la cola configurada en
 * {@code fluxy.eventbus.sqs.queue-url}, los deserializa a {@link FluxyEvent}
 * y delega a {@link SqsFluxyEventBus#listen} para distribuir a los handlers.
 *
 * <p>Se activa automáticamente cuando la implementación de event bus
 * seleccionada es {@code SQS}.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class FluxyAwsSqsListener {

    private final SqsFluxyEventBus bus;
    private final ObjectMapper objectMapper;

    /**
     * Recibe un mensaje JSON de la cola SQS, lo deserializa a {@link FluxyEvent}
     * y lo delega a {@code bus.listen()}.
     *
     * @param message el cuerpo del mensaje SQS (JSON)
     */
    @SqsListener("${fluxy.eventbus.sqs.queue-url}")
    public void onMessage(String message) {
        try {
            FluxyEvent<?, ?> event = objectMapper.readValue(message, FluxyEvent.class);
            log.debug("Recibido FluxyEvent vía SQS, delegando a bus.listen()");
            bus.listen(event);
        } catch (Exception ex) {
            log.error("Error procesando mensaje SQS: {}", ex.getMessage(), ex);
        }
    }
}
