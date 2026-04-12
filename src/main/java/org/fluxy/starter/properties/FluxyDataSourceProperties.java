package org.fluxy.starter.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades para configurar un datasource dedicado para Fluxy.
 *
 * <p>Cuando {@code fluxy.datasource.url} está presente en el {@code application.yml},
 * el starter crea un {@code DataSource}, un {@code EntityManagerFactory} y un
 * {@code TransactionManager} propios para Fluxy, completamente independientes
 * del datasource principal de la aplicación.</p>
 *
 * <p>Ejemplo en {@code application.yml}:</p>
 * <pre>{@code
 * fluxy:
 *   datasource:
 *     url: jdbc:postgresql://localhost:5432/fluxydb
 *     driver-class-name: org.postgresql.Driver
 *     username: fluxy_user
 *     password: fluxy_pass
 *     hikari:
 *       pool-name: FluxyPool
 *       maximum-pool-size: 10
 *       minimum-idle: 2
 * }</pre>
 *
 * <p>Cuando {@code fluxy.datasource.url} <b>no</b> está definido, el starter
 * usa el datasource primario de la aplicación (comportamiento por defecto).</p>
 */
@Data
@ConfigurationProperties(prefix = "fluxy.datasource")
public class FluxyDataSourceProperties {

    /**
     * JDBC URL del datasource de Fluxy.
     * Cuando se define, se crea un datasource dedicado para la persistencia de Fluxy.
     */
    private String url;

    /** Clase del driver JDBC. Se auto-detecta a partir de la URL si no se especifica. */
    private String driverClassName;

    /** Usuario del datasource de Fluxy. */
    private String username;

    /** Contraseña del datasource de Fluxy. */
    private String password;

    /** Configuración del pool HikariCP para el datasource de Fluxy. */
    private Hikari hikari = new Hikari();

    @Data
    public static class Hikari {

        /** Nombre del pool de conexiones. Default: {@code FluxyHikariPool}. */
        private String poolName = "FluxyHikariPool";

        /** Número máximo de conexiones en el pool. Default: 10. */
        private int maximumPoolSize = 10;

        /** Número mínimo de conexiones inactivas en el pool. Default: 2. */
        private int minimumIdle = 2;

        /** Tiempo máximo (ms) para obtener una conexión del pool. Default: 30000. */
        private long connectionTimeout = 30_000L;

        /** Tiempo máximo (ms) que una conexión puede estar inactiva. Default: 600000. */
        private long idleTimeout = 600_000L;

        /** Tiempo máximo de vida (ms) de una conexión. Default: 1800000. */
        private long maxLifetime = 1_800_000L;
    }
}

