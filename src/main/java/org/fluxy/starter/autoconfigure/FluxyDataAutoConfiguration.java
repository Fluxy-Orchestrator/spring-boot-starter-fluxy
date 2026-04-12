package org.fluxy.starter.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuración JPA <b>por defecto</b> para Fluxy — usa el datasource primario de la aplicación.
 *
 * <p>Se activa únicamente cuando:</p>
 * <ul>
 *   <li>{@code JpaRepository} está en el classpath</li>
 *   <li><b>No</b> existe un bean {@code fluxyEntityManagerFactory}
 *       (es decir, el usuario NO definió {@code fluxy.datasource.url})</li>
 * </ul>
 *
 * <p>Cuando está activo, registra el paquete {@code org.fluxy.spring} en
 * {@link AutoConfigurationPackages} para que el {@code EntityManagerFactory}
 * primario de la aplicación descubra las entidades de fluxy-spring, y los
 * repositorios se enlazan al EMF y TransactionManager por defecto.</p>
 *
 * <p>Si el usuario define {@code fluxy.datasource.url},
 * {@link FluxyDedicatedDataSourceAutoConfiguration} toma precedencia y esta
 * configuración NO se carga.</p>
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.data.jpa.repository.JpaRepository")
@ConditionalOnMissingBean(name = "fluxyEntityManagerFactory")
@EnableJpaRepositories(basePackages = "org.fluxy.spring.persistence.repository")
public class FluxyDataAutoConfiguration {

    /**
     * Registra {@code org.fluxy.spring} como paquete de auto-configuración para
     * que Spring Boot (a través de {@link AutoConfigurationPackages}) informe a
     * Hibernate/Spring Data JPA de que debe escanear las entidades de fluxy-spring.
     */
    @Bean
    public static BeanDefinitionRegistryPostProcessor fluxyEntityPackagesRegistrar() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
                    throws BeansException {
                AutoConfigurationPackages.register(registry, "org.fluxy.spring");
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
                    throws BeansException {
                // no-op
            }
        };
    }
}


