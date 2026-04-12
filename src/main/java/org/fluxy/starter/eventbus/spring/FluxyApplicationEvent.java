package org.fluxy.starter.eventbus.spring;

import org.fluxy.core.model.FluxyEvent;
import org.springframework.context.ApplicationEvent;

/**
 * Envuelve un {@link FluxyEvent} del core como {@link ApplicationEvent} de Spring
 * para poder publicarlo a través del {@code ApplicationEventPublisher}.
 *
 * <p>Usado internamente por {@link SpringFluxyEventBus}.</p>
 */
public class FluxyApplicationEvent extends ApplicationEvent {

    private final FluxyEvent<?, ?> fluxyEvent;

    /**
     * @param source     el objeto que originó el evento (normalmente el publisher)
     * @param fluxyEvent el evento de Fluxy que se envuelve
     */
    public FluxyApplicationEvent(Object source, FluxyEvent<?, ?> fluxyEvent) {
        super(source);
        this.fluxyEvent = fluxyEvent;
    }

    /** Devuelve el {@link FluxyEvent} envuelto. */
    public FluxyEvent<?, ?> getFluxyEvent() {
        return fluxyEvent;
    }
}
