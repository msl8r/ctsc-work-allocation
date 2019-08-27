package uk.gov.hmcts.reform.workallocation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.gov.hmcts.reform.workallocation.model.Task;
import uk.gov.hmcts.reform.workallocation.queue.QueueClientSupplier;
import uk.gov.hmcts.reform.workallocation.queue.QueueConsumer;

import java.util.Properties;

@Configuration
@EnableScheduling
@EnableFeignClients(basePackages = {"uk.gov.hmcts.reform.workallocation.idam",
    "uk.gov.hmcts.reform.workallocation.ccd"})
public class AppConfig {

    @Bean
    public CloseableHttpClient httpClient(@Value("${http.client.timeout}") int timeout) {
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(timeout * 1000)
            .setConnectionRequestTimeout(timeout * 1000)
            .setSocketTimeout(timeout * 1000).build();
        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }

    @Bean
    public VelocityEngine getVelocityEngine() throws Exception {
        Properties props = new Properties();
        props.put("resource.loader", "class");
        props.put("class.resource.loader.class",
            "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        VelocityEngine engine = new VelocityEngine(props);
        engine.init();
        return engine;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());
    }

    @Bean
    public QueueClientSupplier getQueueClientSupplier(
        @Value("${servicebus.queue.connectionString}") String connectionString,
        @Value("${servicebus.queue.entityPath}") String entityPath) {
        return new QueueClientSupplier(connectionString, entityPath);
    }

    @Bean
    public QueueConsumer<Task> createTaskQueueConsumer(
        @Value("${servicebus.queue.connectionString}") String connectionString,
        @Value("${servicebus.queue.entityPath}") String entityPath,
        @Value("${ccd.deeplinkBaseUrl}") String deeplinkBaseUrl) {
        return new QueueConsumer<>(Task.class);
    }

}
