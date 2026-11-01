package com.cadence.service;

import com.cadence.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// All outgoing email goes through here, so the provider is a configuration
// choice, not a code change:
//
//   app.mail.enabled=false (default)  -> the message is written to the log
//                                        instead of being sent (local development)
//   app.mail.enabled=true             -> sent through Spring's JavaMailSender, which
//                                        is configured with the standard spring.mail.*
//                                        properties (host, port, username, password,
//                                        STARTTLS). Works with any SMTP provider -
//                                        Resend, SES, Brevo, Gmail, Mailtrap, ...
//
// A provider that only offers an HTTP API can be supported later by changing
// the body of send() - nothing else calls the mail library.
@Service
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public MailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from:no-reply@cadence.local}") String from) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.enabled = enabled;
        this.from = from;
        if (enabled && this.mailSender == null) {
            throw new IllegalStateException(
                    "app.mail.enabled=true but no SMTP server is configured. Set spring.mail.host "
                            + "(SPRING_MAIL_HOST) and the related spring.mail.* properties.");
        }
    }

    public void sendInvitation(String to, String inviterName, String roleLabel, String link, long validForHours) {
        String body = "Hello,\n\n"
                + inviterName + " has invited you to join Cadence as " + roleLabel + ".\n\n"
                + "Set up your account here:\n" + link + "\n\n"
                + "This link works once and expires in " + validForHours + " hours.\n"
                + "If you weren't expecting this invitation, you can ignore this email.\n";
        send(to, "You're invited to Cadence", body);
    }

    public void sendPasswordReset(String to, String link, long validForMinutes) {
        String body = "Hello,\n\n"
                + "We received a request to reset your Cadence password. Choose a new one here:\n"
                + link + "\n\n"
                + "This link works once and expires in " + validForMinutes + " minutes.\n"
                + "If you didn't ask for this, you can ignore this email - your password has not changed.\n";
        send(to, "Reset your Cadence password", body);
    }

    private void send(String to, String subject, String textBody) {
        if (!enabled) {
            // Development mode: the log line IS the email, including the link.
            log.info("[mail disabled] To: {} | Subject: {}\n{}", to, subject, textBody);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(textBody);

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send '{}' email to {}", subject, to, e);
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "The email could not be sent. Check the mail configuration and try again.");
        }
    }
}
