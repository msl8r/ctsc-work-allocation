package uk.gov.hmcts.reform.workallocation.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

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
}
