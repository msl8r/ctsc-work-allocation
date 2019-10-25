package uk.gov.hmcts.reform.workallocation;

import ch.qos.logback.classic.Level;
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
        ApplicationInsightsAppender appender = new ApplicationInsightsAppender();
        appender.setName("WA_INSIGHT_APPENDER");
        appender.setContext(lc);
        appender.start();

        Logger waLogger = lc.getLogger(WORKALLOCATION_APPENDER_NAME);
        waLogger.setLevel(Level.INFO);
        waLogger.addAppender(appender);

        Logger rootLogger = lc.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.ERROR);
        rootLogger.addAppender(appender);
    }
}
