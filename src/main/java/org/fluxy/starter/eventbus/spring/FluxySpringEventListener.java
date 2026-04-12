package org.fluxy.starter.eventbus.spring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

/**
 * Listener de Spring que captura {@link FluxyApplicationEvent} y delega
 * a {@link SpringFluxyEventBus#listen} para distribuir a los handlers.
 *
 * <p>Se activa automáticamente cuando la implementación de event bus
 * seleccionada es {@code SPRING}.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class FluxySpringEventListener {

    private final SpringFluxyEventBus bus;

    /**
     * Recibe un {@link FluxyApplicationEvent} publicado por
     * {@link SpringFluxyEventBus#publish} y lo delega a {@code bus.listen()}.
     */
    @EventListener
    public void onFluxyEvent(FluxyApplicationEvent event) {
        log.debug("Recibido FluxyApplicationEvent, delegando a bus.listen()");
        bus.listen(event.getFluxyEvent());
    }
}
