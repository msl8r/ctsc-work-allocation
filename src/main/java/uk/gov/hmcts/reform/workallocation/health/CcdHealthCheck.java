package uk.gov.hmcts.reform.workallocation.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

@Component
public class CcdHealthCheck  extends BaseHealthCheck {

    @Autowired
    public CcdHealthCheck(RestTemplateBuilder restTemplateBuilder, @Value("${ccd.baseUrl}") String ccdUrl) {
        super(restTemplateBuilder, ccdUrl);
    }

}
