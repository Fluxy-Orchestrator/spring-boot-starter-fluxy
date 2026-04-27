package org.fluxy.starter.eventbus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.json.JsonMapper;
import org.fluxy.core.model.ExecutionContext;
import org.fluxy.core.model.Reference;
import org.fluxy.core.model.Variable;
import org.fluxy.starter.support.FluxyEventContext;

/**
 * Factory que produce un {@link ObjectMapper} de Jackson configurado con los
 * mixins necesarios para serializar y deserializar {@link org.fluxy.core.model.FluxyEvent}
 * y sus tipos asociados, que carecen de constructor sin argumentos.
 *
 * <p>Usado internamente por las implementaciones del bus basadas en mensajería
 * externa (SQS, RabbitMQ) para convertir eventos a/desde JSON.</p>
 */
public final class FluxyEventBusObjectMapperFactory {

    private FluxyEventBusObjectMapperFactory() {
        // utility class
    }

    /**
     * Crea un {@link ObjectMapper} con mixins registrados para:
     * <ul>
     *   <li>{@link org.fluxy.core.model.FluxyEvent} — constructor {@code (source, payload, context)}</li>
     *   <li>{@link ExecutionContext} — deserializado como {@link org.fluxy.starter.support.FluxyEventContext}
     *       para resolver la restricción de Jackson de no poder instanciar clases abstractas.</li>
     *   <li>{@link Variable} — constructor {@code (name, value)}</li>
     *   <li>{@link Reference} — constructor {@code (type, value)}</li>
     * </ul>
     */
    public static ObjectMapper create() {
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addMixIn(org.fluxy.core.model.FluxyEvent.class, FluxyEventMixin.class)
                .addMixIn(ExecutionContext.class, ExecutionContextMixin.class)
                .addMixIn(Variable.class, VariableMixin.class)
                .addMixIn(Reference.class, ReferenceMixin.class)
                .build();
    }

    // ── Jackson Mixins ────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class FluxyEventMixin {
        @JsonCreator
        FluxyEventMixin(
                @JsonProperty("source") Object source,
                @JsonProperty("payload") Object payload,
                @JsonProperty("context") ExecutionContext context) {}
    }

    @JsonDeserialize(as = FluxyEventContext.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class ExecutionContextMixin {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class VariableMixin {
        @JsonCreator
        VariableMixin(
                @JsonProperty("name") String name,
                @JsonProperty("value") String value) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class ReferenceMixin {
        @JsonCreator
        ReferenceMixin(
                @JsonProperty("type") String type,
                @JsonProperty("value") String value) {}
    }
}

