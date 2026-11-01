package com.cadence.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Sends email through the Gmail REST API over HTTPS (port 443).
//
// Why this exists: some hosts (Render's free tier, for one) block outbound SMTP
// ports 25/465/587, so Gmail's SMTP server cannot be reached from there. The Gmail
// API is plain HTTPS, so it works everywhere, costs nothing, and needs no domain.
//
// It authenticates with an OAuth2 refresh token (obtained once, see
// AUTH_UPGRADE.md) and exchanges it for short-lived access tokens as needed.
//
// Deliberately plain Java (JDK only, no Spring, no JSON library) so it can be
// unit-tested against a fake server and cannot be broken by a dependency upgrade.
public class GmailApiClient {

    static final String DEFAULT_TOKEN_URL = "https://oauth2.googleapis.com/token";
    static final String DEFAULT_SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    private static final Pattern ADDRESS_WITH_NAME = Pattern.compile("^\\s*(.*?)\\s*<([^<>]+)>\\s*$");
    private static final Pattern PLAIN_ASCII = Pattern.compile("^[\\x20-\\x7E]*$");
    private static final Pattern EXPIRES_IN = Pattern.compile("\"expires_in\"\\s*:\\s*(\\d+)");

    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String tokenUrl;
    private final String sendUrl;
    private final Clock clock;
    private final HttpClient http;

    private String accessToken;
    private Instant accessTokenExpiresAt = Instant.EPOCH;

    public GmailApiClient(String clientId, String clientSecret, String refreshToken) {
        this(clientId, clientSecret, refreshToken, DEFAULT_TOKEN_URL, DEFAULT_SEND_URL, Clock.systemUTC());
    }

    // Package-private: lets tests point the client at a local fake server and fix the clock.
    GmailApiClient(String clientId, String clientSecret, String refreshToken,
                   String tokenUrl, String sendUrl, Clock clock) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.tokenUrl = tokenUrl;
        this.sendUrl = sendUrl;
        this.clock = clock;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /** Sends a plain-text email. Throws IOException on any failure (message never contains secrets). */
    public void send(String from, String to, String subject, String textBody) throws IOException {
        String message = buildMessage(from, to, subject, textBody, ZonedDateTime.now(clock.withZone(ZoneOffset.UTC)));
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(message.getBytes(StandardCharsets.UTF_8));
        // base64url output only contains [A-Za-z0-9_-], so it needs no JSON escaping.
        String json = "{\"raw\":\"" + raw + "\"}";

        HttpResponse<String> response = postJson(json, currentAccessToken());
        if (response.statusCode() == 401) {
            // The cached token was rejected (revoked or clock skew). Get a fresh one once.
            invalidateAccessToken();
            response = postJson(json, currentAccessToken());
        }
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Gmail API returned HTTP " + response.statusCode() + ": "
                    + truncate(response.body()));
        }
    }

    // ---- OAuth ------------------------------------------------------------------------

    private synchronized String currentAccessToken() throws IOException {
        if (accessToken != null && clock.instant().isBefore(accessTokenExpiresAt.minusSeconds(60))) {
            return accessToken;
        }

        String form = "client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret)
                + "&refresh_token=" + enc(refreshToken)
                + "&grant_type=refresh_token";

        HttpResponse<String> response = execute(HttpRequest.newBuilder(URI.create(tokenUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build());

        if (response.statusCode() / 100 != 2) {
            String body = response.body() == null ? "" : response.body();
            String hint = body.contains("invalid_grant")
                    ? " (the refresh token is expired or revoked - generate a new one)"
                    : "";
            throw new IOException("Google token endpoint returned HTTP " + response.statusCode()
                    + hint + ": " + truncate(body));
        }

        String token = extractString(response.body(), "access_token");
        if (token == null) {
            throw new IOException("Google token response had no access_token");
        }
        Matcher expires = EXPIRES_IN.matcher(response.body());
        long seconds = expires.find() ? Long.parseLong(expires.group(1)) : 3000L;

        this.accessToken = token;
        this.accessTokenExpiresAt = clock.instant().plusSeconds(seconds);
        return token;
    }

    private synchronized void invalidateAccessToken() {
        this.accessToken = null;
        this.accessTokenExpiresAt = Instant.EPOCH;
    }

    // ---- HTTP -------------------------------------------------------------------------

    private HttpResponse<String> postJson(String json, String bearer) throws IOException {
        return execute(HttpRequest.newBuilder(URI.create(sendUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + bearer)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build());
    }

    private HttpResponse<String> execute(HttpRequest request) throws IOException {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while contacting Google", e);
        }
    }

    // ---- message building --------------------------------------------------------------

    // Builds an RFC 5322 message by hand: it is small, has no attachments, and this keeps the
    // class free of any mail library. CR/LF in any header value is rejected to rule out header injection.
    static String buildMessage(String from, String to, String subject, String body, ZonedDateTime now) {
        rejectLineBreaks("from", from);
        rejectLineBreaks("to", to);
        rejectLineBreaks("subject", subject);

        String encodedBody = Base64.getMimeEncoder(76, new byte[] {'\r', '\n'})
                .encodeToString(body.getBytes(StandardCharsets.UTF_8));

        return "From: " + formatFrom(from) + "\r\n"
                + "To: " + to.trim() + "\r\n"
                + "Subject: " + encodeHeaderText(subject) + "\r\n"
                + "Date: " + DateTimeFormatter.RFC_1123_DATE_TIME.format(now) + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: base64\r\n"
                + "\r\n"
                + encodedBody + "\r\n";
    }

    private static String formatFrom(String from) {
        Matcher m = ADDRESS_WITH_NAME.matcher(from);
        if (!m.matches()) {
            return from.trim();
        }
        String name = m.group(1).trim();
        if (name.length() >= 2 && name.startsWith("\"") && name.endsWith("\"")) {
            name = name.substring(1, name.length() - 1);
        }
        String address = m.group(2).trim();
        if (name.isEmpty()) {
            return address;
        }
        String displayName = PLAIN_ASCII.matcher(name).matches()
                ? "\"" + name.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
                : encodeWord(name);
        return displayName + " <" + address + ">";
    }

    private static String encodeHeaderText(String text) {
        return PLAIN_ASCII.matcher(text).matches() ? text : encodeWord(text);
    }

    private static String encodeWord(String text) {
        return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)) + "?=";
    }

    private static void rejectLineBreaks(String field, String value) {
        if (value == null || value.isBlank() || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("Invalid email " + field);
        }
    }

    // ---- helpers ------------------------------------------------------------------------

    private static String extractString(String json, String field) {
        if (json == null) {
            return null;
        }
        Matcher m = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"\\\\]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String truncate(String text) {
        if (text == null) {
            return "";
        }
        String single = text.replaceAll("\\s+", " ").trim();
        return single.length() <= 300 ? single : single.substring(0, 300) + "...";
    }
}
