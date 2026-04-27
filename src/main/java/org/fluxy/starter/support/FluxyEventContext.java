package org.fluxy.starter.support;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.fluxy.core.model.ExecutionContext;

/**
 * Implementación concreta de {@link ExecutionContext} de solo lectura, diseñada
 * exclusivamente para la deserialización JSON de {@link org.fluxy.core.model.FluxyEvent}
 * recibidos desde el bus de eventos (SQS, RabbitMQ, etc.).
 *
 * <p>Resuelve el problema de que {@link ExecutionContext} es una clase abstracta y
 * Jackson no puede instanciarla directamente durante la deserialización. Esta clase
 * actúa como target concreto, registrado mediante el mixin de
 * {@link org.fluxy.starter.eventbus.FluxyEventBusObjectMapperFactory}.</p>
 *
 * <p><strong>Importante:</strong> Los métodos abstractos tipados lanzan
 * {@link UnsupportedOperationException} ya que esta clase existe únicamente para
 * transportar datos de eventos deserializados, no para ejecutar lógica de negocio.
 * Si un {@link org.fluxy.starter.eventbus.FluxyEventHandler} necesita acceso tipado
 * al contexto, debe hacer cast a {@link ExecutionContextProxy} (disponible solo en
 * contextos de ejecución activa, no en eventos recibidos vía mensajería).</p>
 */
public class FluxyEventContext extends ExecutionContext {

    @JsonCreator
    public FluxyEventContext(
            @JsonProperty("type") String type,
            @JsonProperty("version") String version) {
        super(type, version);
    }

    /**
     * @throws UnsupportedOperationException siempre — esta clase no soporta deserialización tipada.
     */
    @Override
    public <T> T getVariable(String name, Class<T> type) {
        throw new UnsupportedOperationException(
                "FluxyEventContext no soporta acceso tipado. Úsalo solo para leer variables como String.");
    }

    /**
     * @throws UnsupportedOperationException siempre — esta clase no soporta deserialización tipada.
     */
    @Override
    public <T> T getReference(String type, Class<T> clazz) {
        throw new UnsupportedOperationException(
                "FluxyEventContext no soporta acceso tipado. Úsalo solo para leer referencias como String.");
    }

    /**
     * @throws UnsupportedOperationException siempre — esta clase es de solo lectura post-deserialización.
     */
    @Override
    public void addVariable(String name, Object value) {
        throw new UnsupportedOperationException(
                "FluxyEventContext es de solo lectura. No se pueden agregar variables tipadas.");
    }

    /**
     * @throws UnsupportedOperationException siempre — esta clase es de solo lectura post-deserialización.
     */
    @Override
    public void addReference(String type, Object value) {
        throw new UnsupportedOperationException(
                "FluxyEventContext es de solo lectura. No se pueden agregar referencias tipadas.");
    }
}

