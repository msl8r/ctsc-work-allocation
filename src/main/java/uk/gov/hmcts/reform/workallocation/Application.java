package uk.gov.hmcts.reform.workallocation;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.microsoft.applicationinsights.logback.ApplicationInsightsAppender;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;

@SpringBootApplication
@EnableCircuitBreaker
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application {

    public static final String WORKALLOCATION_APPENDER_NAME = "uk.gov.hmcts.reform.workallocation";

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger waLogger = lc.getLogger(WORKALLOCATION_APPENDER_NAME);
        ApplicationInsightsAppender appender = new ApplicationInsightsAppender();
        appender.setName("WA_INSIGHT_APPENDER");
        appender.setContext(lc);
        appender.start();

        waLogger.addAppender(appender);
    }
}
