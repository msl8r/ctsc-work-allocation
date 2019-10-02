package uk.gov.hmcts.reform.workallocation.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import org.apache.commons.lang.StringUtils;

public class LoggerStartupListener extends ContextAwareBase implements LoggerContextListener, LifeCycle {

    private static final String DEFAULT_DB_USER = "workallocation";
    private static final String DEFAULT_DB_PASS = "workallocation";
    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/workallocation";

    private boolean started = false;

    @Override
    public void start() {
        if (started) {
            return;
        }
        System.out.println(System.getenv("SPRING_DATASOURCE_USERNAME"));
        System.out.println(System.getenv("SPRING_DATASOURCE_PASSWORD"));
        System.out.println(System.getenv("SPRING_DATASOURCE_URL"));
        String dbUser = StringUtils.isEmpty(System.getenv("SPRING_DATASOURCE_USERNAME")) ? DEFAULT_DB_USER :
            System.getenv("SPRING_DATASOURCE_USERNAME");
        String dbPass = StringUtils.isEmpty(System.getenv("SPRING_DATASOURCE_PASSWORD")) ? DEFAULT_DB_PASS :
            System.getenv("SPRING_DATASOURCE_PASSWORD");
        String url = StringUtils.isEmpty(System.getenv("SPRING_DATASOURCE_URL")) ? DEFAULT_DB_URL :
            System.getenv("SPRING_DATASOURCE_URL");

        Context context = getContext();

        context.putProperty("DB_USER", dbUser);
        context.putProperty("DB_PASS", dbPass);
        context.putProperty("DB_URL", url);

        started = true;
    }

    @Override
    public boolean isResetResistant() {
        return false;
    }

    @Override
    public void onStart(LoggerContext context) {

    }

    @Override
    public void onReset(LoggerContext context) {

    }

    @Override
    public void onStop(LoggerContext context) {

    }

    @Override
    public void onLevelChange(Logger logger, Level level) {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isStarted() {
        return false;
    }
}
