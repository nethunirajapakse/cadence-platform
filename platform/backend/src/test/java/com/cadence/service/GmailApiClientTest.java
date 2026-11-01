package com.cadence.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Runs GmailApiClient against a throwaway local HTTP server that imitates
// Google's token endpoint and the Gmail "send" endpoint. No network, no credentials.
class GmailApiClientTest {

    private HttpServer server;
    private String baseUrl;

    private final List<String> tokenRequests = new ArrayList<>();
    private final List<String> sendAuthHeaders = new ArrayList<>();
    private final List<String> sendBodies = new ArrayList<>();

    private int tokenStatus;
    private String tokenResponse;
    private final List<Integer> sendStatuses = new ArrayList<>();

    private final MutableClock clock = new MutableClock();

    @BeforeEach
    void startServer() throws IOException {
        tokenStatus = 200;
        tokenResponse = "{\"access_token\":\"tok-1\",\"expires_in\":3599,\"token_type\":\"Bearer\"}";
        sendStatuses.clear();
        sendStatuses.add(200);

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", ex -> {
            tokenRequests.add(read(ex));
            reply(ex, tokenStatus, tokenResponse);
        });
        server.createContext("/send", ex -> {
            sendAuthHeaders.add(ex.getRequestHeaders().getFirst("Authorization"));
            sendBodies.add(read(ex));
            int status = sendStatuses.size() > 1 ? sendStatuses.remove(0) : sendStatuses.get(0);
            reply(ex, status, status / 100 == 2 ? "{\"id\":\"m1\"}" : "{\"error\":{\"message\":\"nope\"}}");
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private GmailApiClient client() {
        return new GmailApiClient("my-id", "my secret", "my-refresh", baseUrl + "/token", baseUrl + "/send", clock);
    }

    @Test
    void sendsAMessageWithTheAccessToken() throws Exception {
        client().send("Cadence <me@example.com>", "you@example.com", "Hello", "Line one\nLine two");

        assertEquals(1, tokenRequests.size());
        String form = tokenRequests.get(0);
        assertTrue(form.contains("client_id=my-id"));
        assertTrue(form.contains("client_secret=my+secret"));
        assertTrue(form.contains("refresh_token=my-refresh"));
        assertTrue(form.contains("grant_type=refresh_token"));

        assertEquals(List.of("Bearer tok-1"), sendAuthHeaders);

        String json = sendBodies.get(0);
        assertTrue(json.startsWith("{\"raw\":\"") && json.endsWith("\"}"));
        String raw = json.substring(8, json.length() - 2);
        String message = new String(Base64.getUrlDecoder().decode(raw), StandardCharsets.UTF_8);

        assertTrue(message.contains("From: \"Cadence\" <me@example.com>\r\n"));
        assertTrue(message.contains("To: you@example.com\r\n"));
        assertTrue(message.contains("Subject: Hello\r\n"));
        assertTrue(message.contains("Content-Type: text/plain; charset=UTF-8\r\n"));

        String encodedBody = message.substring(message.indexOf("\r\n\r\n") + 4).trim();
        String body = new String(Base64.getMimeDecoder().decode(encodedBody), StandardCharsets.UTF_8);
        assertEquals("Line one\nLine two", body);
    }

    @Test
    void reusesTheAccessTokenUntilItIsAboutToExpire() throws Exception {
        GmailApiClient client = client();
        client.send("me@example.com", "you@example.com", "One", "a");
        client.send("me@example.com", "you@example.com", "Two", "b");
        assertEquals(1, tokenRequests.size());

        clock.advanceSeconds(3599 - 30); // inside the 60 second safety margin
        client.send("me@example.com", "you@example.com", "Three", "c");
        assertEquals(2, tokenRequests.size());
    }

    @Test
    void getsANewTokenAndRetriesOnceWhenGoogleRejectsTheOldOne() throws Exception {
        sendStatuses.clear();
        sendStatuses.add(401);
        sendStatuses.add(200);

        client().send("me@example.com", "you@example.com", "Hi", "x");

        assertEquals(2, tokenRequests.size());
        assertEquals(2, sendBodies.size());
    }

    @Test
    void reportsAFailedSend() {
        sendStatuses.clear();
        sendStatuses.add(403);

        IOException e = assertThrows(IOException.class,
                () -> client().send("me@example.com", "you@example.com", "Hi", "x"));
        assertTrue(e.getMessage().contains("HTTP 403"));
    }

    @Test
    void explainsAnExpiredRefreshToken() {
        tokenStatus = 400;
        tokenResponse = "{\"error\":\"invalid_grant\",\"error_description\":\"Token has been expired or revoked.\"}";

        IOException e = assertThrows(IOException.class,
                () -> client().send("me@example.com", "you@example.com", "Hi", "x"));
        assertTrue(e.getMessage().contains("refresh token is expired or revoked"));
        assertTrue(sendBodies.isEmpty());
    }

    @Test
    void neverPutsSecretsInTheErrorMessage() {
        tokenStatus = 500;
        tokenResponse = "oops";

        IOException e = assertThrows(IOException.class,
                () -> client().send("me@example.com", "you@example.com", "Hi", "x"));
        assertTrue(!e.getMessage().contains("my secret"));
        assertTrue(!e.getMessage().contains("my-refresh"));
    }

    @Test
    void rejectsLineBreaksInHeaders() {
        assertThrows(IllegalArgumentException.class,
                () -> client().send("me@example.com", "you@example.com", "Hi\r\nBcc: evil@example.com", "x"));
        assertThrows(IllegalArgumentException.class,
                () -> client().send("me@example.com", "you@example.com\nBcc: evil@example.com", "Hi", "x"));
        assertThrows(IllegalArgumentException.class,
                () -> client().send("Cadence\n<me@example.com>", "you@example.com", "Hi", "x"));
        assertTrue(sendBodies.isEmpty());
    }

    @Test
    void encodesNonAsciiHeaders() {
        String message = GmailApiClient.buildMessage("Cadence Ünï <me@example.com>", "you@example.com",
                "Welcome ✓", "Hi", ZonedDateTime.of(2026, 11, 2, 1, 0, 0, 0, ZoneOffset.UTC));

        assertTrue(message.contains("From: =?UTF-8?B?"));
        assertTrue(message.contains("<me@example.com>"));
        assertTrue(message.contains("Subject: =?UTF-8?B?"));
        assertTrue(message.contains("Date: Mon, 2 Nov 2026 01:00:00 GMT"));
    }

    // ---- helpers ----------------------------------------------------------------------

    private static String read(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void reply(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-02T00:00:00Z");

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
