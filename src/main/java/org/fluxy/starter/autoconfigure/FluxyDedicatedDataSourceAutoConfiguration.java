package org.fluxy.starter.autoconfigure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.fluxy.starter.properties.FluxyDataSourceProperties;
import org.fluxy.starter.properties.FluxyJpaProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuración JPA con datasource <b>dedicado</b> para Fluxy.
 *
 * <p>Se activa <b>únicamente</b> cuando {@code fluxy.datasource.url} está definido en el
 * {@code application.yml}. Cuando está activo, crea una infraestructura JPA completamente
 * independiente del datasource primario de la aplicación:</p>
 *
 * <ul>
 *   <li>{@code fluxyDataSource} — pool HikariCP propio configurado con {@code fluxy.datasource.*}</li>
 *   <li>{@code fluxyEntityManagerFactory} — EMF que escanea solo las entidades de {@code fluxy-spring}</li>
 *   <li>{@code fluxyTransactionManager} — gestor de transacciones propio</li>
 * </ul>
 *
 * <p>Los repositorios de {@code fluxy-spring} se vinculan a esta infraestructura mediante
 * {@link EnableJpaRepositories#entityManagerFactoryRef()} y
 * {@link EnableJpaRepositories#transactionManagerRef()}.</p>
 *
 * <p><b>Nota:</b> Ninguno de estos beans lleva {@code @Primary}, por lo que no interfieren
 * con el datasource principal de la aplicación.</p>
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.data.jpa.repository.JpaRepository")
@ConditionalOnProperty(prefix = "fluxy.datasource", name = "url")
@EnableJpaRepositories(
        basePackages = "org.fluxy.spring.persistence.repository",
        entityManagerFactoryRef = "fluxyEntityManagerFactory",
        transactionManagerRef = "fluxyTransactionManager"
)
public class FluxyDedicatedDataSourceAutoConfiguration {

    /**
     * Crea un {@link DataSource} HikariCP dedicado para Fluxy, usando las propiedades
     * definidas bajo {@code fluxy.datasource.*}.
     */
    @Bean("fluxyDataSource")
    public DataSource fluxyDataSource(FluxyDataSourceProperties props) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getUrl());
        config.setUsername(props.getUsername());
        config.setPassword(props.getPassword());

        if (props.getDriverClassName() != null && !props.getDriverClassName().isBlank()) {
            config.setDriverClassName(props.getDriverClassName());
        }

        FluxyDataSourceProperties.Hikari hikari = props.getHikari();
        config.setPoolName(hikari.getPoolName());
        config.setMaximumPoolSize(hikari.getMaximumPoolSize());
        config.setMinimumIdle(hikari.getMinimumIdle());
        config.setConnectionTimeout(hikari.getConnectionTimeout());
        config.setIdleTimeout(hikari.getIdleTimeout());
        config.setMaxLifetime(hikari.getMaxLifetime());

        return new HikariDataSource(config);
    }

    /**
     * Crea un {@link EntityManagerFactory} exclusivo para las entidades de Fluxy.
     *
     * <p>Solo escanea el paquete {@code org.fluxy.spring.persistence.entity}, asegurando
     * aislamiento total del modelo de entidades de la aplicación principal.</p>
     */
    @Bean("fluxyEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean fluxyEntityManagerFactory(
            DataSource fluxyDataSource,
            FluxyJpaProperties jpaProps) {

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(jpaProps.isShowSql());
        vendorAdapter.setGenerateDdl(jpaProps.isGenerateDdl());

        if (jpaProps.getDatabasePlatform() != null && !jpaProps.getDatabasePlatform().isBlank()) {
            vendorAdapter.setDatabasePlatform(jpaProps.getDatabasePlatform());
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", jpaProps.getHibernate().getDdlAuto());

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(fluxyDataSource);
        emf.setPackagesToScan("org.fluxy.spring.persistence.entity");
        emf.setJpaVendorAdapter(vendorAdapter);
        emf.setJpaPropertyMap(properties);
        emf.setPersistenceUnitName("fluxy");

        return emf;
    }

    /**
     * Crea un {@link PlatformTransactionManager} vinculado al EMF de Fluxy.
     *
     * <p>Permite que las operaciones transaccionales de los repositorios de Fluxy
     * se gestionen de forma independiente al {@code transactionManager} primario.</p>
     */
    @Bean("fluxyTransactionManager")
    public PlatformTransactionManager fluxyTransactionManager(
            EntityManagerFactory fluxyEntityManagerFactory) {
        return new JpaTransactionManager(fluxyEntityManagerFactory);
    }
}

