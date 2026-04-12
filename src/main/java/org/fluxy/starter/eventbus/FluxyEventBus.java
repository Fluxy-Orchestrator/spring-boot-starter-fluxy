package org.fluxy.starter.eventbus;

import org.fluxy.core.service.FluxyEventsBus;

/**
 * Extensión del bus de eventos de Fluxy definido en {@code fluxy-core}.
 *
 * <p>Las implementaciones concretas (Spring Events, AWS SQS, RabbitMQ) implementan
 * esta interfaz, que hereda de {@link FluxyEventsBus} definida en el core.
 * La implementación activa se selecciona automáticamente según la propiedad
 * {@code fluxy.eventbus.type} en {@code application.yml}.</p>
 *
 * <p>Ejemplo de uso:</p>
 * <pre>{@code
 * @Autowired
 * private FluxyEventsBus eventBus;
 *
 * eventBus.publish(new FluxyEvent<>(source, payload, context));
 * }</pre>
 *
 * @see FluxyEventsBus
 * @see org.fluxy.core.model.FluxyEvent
 */
public interface FluxyEventBus extends FluxyEventsBus {
}
