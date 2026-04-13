package org.fluxy.starter.autoconfigure;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.fluxy.starter.eventbus.FluxyEventBusObjectMapperFactory;
import org.fluxy.starter.eventbus.FluxyEventHandler;
import org.fluxy.starter.eventbus.sqs.FluxyAwsSqsListener;
import org.fluxy.starter.eventbus.sqs.SqsFluxyEventBus;
import org.fluxy.starter.properties.FluxyEventBusProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;
import java.util.List;

/**
 * Auto-configuración del bus de eventos basado en <b>Amazon SQS</b>.
 *
 * <p>Se activa cuando:</p>
 * <ul>
 *   <li>{@code fluxy.eventbus.type = SQS}</li>
 *   <li>{@code io.awspring.cloud:spring-cloud-aws-sqs} está en el classpath</li>
 * </ul>
 *
 * <p>Crea una infraestructura SQS propia para Fluxy (cliente, template, listener)
 * aislada de la configuración SQS principal de la aplicación, usando las
 * propiedades definidas en {@code fluxy.eventbus.sqs.*}.</p>
 *
 * <p>Ejemplo de configuración:</p>
 * <pre>{@code
 * fluxy:
 *   eventbus:
 *     type: SQS
 *     sqs:
 *       queue-url: https://sqs.us-east-1.amazonaws.com/123456789012/fluxy-events
 *       region: us-east-1
 * }</pre>
 */
@Configuration
@ConditionalOnProperty(prefix = "fluxy.eventbus", name = "type", havingValue = "SQS")
@ConditionalOnClass(SqsTemplate.class)
public class FluxyAwsSqsEventBusAutoConfiguration {

    /**
     * Cliente asíncrono de SQS configurado con la región especificada en
     * {@code fluxy.eventbus.sqs.region}. Si no se especifica región, usa
     * la región por defecto del SDK.
     */
    @Bean
    public SqsAsyncClient fluxySqsAsyncClient(FluxyEventBusProperties properties) {
        FluxyEventBusProperties.Sqs sqs = properties.getSqs();
        var builder = SqsAsyncClient.builder();
        if (sqs.getRegion() != null && !sqs.getRegion().isBlank()) {
            builder.region(Region.of(sqs.getRegion()));
        }
        if (sqs.getEndpoint() != null && !sqs.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(sqs.getEndpoint()));
        }

        if (sqs.getAccessKey() != null && ! sqs.getAccessKey().isBlank() &&
                sqs.getSecretKey() != null && !sqs.getSecretKey().isBlank()) {
            var credentials = AwsBasicCredentials.create(sqs.getAccessKey(), sqs.getSecretKey());
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        }

        return builder.build();
    }

    /** Template de SQS que usa el cliente dedicado de Fluxy. */
    @Bean
    public SqsTemplate fluxySqsTemplate(SqsAsyncClient fluxySqsAsyncClient) {
        return SqsTemplate.builder().sqsAsyncClient(fluxySqsAsyncClient).build();
    }

    @Bean
    public SqsFluxyEventBus fluxyEventBus(
            SqsTemplate fluxySqsTemplate,
            FluxyEventBusProperties properties,
            List<FluxyEventHandler> handlers) {
        return new SqsFluxyEventBus(
                fluxySqsTemplate,
                FluxyEventBusObjectMapperFactory.create(),
                properties.getSqs().getQueueUrl(),
                handlers);
    }

    @Bean
    public FluxyAwsSqsListener fluxyAwsSqsListener(SqsFluxyEventBus fluxyEventBus) {
        return new FluxyAwsSqsListener(fluxyEventBus, FluxyEventBusObjectMapperFactory.create());
    }
}
