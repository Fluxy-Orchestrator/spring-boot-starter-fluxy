package org.fluxy.starter.eventbus;

import tools.jackson.databind.ObjectMapper;
import org.fluxy.core.model.ExecutionContext;
import org.fluxy.core.model.FluxyEvent;
import org.fluxy.core.model.Reference;
import org.fluxy.core.model.Variable;
import org.fluxy.starter.support.FluxyEventContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que {@link FluxyEventBusObjectMapperFactory#create()} puede deserializar
 * correctamente un {@link FluxyEvent} cuyo {@link ExecutionContext} es una clase abstracta,
 * usando {@link FluxyEventContext} como tipo concreto.
 */
@DisplayName("FluxyEventBusObjectMapperFactory")
class FluxyEventBusObjectMapperFactoryTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = FluxyEventBusObjectMapperFactory.create();
    }

    @Test
    @DisplayName("Deserializa FluxyEvent con ExecutionContext abstracto como FluxyEventContext")
    void shouldDeserializeFluxyEventWithExecutionContext() throws Exception {
        String json = """
                {
                  "source": "someTask",
                  "payload": "SUCCESS",
                  "context": {
                    "type": "order-flow",
                    "version": "1.0",
                    "variables": [
                      {"name": "orderId", "value": "ORD-001"}
                    ],
                    "references": [
                      {"type": "customer", "value": "CUST-999"}
                    ]
                  }
                }
                """;

        FluxyEvent<?, ?> event = objectMapper.readValue(json, FluxyEvent.class);

        assertNotNull(event);
        assertNotNull(event.getContext());
        assertInstanceOf(FluxyEventContext.class, event.getContext(),
                "El contexto debe ser instancia de FluxyEventContext, no de ExecutionContext abstracto");

        ExecutionContext ctx = event.getContext();
        assertEquals("order-flow", ctx.getType());
        assertEquals("1.0", ctx.getVersion());

        List<Variable> variables = ctx.getVariables();
        assertEquals(1, variables.size());
        assertEquals("orderId", variables.get(0).getName());
        assertEquals("ORD-001", variables.get(0).getValue());

        List<Reference> references = ctx.getReferences();
        assertEquals(1, references.size());
        assertEquals("customer", references.get(0).getType());
        assertEquals("CUST-999", references.get(0).getValue());
    }

    @Test
    @DisplayName("Deserializa FluxyEvent con contexto vacío (sin variables ni referencias)")
    void shouldDeserializeFluxyEventWithEmptyContext() throws Exception {
        String json = """
                {
                  "source": "anotherTask",
                  "payload": "FAILURE",
                  "context": {
                    "type": "payment-flow",
                    "version": "2.0"
                  }
                }
                """;

        FluxyEvent<?, ?> event = objectMapper.readValue(json, FluxyEvent.class);

        assertNotNull(event);
        assertInstanceOf(FluxyEventContext.class, event.getContext());
        assertEquals("payment-flow", event.getContext().getType());
        assertEquals("2.0", event.getContext().getVersion());
    }

    @Test
    @DisplayName("FluxyEventContext.getVariable(name, type) lanza UnsupportedOperationException")
    void shouldThrowOnTypedVariableAccess() throws Exception {
        String json = """
                {"source": "t", "payload": "p",
                 "context": {"type": "f", "version": "1"}}
                """;

        FluxyEvent<?, ?> event = objectMapper.readValue(json, FluxyEvent.class);
        FluxyEventContext ctx = (FluxyEventContext) event.getContext();

        assertThrows(UnsupportedOperationException.class,
                () -> ctx.getVariable("key", String.class));
    }

    @Test
    @DisplayName("FluxyEventContext.getReference(type, clazz) lanza UnsupportedOperationException")
    void shouldThrowOnTypedReferenceAccess() throws Exception {
        String json = """
                {"source": "t", "payload": "p",
                 "context": {"type": "f", "version": "1"}}
                """;

        FluxyEvent<?, ?> event = objectMapper.readValue(json, FluxyEvent.class);
        FluxyEventContext ctx = (FluxyEventContext) event.getContext();

        assertThrows(UnsupportedOperationException.class,
                () -> ctx.getReference("customer", String.class));
    }
}

