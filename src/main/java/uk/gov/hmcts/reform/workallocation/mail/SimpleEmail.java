package uk.gov.hmcts.reform.workallocation.mail;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import java.util.Properties;

public class SimpleEmail {

    /**
     Outgoing Mail (SMTP) Server
     requires TLS or SSL: smtp.gmail.com (use authentication)
     Use Authentication: Yes
     Port for TLS/STARTTLS: 587
     */
    public static void main(String[] args) {
        final String fromEmail = "ctsc-email-channel@HMCTS.NET";
        final String password = "London123";
//        final String toEmail = "sscs@hmcts.dcbp.co.uk";
        final String userName = "ctsc-email-channel@HMCTS.NET";
//        final String password = "Hungary123";
//        final String fromEmail = "ctsc.workallocation.service@hmcts.net";
        final String toEmail = "ctsc.workallocation.service@hmcts.net";
//        final String userName = "ctsc.workallocation.service@hmcts.net";

        System.out.println("TLSEmail Start");
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.office365.com"); //SMTP Host
        props.put("mail.smtp.port", "587"); //TLS Port
        props.put("mail.smtp.auth", "true"); //enable authentication
        props.put("mail.smtp.starttls.enable", "true"); //enable STARTTLS

        //create Authenticator object to pass in Session.getInstance argument
        Authenticator auth = new Authenticator() {
            //override the getPasswordAuthentication method
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userName, password);
            }
        };
        Session session = Session.getInstance(props, auth);

        EmailUtil.sendEmail(session, toEmail,"Submitted", "Event Description Test body");

    }


}

