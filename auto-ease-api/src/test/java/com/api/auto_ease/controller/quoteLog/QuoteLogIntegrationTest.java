package com.api.auto_ease.controller.quoteLog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class QuoteLogIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private String toyotaMakeId;
    private String corollaModelId;
    private String oilChangeCategoryId;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_REF =
            new ParameterizedTypeReference<>() {};

    private String uniqueEmail() {
        return "ql-test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private String registerAndGetToken(String email, String userType) {
        var req = Map.of(
                "email", email,
                "password", "pass123",
                "fullName", "Test User",
                "userType", userType
        );
        var resp = rest.postForEntity("/api/auth/register", req, Map.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return (String) resp.getBody().get("token");
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @BeforeEach
    void lookupReferenceData() {
        var makesResp = rest.exchange("/api/car-makes", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        toyotaMakeId = makesResp.getBody().stream()
                .filter(make -> "Toyota".equals(make.get("name")))
                .map(make -> make.get("id").toString())
                .findFirst().orElseThrow();

        var modelsResp = rest.exchange("/api/car-makes/" + toyotaMakeId + "/models", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        corollaModelId = modelsResp.getBody().stream()
                .filter(model -> "Corolla".equals(model.get("name")))
                .map(model -> model.get("id").toString())
                .findFirst().orElseThrow();

        var categoriesResp = rest.exchange("/api/service-categories", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        oilChangeCategoryId = categoriesResp.getBody().stream()
                .filter(category -> "Oil Change".equals(category.get("name")))
                .map(category -> category.get("id").toString())
                .findFirst().orElseThrow();
    }

    private Map<String, Object> addCar(String token) {
        var body = Map.of("makeId", toyotaMakeId, "modelId", corollaModelId, "year", 2022);
        var resp = rest.exchange("/api/cars", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(token)), MAP_REF);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return resp.getBody();
    }

    private Map<String, Object> createJobRequest(String token, Object carId) {
        var body = new HashMap<String, Object>();
        body.put("carId", carId);
        body.put("categoryId", oilChangeCategoryId);
        body.put("title", "Oil change");
        body.put("urgency", "NORMAL");
        var resp = rest.exchange("/api/job-requests", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(token)), MAP_REF);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return resp.getBody();
    }

    private Map<String, Object> createGarage(String token, String name) {
        var body = Map.of("businessName", name, "city", "București", "phone", "+40741000000");
        var resp = rest.exchange("/api/garages", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(token)), MAP_REF);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return resp.getBody();
    }

    private Map<String, Object> submitQuote(String garageToken, String jobId, double price) {
        var body = Map.of("price", price, "estimatedDuration", "2h", "description", "Initial work");
        var resp = rest.exchange("/api/job-requests/" + jobId + "/quotes", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(garageToken)), MAP_REF);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return resp.getBody();
    }

    private void acceptQuote(String ownerToken, String quoteId) {
        var body = Map.of(
                "addendumFlow", false,
                "scheduledDate", "2026-04-10",
                "scheduledTime", "10:00");
        var resp = rest.exchange("/api/quotes/" + quoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(ownerToken)), MAP_REF);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    private record FullSetup(String ownerToken, String garageToken, String jobId,
                             String originalQuoteId, String garageId) {
    }

    private FullSetup fullSetupAcceptedOriginalQuote() {
        String ownerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        Map<String, Object> car = addCar(ownerToken);
        Map<String, Object> job = createJobRequest(ownerToken, car.get("id"));

        String garageToken = registerAndGetToken(uniqueEmail(), "GARAGE");
        Map<String, Object> garage = createGarage(garageToken, "AutoService Pro " + UUID.randomUUID());
        Map<String, Object> quote = submitQuote(garageToken, job.get("id").toString(), 250.00);
        acceptQuote(ownerToken, quote.get("id").toString());

        return new FullSetup(ownerToken, garageToken,
                job.get("id").toString(), quote.get("id").toString(),
                garage.get("id").toString());
    }

    // ---------- Tests ----------

    @Test
    void plainComment_carOwnerCanSeeUnreadLog() {
        FullSetup s = fullSetupAcceptedOriginalQuote();

        var body = Map.of("message", "Just checking in", "notificationFlag", true, "updateFlag", false);
        var createResp = rest.exchange("/api/quotes/" + s.originalQuoteId + "/logs", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(s.garageToken)), MAP_REF);

        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());
        Map<String, Object> created = createResp.getBody();
        assertEquals("CAR_OWNER", created.get("typeOfNotification"));
        assertEquals(Boolean.TRUE, created.get("notificationFlag"));
        assertNull(created.get("triggeredQuoteId"));

        var listResp = rest.exchange("/api/quote-logs?notificationFlag=true", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.ownerToken)), MAP_REF);
        assertEquals(HttpStatus.OK, listResp.getStatusCode());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> logs = (List<Map<String, Object>>) listResp.getBody().get("logs");
        assertFalse(logs.isEmpty());
        assertEquals(created.get("id"), logs.get(0).get("id"));
        assertEquals("CAR_OWNER", logs.get(0).get("typeOfNotification"));
    }

    @Test
    void carOwnerCannotCreateLog() {
        FullSetup s = fullSetupAcceptedOriginalQuote();
        var body = Map.of("message", "Hi", "notificationFlag", true);
        var resp = rest.exchange("/api/quotes/" + s.originalQuoteId + "/logs", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(s.ownerToken)), String.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void garageThatDoesNotOwnQuoteCannotCreateLog() {
        FullSetup s = fullSetupAcceptedOriginalQuote();

        String otherGarageToken = registerAndGetToken(uniqueEmail(), "GARAGE");
        createGarage(otherGarageToken, "Other " + UUID.randomUUID());

        var body = Map.of("message", "Should fail", "notificationFlag", true);
        var resp = rest.exchange("/api/quotes/" + s.originalQuoteId + "/logs", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(otherGarageToken)), String.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void addendumQuoteCreatedThroughLog_acceptFlow() {
        FullSetup s = fullSetupAcceptedOriginalQuote();

        var newQuotePayload = Map.of(
                "price", 120.00,
                "estimatedDuration", "3 days",
                "description", "Oil spill repair"
        );
        var body = Map.of(
                "message", "Oil spill found. Approve?",
                "notificationFlag", true,
                "updateFlag", true,
                "newQuote", newQuotePayload
        );
        var createResp = rest.exchange("/api/quotes/" + s.originalQuoteId + "/logs", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(s.garageToken)), MAP_REF);
        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());

        Map<String, Object> originatingLog = createResp.getBody();
        String addendumQuoteId = String.valueOf(originatingLog.get("triggeredQuoteId"));
        assertNotEquals("null", addendumQuoteId);
        assertEquals(Boolean.TRUE, originatingLog.get("notificationFlag"));

        var listForOwnerJob = rest.exchange("/api/job-requests/" + s.jobId + "/quotes", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.ownerToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertEquals(HttpStatus.OK, listForOwnerJob.getStatusCode());
        assertTrue(listForOwnerJob.getBody().stream()
                .anyMatch(q -> addendumQuoteId.equals(q.get("id").toString())
                        && "PENDING".equals(q.get("status"))));

        var acceptBody = Map.of(
                "addendumFlow", true,
                "scheduledDate", "2026-05-15",
                "scheduledTime", "09:30");
        var acceptResp = rest.exchange("/api/quotes/" + addendumQuoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(acceptBody, bearerHeaders(s.ownerToken)), MAP_REF);
        assertEquals(HttpStatus.CREATED, acceptResp.getStatusCode());

        var unreadForOwner = rest.exchange("/api/quote-logs?notificationFlag=true", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.ownerToken)), MAP_REF);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ownerUnread = (List<Map<String, Object>>) unreadForOwner.getBody().get("logs");
        assertTrue(ownerUnread.stream()
                .noneMatch(l -> originatingLog.get("id").equals(l.get("id"))),
                "originating CAR_OWNER log should have been marked read");

        var garageInbox = rest.exchange("/api/quote-logs?notificationFlag=true", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.garageToken)), MAP_REF);
        assertEquals(HttpStatus.OK, garageInbox.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> garageLogs = (List<Map<String, Object>>) garageInbox.getBody().get("logs");
        assertTrue(garageLogs.stream()
                .anyMatch(l -> "GARAGE_OWNER".equals(l.get("typeOfNotification"))
                        && addendumQuoteId.equals(String.valueOf(l.get("quoteId")))
                        && Boolean.TRUE.equals(l.get("notificationFlag"))),
                "garage should receive a notification that the addendum quote was accepted");
    }

    @Test
    void addendumQuoteCreatedThroughLog_rejectFlow() {
        FullSetup s = fullSetupAcceptedOriginalQuote();

        var newQuotePayload = Map.of("price", 80.00, "description", "Tire balancing");
        var body = Map.of(
                "message", "Tire imbalance. Approve?",
                "notificationFlag", true,
                "updateFlag", true,
                "newQuote", newQuotePayload
        );
        var createResp = rest.exchange("/api/quotes/" + s.originalQuoteId + "/logs", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(s.garageToken)), MAP_REF);
        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());
        Map<String, Object> originatingLog = createResp.getBody();
        String addendumQuoteId = String.valueOf(originatingLog.get("triggeredQuoteId"));

        var rejectResp = rest.exchange("/api/quotes/" + addendumQuoteId + "/reject", HttpMethod.POST,
                new HttpEntity<>(null, bearerHeaders(s.ownerToken)), MAP_REF);
        assertEquals(HttpStatus.OK, rejectResp.getStatusCode());
        assertEquals("REJECTED", rejectResp.getBody().get("status"));

        var unreadForOwner = rest.exchange("/api/quote-logs?notificationFlag=true", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.ownerToken)), MAP_REF);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ownerUnread = (List<Map<String, Object>>) unreadForOwner.getBody().get("logs");
        assertTrue(ownerUnread.stream().noneMatch(l -> originatingLog.get("id").equals(l.get("id"))));

        var garageInbox = rest.exchange("/api/quote-logs?notificationFlag=true", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.garageToken)), MAP_REF);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> garageLogs = (List<Map<String, Object>>) garageInbox.getBody().get("logs");
        assertTrue(garageLogs.stream()
                .anyMatch(l -> "GARAGE_OWNER".equals(l.get("typeOfNotification"))
                        && addendumQuoteId.equals(String.valueOf(l.get("quoteId")))));
    }

    @Test
    void acceptAddendumWithAddendumFlowFalseRejected() {
        FullSetup s = fullSetupAcceptedOriginalQuote();

        var newQuotePayload = Map.of("price", 50.00, "description", "Extra");
        var logBody = new HashMap<String, Object>();
        logBody.put("message", "Extra work");
        logBody.put("notificationFlag", true);
        logBody.put("updateFlag", true);
        logBody.put("newQuote", newQuotePayload);
        var createResp = rest.exchange("/api/quotes/" + s.originalQuoteId + "/logs", HttpMethod.POST,
                new HttpEntity<>(logBody, bearerHeaders(s.garageToken)), MAP_REF);
        assertEquals(HttpStatus.CREATED, createResp.getStatusCode());
        String addendumQuoteId = String.valueOf(createResp.getBody().get("triggeredQuoteId"));

        var wrongModeBody = Map.of(
                "addendumFlow", false,
                "scheduledDate", "2026-06-01",
                "scheduledTime", "11:00");
        var resp = rest.exchange("/api/quotes/" + addendumQuoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(wrongModeBody, bearerHeaders(s.ownerToken)), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void rejectingNonAddendumQuote_doesNotCreateLog() {
        String ownerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        Map<String, Object> car = addCar(ownerToken);
        Map<String, Object> job = createJobRequest(ownerToken, car.get("id"));

        String garageToken = registerAndGetToken(uniqueEmail(), "GARAGE");
        createGarage(garageToken, "Plain Garage " + UUID.randomUUID());
        Map<String, Object> quote = submitQuote(garageToken, job.get("id").toString(), 250.00);

        var rejectResp = rest.exchange("/api/quotes/" + quote.get("id") + "/reject", HttpMethod.POST,
                new HttpEntity<>(null, bearerHeaders(ownerToken)), MAP_REF);
        assertEquals(HttpStatus.OK, rejectResp.getStatusCode());
        assertEquals("REJECTED", rejectResp.getBody().get("status"));

        var garageInbox = rest.exchange("/api/quote-logs?notificationFlag=true", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(garageToken)), MAP_REF);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> garageLogs = (List<Map<String, Object>>) garageInbox.getBody().get("logs");
        assertTrue(garageLogs.isEmpty(),
                "no auto-log expected when rejecting a non-addendum (root) quote");
    }

    @Test
    void listLogsWithoutNotificationFlag_returnsAllDesc() {
        FullSetup s = fullSetupAcceptedOriginalQuote();

        Map<String, Object> first = createPlainLog(s, "First");
        Map<String, Object> second = createPlainLog(s, "Second");
        Map<String, Object> third = createPlainLog(s, "Third");

        var allResp = rest.exchange("/api/quote-logs", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.ownerToken)), MAP_REF);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allLogs = (List<Map<String, Object>>) allResp.getBody().get("logs");
        assertTrue(allLogs.size() >= 3);
        int idxThird = indexOfLog(allLogs, third.get("id"));
        int idxSecond = indexOfLog(allLogs, second.get("id"));
        int idxFirst = indexOfLog(allLogs, first.get("id"));
        assertTrue(idxThird < idxSecond && idxSecond < idxFirst,
                "newest-first when notificationFlag is not provided");
    }

    @Test
    void listLogsWithNotificationFlagTrue_returnsUnreadAsc() {
        FullSetup s = fullSetupAcceptedOriginalQuote();

        Map<String, Object> first = createPlainLog(s, "First");
        Map<String, Object> second = createPlainLog(s, "Second");

        rest.exchange("/api/quote-logs/" + first.get("id") + "/mark-read", HttpMethod.POST,
                new HttpEntity<>(null, bearerHeaders(s.ownerToken)), MAP_REF);

        var unreadResp = rest.exchange("/api/quote-logs?notificationFlag=true", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.ownerToken)), MAP_REF);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> unread = (List<Map<String, Object>>) unreadResp.getBody().get("logs");
        assertTrue(unread.stream().noneMatch(l -> first.get("id").equals(l.get("id"))));
        assertTrue(unread.stream().anyMatch(l -> second.get("id").equals(l.get("id"))));
        unread.forEach(l -> assertEquals(Boolean.TRUE, l.get("notificationFlag")));
    }

    @Test
    void markReadOnlyByIntendedRecipient() {
        FullSetup s = fullSetupAcceptedOriginalQuote();

        Map<String, Object> log = createPlainLog(s, "Mark-read target");

        var garageAttempt = rest.exchange("/api/quote-logs/" + log.get("id") + "/mark-read", HttpMethod.POST,
                new HttpEntity<>(null, bearerHeaders(s.garageToken)), String.class);
        assertEquals(HttpStatus.FORBIDDEN, garageAttempt.getStatusCode());

        var ownerMark = rest.exchange("/api/quote-logs/" + log.get("id") + "/mark-read", HttpMethod.POST,
                new HttpEntity<>(null, bearerHeaders(s.ownerToken)), MAP_REF);
        assertEquals(HttpStatus.OK, ownerMark.getStatusCode());
        assertEquals(Boolean.FALSE, ownerMark.getBody().get("notificationFlag"));
    }

    @Test
    void garageInboxIsAutoScopedAndCanMarkRead() {
        FullSetup s = fullSetupAcceptedOriginalQuote();

        var newQuotePayload = Map.of("price", 90.00, "description", "Wiper replacement");
        var body = Map.of(
                "message", "Wipers worn out",
                "notificationFlag", true,
                "updateFlag", true,
                "newQuote", newQuotePayload
        );
        var createResp = rest.exchange("/api/quotes/" + s.originalQuoteId + "/logs", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(s.garageToken)), MAP_REF);
        String addendumQuoteId = String.valueOf(createResp.getBody().get("triggeredQuoteId"));

        var acceptBody = Map.of(
                "addendumFlow", true,
                "scheduledDate", "2026-05-16",
                "scheduledTime", "09:30");
        rest.exchange("/api/quotes/" + addendumQuoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(acceptBody, bearerHeaders(s.ownerToken)), MAP_REF);

        var garageInbox = rest.exchange("/api/quote-logs?notificationFlag=true", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.garageToken)), MAP_REF);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> garageLogs = (List<Map<String, Object>>) garageInbox.getBody().get("logs");
        assertFalse(garageLogs.isEmpty());
        Map<String, Object> acceptanceLog = garageLogs.stream()
                .filter(l -> "GARAGE_OWNER".equals(l.get("typeOfNotification")))
                .findFirst().orElseThrow();
        assertEquals(s.garageId, acceptanceLog.get("reference"));

        var markResp = rest.exchange("/api/quote-logs/" + acceptanceLog.get("id") + "/mark-read", HttpMethod.POST,
                new HttpEntity<>(null, bearerHeaders(s.garageToken)), MAP_REF);
        assertEquals(HttpStatus.OK, markResp.getStatusCode());
        assertEquals(Boolean.FALSE, markResp.getBody().get("notificationFlag"));

        var garageInboxAfter = rest.exchange("/api/quote-logs?notificationFlag=true", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.garageToken)), MAP_REF);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> garageLogsAfter = (List<Map<String, Object>>) garageInboxAfter.getBody().get("logs");
        assertTrue(garageLogsAfter.stream().noneMatch(l -> acceptanceLog.get("id").equals(l.get("id"))));
    }

    @Test
    void carOwnerLogsAreScopedPerOwner() {
        FullSetup s = fullSetupAcceptedOriginalQuote();
        createPlainLog(s, "Visible to owner A");

        String otherOwnerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        var resp = rest.exchange("/api/quote-logs", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(otherOwnerToken)), MAP_REF);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> logs = (List<Map<String, Object>>) resp.getBody().get("logs");
        assertTrue(logs.isEmpty(), "other car owner should not see logs targeted at the first owner");
    }

    @Test
    void updateFlagWithoutNewQuoteIsRejected() {
        FullSetup s = fullSetupAcceptedOriginalQuote();

        var body = Map.of("message", "Missing payload", "notificationFlag", true, "updateFlag", true);
        var resp = rest.exchange("/api/quotes/" + s.originalQuoteId + "/logs", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(s.garageToken)), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // ---------- helpers ----------

    private Map<String, Object> createPlainLog(FullSetup s, String message) {
        var body = new HashMap<String, Object>();
        body.put("message", message);
        body.put("notificationFlag", true);
        body.put("updateFlag", false);
        var resp = rest.exchange("/api/quotes/" + s.originalQuoteId + "/logs", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(s.garageToken)), MAP_REF);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return resp.getBody();
    }

    private static int indexOfLog(List<Map<String, Object>> logs, Object id) {
        for (int i = 0; i < logs.size(); i++) {
            if (id.equals(logs.get(i).get("id"))) {
                return i;
            }
        }
        return -1;
    }
}
