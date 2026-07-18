package com.frauddetection.platform;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentFraudApplicationTests {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetStepUpState() {
        jdbcTemplate.update("delete from step_up_operator_security_state");
        jdbcTemplate.update("delete from one_time_tokens");
        jdbcTemplate.update("delete from step_up_token_deliveries");
    }

    @Test
    void contextLoads() {
    }

    @Test
    void operatorCanCreateCookieSessionWithoutPersistingBasicCredentials() throws IOException, InterruptedException {
        HttpClient sessionClient = sessionHttpClient();
        HttpResponse<String> csrfResponse = sendForBody(
            sessionClient,
            request("/api/v1/auth/csrf").GET().build()
        );
        assertThat(HttpStatus.valueOf(csrfResponse.statusCode())).isEqualTo(HttpStatus.OK);
        String csrfToken = extractJsonValue(csrfResponse.body(), "token");

        HttpResponse<String> loginResponse = sendForBody(
            sessionClient,
            request("/api/v1/auth/session")
                .header("Content-Type", "application/json")
                .header("X-XSRF-TOKEN", csrfToken)
                .POST(HttpRequest.BodyPublishers.ofString("""
                    {"username":"analyst.one","password":"local-analyst-2026"}
                    """))
                .build()
        );
        assertThat(HttpStatus.valueOf(loginResponse.statusCode())).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.body()).contains("analyst.one").doesNotContain("local-analyst-2026");

        HttpStatus authenticatedStatus = send(
            sessionClient,
            request("/api/v1/fraud-operations/summary").GET().build()
        );
        assertThat(authenticatedStatus).isEqualTo(HttpStatus.OK);
    }

    @Test
    void anonymousFraudCaseRequestIsRejected() throws IOException, InterruptedException {
        HttpStatus status = send(request("/api/v1/fraud-cases").GET().build());
        assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void healthEndpointIsPublic() throws IOException, InterruptedException {
        HttpStatus status = send(request("/actuator/health").GET().build());
        assertThat(status).isEqualTo(HttpStatus.OK);
    }

    @Test
    void analystCannotUseSupervisorDecisionRoute() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-cases/00000000-0000-0000-0000-000000000001/release",
            "analyst.one",
            "local-analyst-2026"
        ).POST(HttpRequest.BodyPublishers.noBody()).build());
        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void analystCanReadFraudCaseQueue() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-cases",
            "analyst.one",
            "local-analyst-2026"
        ).GET().build());
        assertThat(status).isEqualTo(HttpStatus.OK);
    }

    @Test
    void analystCanReadFilteredFraudCaseQueue() throws IOException, InterruptedException {
        HttpStatus createStatus = send(authenticatedRequest(
            "/api/v1/fraud-assessments",
            "ingest.client",
            "local-ingest-2026"
        ).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("""
            {
              "paymentId":"PAY-FILTER-HTTP-1",
              "customerId":"CUST-FILTER-HTTP-1",
              "amount":12000.00,
              "currency":"ZAR",
              "merchantCategory":"ELECTRONICS",
              "paymentChannel":"MOBILE_APP",
              "customerAverageTicket":1400.00,
              "transactionCountLastFiveMinutes":5,
              "spendLastHour":17000.00,
              "beneficiaryAgeHours":1,
              "newDevice":true,
              "highRiskCountry":false,
              "impossibleTravel":true,
              "recentPasswordReset":true
            }
            """)).build());
        assertThat(createStatus).isEqualTo(HttpStatus.OK);

        HttpResponse<String> response = sendForBody(authenticatedRequest(
            "/api/v1/fraud-cases?status=OPEN&paymentId=PAY-FILTER-HTTP-1&minRiskScore=70",
            "analyst.one",
            "local-analyst-2026"
        ).GET().build());
        assertThat(HttpStatus.valueOf(response.statusCode())).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("PAY-FILTER-HTTP-1");
    }

    @Test
    void analystCannotExportFraudCases() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-cases/export",
            "analyst.one",
            "local-analyst-2026"
        ).GET().build());
        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void supervisorCannotExportFraudCasesWithoutStepUp() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-cases/export",
            "senior.analyst",
            "local-senior-2026"
        ).GET().build());
        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void supervisorCanExportFraudCasesAfterStepUp() throws IOException, InterruptedException {
        HttpStatus createStatus = send(authenticatedRequest(
            "/api/v1/fraud-assessments",
            "ingest.client",
            "local-ingest-2026"
        ).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("""
            {
              "paymentId":"PAY-EXPORT-HTTP-1",
              "customerId":"CUST-EXPORT-HTTP-1",
              "amount":13000.00,
              "currency":"ZAR",
              "merchantCategory":"ELECTRONICS",
              "paymentChannel":"MOBILE_APP",
              "customerAverageTicket":1500.00,
              "transactionCountLastFiveMinutes":6,
              "spendLastHour":18000.00,
              "beneficiaryAgeHours":1,
              "newDevice":true,
              "highRiskCountry":false,
              "impossibleTravel":true,
              "recentPasswordReset":true
            }
            """)).build());
        assertThat(createStatus).isEqualTo(HttpStatus.OK);

        HttpClient sessionClient = sessionHttpClient();
        verifyStepUp(sessionClient, "senior.analyst", "local-senior-2026");

        HttpResponse<String> response = sendForBody(sessionClient, authenticatedRequest(
            "/api/v1/fraud-cases/export?paymentId=PAY-EXPORT-HTTP-1",
            "senior.analyst",
            "local-senior-2026"
        ).GET().build());
        assertThat(HttpStatus.valueOf(response.statusCode())).isEqualTo(HttpStatus.OK);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(value -> assertThat(value).contains("text/csv"));
        assertThat(response.body()).contains("PAY-EXPORT-HTTP-1");
    }

    @Test
    void stepUpTokenCannotBeReusedAfterVerification() throws IOException, InterruptedException {
        HttpClient sessionClient = sessionHttpClient();
        String verificationUrl = requestStepUpVerificationUrl(sessionClient, "senior.analyst", "local-senior-2026");

        HttpStatus firstVerification = send(sessionClient, authenticatedAbsoluteRequest(
            verificationUrl,
            "senior.analyst",
            "local-senior-2026"
        ).GET().build());
        assertThat(firstVerification).isEqualTo(HttpStatus.OK);

        HttpStatus secondVerification = send(sessionClient, authenticatedAbsoluteRequest(
            verificationUrl,
            "senior.analyst",
            "local-senior-2026"
        ).GET().build());
        assertThat(secondVerification).isIn(HttpStatus.FORBIDDEN, HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void resendingStepUpTokenRevokesPreviousToken() throws IOException, InterruptedException {
        HttpClient sessionClient = sessionHttpClient();
        String originalVerificationUrl = requestStepUpVerificationUrl(sessionClient, "senior.analyst", "local-senior-2026");

        HttpResponse<String> resendResponse = sendForBody(sessionClient, authenticatedRequest(
            "/api/v1/security/step-up/token/resend",
            "senior.analyst",
            "local-senior-2026"
        ).POST(HttpRequest.BodyPublishers.noBody()).build());
        assertThat(HttpStatus.valueOf(resendResponse.statusCode())).isEqualTo(HttpStatus.ACCEPTED);

        String resentVerificationUrl = extractJsonValue(resendResponse.body(), "verificationUrl");
        assertThat(resentVerificationUrl).isNotBlank();

        HttpStatus originalStatus = send(sessionClient, authenticatedAbsoluteRequest(
            originalVerificationUrl,
            "senior.analyst",
            "local-senior-2026"
        ).GET().build());
        assertThat(originalStatus).isEqualTo(HttpStatus.FORBIDDEN);

        HttpStatus resentStatus = send(sessionClient, authenticatedAbsoluteRequest(
            resentVerificationUrl,
            "senior.analyst",
            "local-senior-2026"
        ).GET().build());
        assertThat(resentStatus).isEqualTo(HttpStatus.OK);
    }

    @Test
    void revokingOutstandingStepUpTokenBlocksItsVerification() throws IOException, InterruptedException {
        HttpClient sessionClient = sessionHttpClient();
        String verificationUrl = requestStepUpVerificationUrl(sessionClient, "senior.analyst", "local-senior-2026");

        HttpStatus revokeStatus = send(sessionClient, authenticatedRequest(
            "/api/v1/security/step-up/revoke",
            "senior.analyst",
            "local-senior-2026"
        ).POST(HttpRequest.BodyPublishers.noBody()).build());
        assertThat(revokeStatus).isEqualTo(HttpStatus.OK);

        HttpStatus verifyStatus = send(sessionClient, authenticatedAbsoluteRequest(
            verificationUrl,
            "senior.analyst",
            "local-senior-2026"
        ).GET().build());
        assertThat(verifyStatus).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void supervisorCanReadOwnStepUpDeliveryAudit() throws IOException, InterruptedException {
        HttpClient sessionClient = sessionHttpClient();
        requestStepUpVerificationUrl(sessionClient, "senior.analyst", "local-senior-2026");

        HttpResponse<String> response = sendForBody(sessionClient, authenticatedRequest(
            "/api/v1/security/step-up/deliveries?limit=5",
            "senior.analyst",
            "local-senior-2026"
        ).GET().build());
        assertThat(HttpStatus.valueOf(response.statusCode())).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("SENT");
        assertThat(response.body()).contains("development-link");
    }

    @Test
    void stepUpTokenGenerationIsRateLimitedAfterTooManyRequests() throws IOException, InterruptedException {
        HttpClient sessionClient = sessionHttpClient();
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpStatus status = send(sessionClient, authenticatedRequest(
                "/api/v1/security/step-up/token",
                "senior.analyst",
                "local-senior-2026"
            ).POST(HttpRequest.BodyPublishers.noBody()).build());
            assertThat(status).isEqualTo(HttpStatus.ACCEPTED);
        }

        HttpResponse<String> limitedResponse = sendForBody(sessionClient, authenticatedRequest(
            "/api/v1/security/step-up/token",
            "senior.analyst",
            "local-senior-2026"
        ).POST(HttpRequest.BodyPublishers.noBody()).build());
        assertThat(HttpStatus.valueOf(limitedResponse.statusCode())).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(limitedResponse.body()).contains("Retry after");
    }

    @Test
    void stepUpVerificationIsLockedAfterRepeatedFailures() throws IOException, InterruptedException {
        HttpClient sessionClient = sessionHttpClient();
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpStatus status = send(sessionClient, authenticatedRequest(
                "/api/v1/security/step-up/verify?token=definitely-invalid-token",
                "senior.analyst",
                "local-senior-2026"
            ).GET().build());
            assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
        }

        HttpResponse<String> lockedResponse = sendForBody(sessionClient, authenticatedRequest(
            "/api/v1/security/step-up/verify?token=definitely-invalid-token",
            "senior.analyst",
            "local-senior-2026"
        ).GET().build());
        assertThat(HttpStatus.valueOf(lockedResponse.statusCode())).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(lockedResponse.body()).contains("Retry after");

        HttpStatus tokenRequestWhileLocked = send(sessionClient, authenticatedRequest(
            "/api/v1/security/step-up/token",
            "senior.analyst",
            "local-senior-2026"
        ).POST(HttpRequest.BodyPublishers.noBody()).build());
        assertThat(tokenRequestWhileLocked).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void analystCanReadFraudScoringRules() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-assessments/rules",
            "analyst.one",
            "local-analyst-2026"
        ).GET().build());
        assertThat(status).isEqualTo(HttpStatus.OK);
    }

    @Test
    void analystCanReadActiveFraudScoringProfile() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-assessments/scoring-profiles/active",
            "analyst.one",
            "local-analyst-2026"
        ).GET().build());
        assertThat(status).isEqualTo(HttpStatus.OK);
    }

    @Test
    void analystCanCreateFraudScoringProfile() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-assessments/scoring-profiles",
            "analyst.one",
            "local-analyst-2026"
        ).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("""
            {
              "profileName":"July rollout candidate",
              "challengeThreshold":42,
              "holdThreshold":63,
              "declineThreshold":84,
              "changeSummary":"Lower early friction while keeping the decline boundary conservative."
            }
            """)).build());
        assertThat(status).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void scoringClientCannotReadFraudScoringRules() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-assessments/rules",
            "ingest.client",
            "local-ingest-2026"
        ).GET().build());
        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void analystCannotActivateFraudScoringProfile() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-assessments/scoring-profiles/00000000-0000-0000-0000-000000000001/activate",
            "analyst.one",
            "local-analyst-2026"
        ).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("""
            {}
            """)).build());
        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void supervisorCanActivateFraudScoringProfile() throws IOException, InterruptedException {
        HttpStatus deniedStatus = send(authenticatedRequest(
            "/api/v1/fraud-assessments/scoring-profiles/00000000-0000-0000-0000-000000000001/activate",
            "senior.analyst",
            "local-senior-2026"
        ).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("""
            {}
            """)).build());
        assertThat(deniedStatus).isEqualTo(HttpStatus.FORBIDDEN);

        HttpClient sessionClient = sessionHttpClient();
        verifyStepUp(sessionClient, "senior.analyst", "local-senior-2026");

        HttpStatus status = send(sessionClient, authenticatedRequest(
            "/api/v1/fraud-assessments/scoring-profiles/00000000-0000-0000-0000-000000000001/activate",
            "senior.analyst",
            "local-senior-2026"
        ).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("""
            {}
            """)).build());
        assertThat(status).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void analystCanReadFraudOperationsSummary() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-operations/summary",
            "analyst.one",
            "local-analyst-2026"
        ).GET().build());
        assertThat(status).isEqualTo(HttpStatus.OK);
    }

    @Test
    void analystCanCompletePaymentChallenge() throws IOException, InterruptedException {
        HttpStatus createStatus = send(authenticatedRequest(
            "/api/v1/fraud-assessments",
            "ingest.client",
            "local-ingest-2026"
        ).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("""
            {
              "paymentId":"PAY-CHALLENGE-1",
              "customerId":"CUST-CHALLENGE-1",
              "amount":5000.00,
              "currency":"ZAR",
              "merchantCategory":"ELECTRONICS",
              "paymentChannel":"MOBILE_APP",
              "customerAverageTicket":1500.00,
              "transactionCountLastFiveMinutes":0,
              "spendLastHour":2000.00,
              "beneficiaryAgeHours":48,
              "newDevice":true,
              "highRiskCountry":false,
              "impossibleTravel":false,
              "recentPasswordReset":false
            }
            """)).build());
        assertThat(createStatus).isEqualTo(HttpStatus.OK);

        HttpStatus status = send(authenticatedRequest(
            "/api/v1/payments/PAY-CHALLENGE-1/challenge-outcome",
            "analyst.one",
            "local-analyst-2026"
        ).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("""
            {
              "outcome":"PASSED",
              "note":"Customer completed OTP challenge."
            }
            """)).build());
        assertThat(status).isEqualTo(HttpStatus.OK);
    }

    @Test
    void scoringClientCannotReadFraudOperationsSummary() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-operations/summary",
            "ingest.client",
            "local-ingest-2026"
        ).GET().build());
        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void analystCanReadOutboundEventOperations() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-operations/outbound-events?status=FAILED&topicName=fraud-notifications&limit=10",
            "analyst.one",
            "local-analyst-2026"
        ).GET().build());
        assertThat(status).isEqualTo(HttpStatus.OK);
    }

    @Test
    void analystCanReadOutboundEventAnalytics() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-operations/outbound-events/analytics?days=7",
            "analyst.one",
            "local-analyst-2026"
        ).GET().build());
        assertThat(status).isEqualTo(HttpStatus.OK);
    }

    @Test
    void analystCanExportFailedOutboundEvents() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-operations/outbound-events/failed-export?limit=10",
            "analyst.one",
            "local-analyst-2026"
        ).GET().build());
        assertThat(status).isEqualTo(HttpStatus.OK);
    }

    @Test
    void analystCannotRetryOutboundEvent() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-operations/outbound-events/00000000-0000-0000-0000-000000000001/retry",
            "analyst.one",
            "local-analyst-2026"
        ).POST(HttpRequest.BodyPublishers.noBody()).build());
        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void analystCannotAddOutboundIncidentNote() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-operations/outbound-events/00000000-0000-0000-0000-000000000001/incident-note",
            "analyst.one",
            "local-analyst-2026"
        ).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("""
            {
              "note":"Observed delivery issue."
            }
            """)).build());
        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void scoringClientCannotReadOutboundEventAnalytics() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-operations/outbound-events/analytics?days=7",
            "ingest.client",
            "local-ingest-2026"
        ).GET().build());
        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void supervisorCanRetryOutboundEvent() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-operations/outbound-events/00000000-0000-0000-0000-000000000001/retry",
            "senior.analyst",
            "local-senior-2026"
        ).POST(HttpRequest.BodyPublishers.noBody()).build());
        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void supervisorCanAddOutboundIncidentNote() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-operations/outbound-events/00000000-0000-0000-0000-000000000001/incident-note",
            "senior.analyst",
            "local-senior-2026"
        ).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("""
            {
              "note":"Investigating broker failure."
            }
            """)).build());
        assertThat(status).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void supervisorCanRetryFailedOutboundEventsInBatch() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-operations/outbound-events/retry-failed?limit=5",
            "senior.analyst",
            "local-senior-2026"
        ).POST(HttpRequest.BodyPublishers.noBody()).build());
        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void supervisorCanTriggerOutboundDispatchSweepAfterStepUp() throws IOException, InterruptedException {
        HttpClient sessionClient = sessionHttpClient();
        verifyStepUp(sessionClient, "senior.analyst", "local-senior-2026");

        HttpStatus status = send(sessionClient, authenticatedRequest(
            "/api/v1/fraud-operations/outbound-events/dispatch-now",
            "senior.analyst",
            "local-senior-2026"
        ).POST(HttpRequest.BodyPublishers.noBody()).build());
        assertThat(status).isEqualTo(HttpStatus.OK);
    }

    @Test
    void analystCanReadFraudReplayBatches() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-replays",
            "analyst.one",
            "local-analyst-2026"
        ).GET().build());
        assertThat(status).isEqualTo(HttpStatus.OK);
    }

    @Test
    void analystCanCreateFraudReplayBatch() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-replays",
            "analyst.one",
            "local-analyst-2026"
        ).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("""
            {
              "batchName":"Smoke replay",
              "scenarios":[
                {
                  "paymentId":"PAY-REPLAY-1",
                  "customerId":"CUST-REPLAY-1",
                  "amount":5000.00,
                  "currency":"ZAR",
                  "merchantCategory":"ELECTRONICS",
                  "paymentChannel":"MOBILE_APP",
                  "customerAverageTicket":1500.00,
                  "transactionCountLastFiveMinutes":3,
                  "spendLastHour":8000.00,
                  "beneficiaryAgeHours":4,
                  "newDevice":true,
                  "highRiskCountry":false,
                  "impossibleTravel":false,
                  "recentPasswordReset":false
                }
              ]
            }
            """)).build());
        assertThat(status).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void scoringClientCannotReadFraudReplayBatches() throws IOException, InterruptedException {
        HttpStatus status = send(authenticatedRequest(
            "/api/v1/fraud-replays",
            "ingest.client",
            "local-ingest-2026"
        ).GET().build());
        assertThat(status).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    }

    private HttpRequest.Builder authenticatedRequest(String path, String username, String password) {
        String token = Base64.getEncoder()
            .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return request(path).header("Authorization", "Basic " + token);
    }

    private HttpStatus send(HttpRequest request) throws IOException, InterruptedException {
        return send(HTTP_CLIENT, request);
    }

    private HttpStatus send(HttpClient httpClient, HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        return HttpStatus.valueOf(response.statusCode());
    }

    private HttpResponse<String> sendForBody(HttpClient httpClient, HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpClient sessionHttpClient() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        return HttpClient.newBuilder().cookieHandler(cookieManager).build();
    }

    private void verifyStepUp(HttpClient httpClient, String username, String password) throws IOException, InterruptedException {
        String verificationUrl = requestStepUpVerificationUrl(httpClient, username, password);

        HttpStatus verifyStatus = send(httpClient, authenticatedAbsoluteRequest(
            verificationUrl,
            username,
            password
        ).GET().build());
        assertThat(verifyStatus).isEqualTo(HttpStatus.OK);
    }

    private String requestStepUpVerificationUrl(HttpClient httpClient, String username, String password) throws IOException, InterruptedException {
        HttpResponse<String> tokenResponse = sendForBody(httpClient, authenticatedRequest(
            "/api/v1/security/step-up/token",
            username,
            password
        ).POST(HttpRequest.BodyPublishers.noBody()).build());
        assertThat(HttpStatus.valueOf(tokenResponse.statusCode())).isEqualTo(HttpStatus.ACCEPTED);

        String verificationUrl = extractJsonValue(tokenResponse.body(), "verificationUrl");
        assertThat(verificationUrl).isNotBlank();
        return verificationUrl;
    }

    private String extractJsonValue(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private HttpResponse<String> sendForBody(HttpRequest request) throws IOException, InterruptedException {
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder authenticatedAbsoluteRequest(String url, String username, String password) {
        String token = Base64.getEncoder()
            .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Basic " + token);
    }
}
