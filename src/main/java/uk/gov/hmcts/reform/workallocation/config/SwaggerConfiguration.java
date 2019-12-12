package uk.gov.hmcts.reform.workallocation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import uk.gov.hmcts.reform.workallocation.Application;

import static springfox.documentation.builders.RequestHandlerSelectors.basePackage;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .groupName("work-allocation")
            .apiInfo(waApiInfo()).select()
            .apis(basePackage(Application.class.getPackage().getName()))
            .build();
    }

    private ApiInfo waApiInfo() {
        return new ApiInfoBuilder()
            .title("CTSC-WORK-ALLOCATIOM-API")
            .description("Automated service to send tasks to 8x8 queues")
            .contact(new Contact("Attila Kiss", "", "attila.kiss@hmcts.net"))
            .version("1.0")
            .build();
    }

}
