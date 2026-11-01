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

import java.io.IOException;
import java.util.Locale;

// All outgoing email goes through here, so the provider is a configuration
// choice, not a code change:
//
//   app.mail.enabled=false (default)  -> the message is written to the log
//                                        instead of being sent (local development)
//
//   app.mail.enabled=true
//     app.mail.provider=smtp (default) -> sent through Spring's JavaMailSender, configured
//                                        with the standard spring.mail.* properties. Works
//                                        with any SMTP provider (Resend, SES, Brevo, Gmail...)
//                                        on hosts that allow outbound SMTP.
//     app.mail.provider=gmail-api      -> sent through the Gmail REST API over HTTPS. Use this
//                                        where SMTP ports are blocked (e.g. Render's free
//                                        tier). Needs app.mail.gmail.* (see AUTH_UPGRADE.md).
//
// A provider that only offers an HTTP API can be supported the same way: add a small
// client class and one branch in send() - nothing else calls the mail code.
@Service
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;   // null unless provider=smtp and spring.mail.* is set
    private final GmailApiClient gmailClient;  // null unless provider=gmail-api
    private final boolean enabled;
    private final String from;

    public MailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from:no-reply@cadence.local}") String from,
            @Value("${app.mail.provider:smtp}") String provider,
            @Value("${app.mail.gmail.client-id:}") String gmailClientId,
            @Value("${app.mail.gmail.client-secret:}") String gmailClientSecret,
            @Value("${app.mail.gmail.refresh-token:}") String gmailRefreshToken) {
        this.enabled = enabled;
        this.from = from;

        JavaMailSender smtp = null;
        GmailApiClient gmail = null;

        if (enabled) {
            String chosen = provider == null ? "smtp" : provider.trim().toLowerCase(Locale.ROOT);
            switch (chosen) {
                case "smtp" -> {
                    smtp = mailSenderProvider.getIfAvailable();
                    if (smtp == null) {
                        throw new IllegalStateException(
                                "app.mail.enabled=true but no SMTP server is configured. Set spring.mail.host "
                                        + "(SPRING_MAIL_HOST) and the related spring.mail.* properties, or use "
                                        + "app.mail.provider=gmail-api.");
                    }
                }
                case "gmail-api" -> {
                    if (isBlank(gmailClientId) || isBlank(gmailClientSecret) || isBlank(gmailRefreshToken)) {
                        throw new IllegalStateException(
                                "app.mail.provider=gmail-api needs app.mail.gmail.client-id, client-secret and "
                                        + "refresh-token (GMAIL_CLIENT_ID, GMAIL_CLIENT_SECRET, GMAIL_REFRESH_TOKEN).");
                    }
                    gmail = new GmailApiClient(gmailClientId.trim(), gmailClientSecret.trim(), gmailRefreshToken.trim());
                }
                default -> throw new IllegalStateException(
                        "Unknown app.mail.provider '" + provider + "'. Use 'smtp' or 'gmail-api'.");
            }
            log.info("Email sending is ON (provider: {})", chosen);
        }

        this.mailSender = smtp;
        this.gmailClient = gmail;
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

        try {
            if (gmailClient != null) {
                gmailClient.send(from, to, subject, textBody);
            } else {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(textBody);
                mailSender.send(message);
            }
        } catch (MailException | IOException e) {
            log.error("Failed to send '{}' email to {}", subject, to, e);
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "The email could not be sent. Check the mail configuration and try again.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
