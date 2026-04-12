package org.fluxy.starter.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades JPA/Hibernate para el datasource dedicado de Fluxy.
 *
 * <p>Solo tienen efecto cuando {@code fluxy.datasource.url} está definido.
 * Cuando Fluxy usa el datasource primario de la aplicación, estas propiedades
 * se ignoran y se usan las de {@code spring.jpa.*} en su lugar.</p>
 *
 * <p>Ejemplo en {@code application.yml}:</p>
 * <pre>{@code
 * fluxy:
 *   jpa:
 *     show-sql: false
 *     database-platform: org.hibernate.dialect.PostgreSQLDialect
 *     hibernate:
 *       ddl-auto: validate
 * }</pre>
 */
@Data
@ConfigurationProperties(prefix = "fluxy.jpa")
public class FluxyJpaProperties {

    /**
     * Mostrar sentencias SQL generadas por Hibernate en la consola.
     * Default: {@code false}.
     */
    private boolean showSql = false;

    /**
     * Permitir la generación automática de DDL (esquema).
     * Default: {@code false}.
     */
    private boolean generateDdl = false;

    /**
     * Dialecto de la base de datos (fully qualified class name).
     * Si no se especifica, Hibernate lo detecta automáticamente.
     *
     * <p>Ejemplo: {@code org.hibernate.dialect.PostgreSQLDialect}</p>
     */
    private String databasePlatform;

    /** Configuración específica de Hibernate para el datasource dedicado. */
    private Hibernate hibernate = new Hibernate();

    @Data
    public static class Hibernate {

        /**
         * Estrategia de generación de esquema de Hibernate.
         *
         * <p>Valores comunes: {@code none}, {@code validate}, {@code update},
         * {@code create}, {@code create-drop}.</p>
         *
         * <p>Default: {@code none} (conservador para producción).</p>
         */
        private String ddlAuto = "none";
    }
}

