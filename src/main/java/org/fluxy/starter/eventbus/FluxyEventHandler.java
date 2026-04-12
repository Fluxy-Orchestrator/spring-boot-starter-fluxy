package org.fluxy.starter.eventbus;

import org.fluxy.core.model.FluxyEvent;

/**
 * Interfaz que las aplicaciones implementan para recibir eventos de Fluxy.
 *
 * <p>Todos los beans que implementen esta interfaz serán invocados automáticamente
 * cuando un {@link FluxyEvent} llegue al bus, independientemente de la
 * implementación subyacente (Spring Events, SQS, RabbitMQ).</p>
 *
 * <p>Ejemplo:</p>
 * <pre>{@code
 * @Component
 * public class MyFluxyHandler implements FluxyEventHandler {
 *
 *     @Override
 *     public void handle(FluxyEvent<?,?> event) {
 *         Object payload = event.getPayload();
 *         ExecutionContext ctx = event.getContext();
 *         // lógica de negocio
 *     }
 * }
 * }</pre>
 */
public interface FluxyEventHandler {

    /**
     * Procesa un evento recibido del bus.
     *
     * @param event el evento recibido; nunca {@code null}
     */
    void handle(FluxyEvent<?, ?> event);
}
