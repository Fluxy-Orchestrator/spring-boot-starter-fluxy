package org.fluxy.starter.eventbus.spring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fluxy.core.model.FluxyEvent;
import org.fluxy.starter.eventbus.FluxyEventBus;
import org.fluxy.starter.eventbus.FluxyEventHandler;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

/**
 * Implementación de {@link FluxyEventBus} basada en Spring Application Events.
 *
 * <p>Flujo:</p>
 * <ol>
 *   <li>{@code publish} envuelve el {@link FluxyEvent} en un {@link FluxyApplicationEvent}
 *       y lo publica vía {@link ApplicationEventPublisher}.</li>
 *   <li>{@link FluxySpringEventListener} captura el evento con {@code @EventListener}
 *       y llama a {@code listen}.</li>
 *   <li>{@code listen} distribuye el evento a todos los {@link FluxyEventHandler}
 *       registrados en el contexto.</li>
 * </ol>
 *
 * <p>Es la implementación por defecto cuando {@code fluxy.eventbus.type} no está
 * definido o es {@code SPRING}. No requiere infraestructura externa.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class SpringFluxyEventBus implements FluxyEventBus {

    private final ApplicationEventPublisher eventPublisher;
    private final List<FluxyEventHandler> handlers;

    @Override
    public void publish(FluxyEvent<?, ?> event) {
        log.debug("Publicando FluxyEvent vía Spring Events: source={}", event.getSource());
        eventPublisher.publishEvent(new FluxyApplicationEvent(this, event));
    }

    /**
     * Invocado por {@link FluxySpringEventListener} cuando Spring entrega el evento.
     * Distribuye el evento a todos los {@link FluxyEventHandler} del contexto.
     */
    @Override
    public void listen(FluxyEvent<?, ?> event) {
        log.debug("Procesando FluxyEvent vía Spring Events: source={}", event.getSource());
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
