package org.fluxy.starter.support;

import org.fluxy.core.model.ExecutionContext;
import org.fluxy.starter.exception.ContextReferenceCastException;
import org.fluxy.starter.exception.ContextVariableCastException;
import org.fluxy.starter.exception.UndefinedContextReferenceException;
import org.fluxy.starter.exception.UndefinedContextVariableException;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

public class ExecutionContextProxy extends ExecutionContext {

    private final ObjectMapper objectMapper;

    public ExecutionContextProxy(String type, String version, ObjectMapper objectMapper) {
        super(type, version);
        this.objectMapper = objectMapper;
    }

    public <T> T getVariable(String name, Class<T> type) {
        Optional<String> variable = getVariable(name);
        if (variable.isPresent()) {
            try {
                return objectMapper.readValue(variable.get(), type);
            } catch (Exception e) {
                throw new ContextVariableCastException("Failed to deserialize variable: " + name, e);
            }
        } else {
            throw new UndefinedContextVariableException("Variable not found: " + name);
        }
    }

    public <T> T getReference(String name, Class<T> type) {
        Optional<String> variable = getReference(name);
        if (variable.isPresent()) {
            try {
                return objectMapper.readValue(variable.get(), type);
            } catch (Exception e) {
                throw new ContextReferenceCastException("Failed to deserialize reference: " + name, e);
            }
        } else {
            throw new UndefinedContextReferenceException("Reference not found: " + name);
        }
    }

    public void addReference(String type, Object value) {
        String stringValue = objectMapper.writeValueAsString(value);
        addReference(type, stringValue);
    }

    public void addVariable(String name, Object value) {
        String stringValue = objectMapper.writeValueAsString(value);
        addParameter(name, stringValue);
    }

}
