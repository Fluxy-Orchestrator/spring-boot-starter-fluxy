package org.fluxy.starter.dto;

import java.util.List;

/**
 * Request para inicializar o continuar la ejecución de un flow, step o task.
 *
 * <p>Contiene los parámetros del {@link org.fluxy.core.model.ExecutionContext}
 * que se construirá para la ejecución.</p>
 *
 * @param type       tipo del contexto de ejecución (default: "default")
 * @param version    versión del contexto de ejecución (default: "1.0")
 * @param variables  lista de variables a inyectar en el contexto
 * @param references lista de referencias a inyectar en el contexto
 */
public record ExecutionContextRequest(
        String type,
        String version,
        List<VariableDto> variables,
        List<ReferenceDto> references
) {
}

