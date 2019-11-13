package uk.gov.hmcts.reform.workallocation.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Properties;
import javax.mail.Session;
import javax.mail.Transport;

@Component
public class MailHealthCheck implements HealthIndicator {

    private final String host;
    private final int port;
    private final String user;
    private final String password;

    private final Session session;

    public MailHealthCheck(@Value("${smtp.host}") String host,
                           @Value("${smtp.port}") int port,
                           @Value("${smtp.user}") String user,
                           @Value("${smtp.password}") String password) {

        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "*");
        this.session = Session.getInstance(props, null);

    }

    @Override
    public Health health() {
        try {
            checkMailServer();
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withDetail("Error: ", e.getMessage()).build();
        }
    }

    private String checkMailServer() throws Exception {
        Transport transport = session.getTransport("smtp");
        transport.connect(host, port, user, password);
        transport.close();
        return "success";
    }

}
