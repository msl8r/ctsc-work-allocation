package uk.gov.hmcts.reform.workallocation.mail;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

public class SimpleEmailNewServer {

    private SimpleEmailNewServer() {
        super();
    }

    /**
     Outgoing Mail (SMTP) Server
     requires TLS or SSL: smtp.gmail.com (use authentication)
     Use Authentication: Yes
     Port for TLS/STARTTLS: 587
     */
    public static void main(String[] args) {
        final String password = "Password12";
        final String userName = "hmcts@contact.justice.sandbox.platform.hmcts.net";
        final String toEmail = "hmcts@contact.justice.sandbox.platform.hmcts.net";

        System.out.println("TLSEmail Start");
        Properties props = new Properties();
        props.put("mail.smtp.host", "ctscmail.sandbox.platform.hmcts.net");
        props.put("mail.smtp.port", "25");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.from", toEmail);
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "*");
        //  props.put("mail.smtp.ssl.enable", "true");

        //create Authenticator object to pass in Session.getInstance argument
        Authenticator auth = new Authenticator() {
            //override the getPasswordAuthentication method
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userName, password);
            }
        };
        Session session = Session.getInstance(props, auth);

        EmailUtil.sendEmail(session, toEmail,"Email Testing Subject ",
            "Email Testing Body from new email server configuration");

    }


}

