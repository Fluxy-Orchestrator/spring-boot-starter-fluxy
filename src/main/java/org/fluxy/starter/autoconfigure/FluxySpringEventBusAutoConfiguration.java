package org.fluxy.starter.autoconfigure;

import org.fluxy.starter.eventbus.FluxyEventHandler;
import org.fluxy.starter.eventbus.spring.FluxySpringEventListener;
import org.fluxy.starter.eventbus.spring.SpringFluxyEventBus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Auto-configuración del bus de eventos basado en <b>Spring Application Events</b>.
 *
 * <p>Se activa cuando {@code fluxy.eventbus.type} es {@code SPRING} o no está
 * definido (comportamiento por defecto). No requiere ninguna dependencia
 * externa adicional.</p>
 *
 * <p>Registra:</p>
 * <ul>
 *   <li>{@link SpringFluxyEventBus} — implementa {@code FluxyEventsBus}: publica vía
 *       {@code ApplicationEventPublisher} y en {@code listen} despacha a los handlers.</li>
 *   <li>{@link FluxySpringEventListener} — captura con {@code @EventListener} y
 *       llama a {@code bus.listen()}.</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(
        prefix = "fluxy.eventbus",
        name = "type",
        havingValue = "SPRING",
        matchIfMissing = true
)
public class FluxySpringEventBusAutoConfiguration {

    @Bean
    public SpringFluxyEventBus fluxyEventBus(
            ApplicationEventPublisher eventPublisher,
            List<FluxyEventHandler> handlers) {
        return new SpringFluxyEventBus(eventPublisher, handlers);
    }

    @Bean
    public FluxySpringEventListener fluxySpringEventListener(SpringFluxyEventBus fluxyEventBus) {
        return new FluxySpringEventListener(fluxyEventBus);
    }
}
