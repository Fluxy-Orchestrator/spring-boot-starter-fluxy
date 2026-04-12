package org.fluxy.starter.eventbus.sqs;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fluxy.core.model.FluxyEvent;
import org.fluxy.starter.eventbus.FluxyEventBus;
import org.fluxy.starter.eventbus.FluxyEventHandler;

import java.util.List;

/**
 * Implementación de {@link FluxyEventBus} basada en Amazon SQS.
 *
 * <p>Flujo:</p>
 * <ol>
 *   <li>{@code publish} serializa el {@link FluxyEvent} a JSON y lo envía a la cola SQS.</li>
 *   <li>{@link FluxyAwsSqsListener} consume el mensaje con {@code @SqsListener},
 *       lo deserializa y llama a {@code listen}.</li>
 *   <li>{@code listen} distribuye el evento a todos los {@link FluxyEventHandler}
 *       registrados en el contexto.</li>
 * </ol>
 *
 * <p>Se activa cuando {@code fluxy.eventbus.type = SQS} y la dependencia
 * {@code io.awspring.cloud:spring-cloud-aws-sqs} está en el classpath.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class SqsFluxyEventBus implements FluxyEventBus {

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;
    private final String queueUrl;
    private final List<FluxyEventHandler> handlers;

    @Override
    public void publish(FluxyEvent<?, ?> event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            log.debug("Publicando FluxyEvent vía SQS: queue={}", queueUrl);
            sqsTemplate.send(to -> to.queue(queueUrl).payload(json));
        } catch (JacksonException ex) {
            throw new IllegalArgumentException(
                    "Error serializando FluxyEvent a JSON: " + ex.getMessage(), ex);
        }
    }

    /**
     * Invocado por {@link FluxyAwsSqsListener} cuando llega un mensaje de la cola.
     * Distribuye el evento a todos los {@link FluxyEventHandler} del contexto.
     */
    @Override
    public void listen(FluxyEvent<?, ?> event) {
        log.debug("Procesando FluxyEvent vía SQS");
        for (FluxyEventHandler handler : handlers) {
            try {
                handler.handle(event);
            } catch (Exception ex) {
                log.error("Error en handler [{}] procesando FluxyEvent: {}",
                        handler.getClass().getSimpleName(), ex.getMessage(), ex);
            }
        }
    }
}
