package com.api.auto_ease.controller.booking;

import com.api.auto_ease.support.GarageTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class BookingAndReviewIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private String toyotaMakeId;
    private String corollaModelId;
    private String oilChangeCategoryId;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_REF =
            new ParameterizedTypeReference<>() {};

    private String uniqueEmail() {
        return "bk-test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private String registerAndGetToken(String email, String userType) {
        return registerAndGetToken(email, userType, null);
    }

    private String registerAndGetToken(String email, String userType, String phone) {
        var req = new HashMap<String, Object>();
        req.put("email", email);
        req.put("password", "pass123");
        req.put("fullName", "Test User");
        req.put("userType", userType);
        if (phone != null) {
            req.put("phone", phone);
        }
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
                new HttpEntity<>(body, bearerHeaders(token)), Map.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return resp.getBody();
    }

    private Map<String, Object> createJobRequest(String token, Object carId) {
        var body = new HashMap<String, Object>();
        body.put("carId", carId);
        body.put("categoryId", oilChangeCategoryId);
        body.put("title", "Oil change needed");
        body.put("urgency", "NORMAL");
        var resp = rest.exchange("/api/job-requests", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(token)), Map.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return resp.getBody();
    }

    private Map<String, Object> createGarageAndGetProfile(String token) {
        var body = Map.of("businessName", "AutoService Pro", "city", "București", "phone", "+40741000000");
        var resp = rest.exchange("/api/garages", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(token)), Map.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        Map<String, Object> profile = resp.getBody();
        GarageTestSupport.approveGarage(rest, profile.get("id").toString());
        return profile;
    }

    private Map<String, Object> submitQuote(String garageToken, String jobId, double price) {
        var body = Map.of("price", price, "estimatedDuration", "2h", "description", "Full service");
        var resp = rest.exchange("/api/job-requests/" + jobId + "/quotes", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(garageToken)), Map.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return resp.getBody();
    }

    record TestSetup(String ownerToken, String garageToken, String jobId, String quoteId, String garageId) {}

    private TestSetup fullSetup(String ownerPhone) {
        String ownerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER", ownerPhone);
        Map<String, Object> car = addCar(ownerToken);
        Map<String, Object> job = createJobRequest(ownerToken, car.get("id"));

        String garageToken = registerAndGetToken(uniqueEmail(), "GARAGE");
        Map<String, Object> garage = createGarageAndGetProfile(garageToken);
        Map<String, Object> quote = submitQuote(garageToken, job.get("id").toString(), 250.00);

        return new TestSetup(ownerToken, garageToken,
                job.get("id").toString(), quote.get("id").toString(), garage.get("id").toString());
    }

    private TestSetup fullSetup() {
        return fullSetup(null);
    }

    private void acceptQuote(String ownerToken, String quoteId, boolean addendumFlow) {
        var body = Map.of("addendumFlow", addendumFlow, "scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        var resp = rest.exchange("/api/quotes/" + quoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(ownerToken)), Map.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
    }

    private Map<String, Object> completeJob(String garageToken, String jobId) {
        var resp = rest.exchange("/api/job-requests/" + jobId + "/complete", HttpMethod.POST,
                new HttpEntity<>(null, bearerHeaders(garageToken)), MAP_REF);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        return resp.getBody();
    }

    private TestSetup setupAcceptedJob() {
        TestSetup s = fullSetup();
        acceptQuote(s.ownerToken(), s.quoteId(), false);
        return s;
    }

    // Test 1: Accept quote — happy path
    @Test
    void acceptQuoteHappyPath() {
        TestSetup s = fullSetup();

        var body = Map.of("addendumFlow", false, "scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        var resp = rest.exchange("/api/quotes/" + s.quoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(s.ownerToken)), Map.class);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        Map<String, Object> booking = resp.getBody();
        assertNotNull(booking.get("id"));
        assertEquals("CONFIRMED", booking.get("status"));
        assertNotNull(booking.get("garageName"));
    }

    @Test
    void acceptQuoteNotYourJobRequest() {
        TestSetup s = fullSetup();
        String otherOwnerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER");

        var body = Map.of("addendumFlow", false, "scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        var resp = rest.exchange("/api/quotes/" + s.quoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(otherOwnerToken)), String.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void acceptQuoteGarageRejected() {
        TestSetup s = fullSetup();

        var body = Map.of("addendumFlow", false, "scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        var resp = rest.exchange("/api/quotes/" + s.quoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(s.garageToken)), String.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void acceptOpenQuoteWithAddendumFlowTrueRejected() {
        TestSetup s = fullSetup();

        var body = Map.of("addendumFlow", true, "scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        var resp = rest.exchange("/api/quotes/" + s.quoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(s.ownerToken)), String.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void acceptQuoteAlreadyAccepted() {
        String ownerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        Map<String, Object> car = addCar(ownerToken);
        Map<String, Object> job = createJobRequest(ownerToken, car.get("id"));
        String jobId = job.get("id").toString();

        String garageToken1 = registerAndGetToken(uniqueEmail(), "GARAGE");
        createGarageAndGetProfile(garageToken1);
        Map<String, Object> quote1 = submitQuote(garageToken1, jobId, 200.00);

        String garageToken2 = registerAndGetToken(uniqueEmail(), "GARAGE");
        createGarageAndGetProfile(garageToken2);
        Map<String, Object> quote2 = submitQuote(garageToken2, jobId, 300.00);

        var body = Map.of("addendumFlow", false, "scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        var resp1 = rest.exchange("/api/quotes/" + quote1.get("id") + "/accept", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(ownerToken)), Map.class);
        assertEquals(HttpStatus.CREATED, resp1.getStatusCode());

        var resp2 = rest.exchange("/api/quotes/" + quote2.get("id") + "/accept", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(ownerToken)), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp2.getStatusCode());
    }

    @Test
    void listOwnBookingsCarOwner() {
        TestSetup s = fullSetup();
        acceptQuote(s.ownerToken(), s.quoteId(), false);

        var resp = rest.exchange("/api/bookings", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.ownerToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<Map<String, Object>> bookings = resp.getBody();
        assertFalse(bookings.isEmpty());
        assertNotNull(bookings.get(0).get("garageName"));
        assertNotNull(bookings.get(0).get("jobTitle"));
    }

    @Test
    void listOwnBookingsGarage() {
        TestSetup s = fullSetup();
        acceptQuote(s.ownerToken(), s.quoteId(), false);

        var resp = rest.exchange("/api/bookings", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.garageToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<Map<String, Object>> bookings = resp.getBody();
        assertFalse(bookings.isEmpty());
    }

    @Test
    void bookingIncludesContactPhonesWhenAvailable() {
        TestSetup s = fullSetup("+40712345678");
        acceptQuote(s.ownerToken(), s.quoteId(), false);

        var ownerResp = rest.exchange("/api/bookings", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.ownerToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertEquals(HttpStatus.OK, ownerResp.getStatusCode());
        Map<String, Object> ownerBooking = ownerResp.getBody().get(0);
        assertEquals("+40741000000", ownerBooking.get("garagePhone"));
        assertEquals("+40712345678", ownerBooking.get("clientPhone"));

        var garageResp = rest.exchange("/api/bookings", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.garageToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertEquals(HttpStatus.OK, garageResp.getStatusCode());
        Map<String, Object> garageBooking = garageResp.getBody().get(0);
        assertEquals("+40741000000", garageBooking.get("garagePhone"));
        assertEquals("+40712345678", garageBooking.get("clientPhone"));
    }

    @Test
    void bookingOmitsClientPhoneWhenNotSet() {
        TestSetup s = fullSetup();
        acceptQuote(s.ownerToken(), s.quoteId(), false);

        var resp = rest.exchange("/api/bookings", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.garageToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map<String, Object> booking = resp.getBody().get(0);
        assertEquals("+40741000000", booking.get("garagePhone"));
        assertNull(booking.get("clientPhone"));
    }

    @Test
    void completeJobHappyPath() {
        TestSetup s = setupAcceptedJob();

        Map<String, Object> completed = completeJob(s.garageToken(), s.jobId());
        assertEquals("COMPLETED", completed.get("status"));
        assertEquals(Boolean.TRUE, completed.get("reviewEligible"));
        assertNull(completed.get("reviewId"));

        var bookingsResp = rest.exchange("/api/bookings", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.ownerToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        bookingsResp.getBody().forEach(b -> assertEquals("COMPLETED", b.get("status")));
    }

    @Test
    void completeJobBlockedByPendingAddendum() {
        TestSetup s = setupAcceptedJob();

        var newQuotePayload = Map.of("price", 120.00, "description", "Extra work");
        var logBody = Map.of(
                "message", "Found extra work",
                "notificationFlag", true,
                "updateFlag", true,
                "newQuote", newQuotePayload
        );
        rest.exchange("/api/quotes/" + s.quoteId + "/logs", HttpMethod.POST,
                new HttpEntity<>(logBody, bearerHeaders(s.garageToken)), MAP_REF);

        var resp = rest.exchange("/api/job-requests/" + s.jobId + "/complete", HttpMethod.POST,
                new HttpEntity<>(null, bearerHeaders(s.garageToken)), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void completeJobAfterAddendumRejected() {
        TestSetup s = setupAcceptedJob();

        var newQuotePayload = Map.of("price", 80.00, "description", "Tire balancing");
        var logBody = Map.of(
                "message", "Tire imbalance",
                "notificationFlag", true,
                "updateFlag", true,
                "newQuote", newQuotePayload
        );
        var logResp = rest.exchange("/api/quotes/" + s.quoteId + "/logs", HttpMethod.POST,
                new HttpEntity<>(logBody, bearerHeaders(s.garageToken)), MAP_REF);
        String addendumQuoteId = String.valueOf(logResp.getBody().get("triggeredQuoteId"));

        rest.exchange("/api/quotes/" + addendumQuoteId + "/reject", HttpMethod.POST,
                new HttpEntity<>(null, bearerHeaders(s.ownerToken)), MAP_REF);

        Map<String, Object> completed = completeJob(s.garageToken(), s.jobId());
        assertEquals("COMPLETED", completed.get("status"));
    }

    @Test
    void completeJobAfterAddendumAccepted() {
        TestSetup s = setupAcceptedJob();

        var newQuotePayload = Map.of("price", 120.00, "description", "Oil spill repair");
        var logBody = Map.of(
                "message", "Oil spill found",
                "notificationFlag", true,
                "updateFlag", true,
                "newQuote", newQuotePayload
        );
        var logResp = rest.exchange("/api/quotes/" + s.quoteId + "/logs", HttpMethod.POST,
                new HttpEntity<>(logBody, bearerHeaders(s.garageToken)), MAP_REF);
        String addendumQuoteId = String.valueOf(logResp.getBody().get("triggeredQuoteId"));

        acceptQuote(s.ownerToken(), addendumQuoteId, true);

        Map<String, Object> completed = completeJob(s.garageToken(), s.jobId());
        assertEquals("COMPLETED", completed.get("status"));

        var bookingsResp = rest.exchange("/api/bookings", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.ownerToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertEquals(2, bookingsResp.getBody().size());
        bookingsResp.getBody().forEach(b -> assertEquals("COMPLETED", b.get("status")));
    }

    @Test
    void createReviewHappyPath() {
        TestSetup s = setupAcceptedJob();
        completeJob(s.garageToken(), s.jobId());

        var reviewBody = Map.of("rating", 5, "comment", "Serviciu excelent!");
        var resp = rest.exchange("/api/job-requests/" + s.jobId + "/reviews", HttpMethod.POST,
                new HttpEntity<>(reviewBody, bearerHeaders(s.ownerToken)), MAP_REF);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        Map<String, Object> review = resp.getBody();
        assertEquals(5, review.get("rating"));
        assertEquals("Serviciu excelent!", review.get("comment"));
        assertEquals(s.jobId, review.get("jobRequestId").toString());
        assertNotNull(review.get("garageId"));
    }

    @Test
    void createReviewNotYourJob() {
        TestSetup s = setupAcceptedJob();
        completeJob(s.garageToken(), s.jobId());

        String otherOwner = registerAndGetToken(uniqueEmail(), "CAR_OWNER");

        var reviewBody = Map.of("rating", 5, "comment", "Not my job");
        var resp = rest.exchange("/api/job-requests/" + s.jobId + "/reviews", HttpMethod.POST,
                new HttpEntity<>(reviewBody, bearerHeaders(otherOwner)), String.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void createReviewDuplicateRejected() {
        TestSetup s = setupAcceptedJob();
        completeJob(s.garageToken(), s.jobId());

        var reviewBody = Map.of("rating", 5, "comment", "Great!");
        rest.exchange("/api/job-requests/" + s.jobId + "/reviews", HttpMethod.POST,
                new HttpEntity<>(reviewBody, bearerHeaders(s.ownerToken)), MAP_REF);

        var reviewBody2 = Map.of("rating", 4, "comment", "Second review");
        var resp = rest.exchange("/api/job-requests/" + s.jobId + "/reviews", HttpMethod.POST,
                new HttpEntity<>(reviewBody2, bearerHeaders(s.ownerToken)), String.class);

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    @Test
    void createReviewBlockedWhileJobNotCompleted() {
        TestSetup s = setupAcceptedJob();

        var reviewBody = Map.of("rating", 5, "comment", "Too early");
        var resp = rest.exchange("/api/job-requests/" + s.jobId + "/reviews", HttpMethod.POST,
                new HttpEntity<>(reviewBody, bearerHeaders(s.ownerToken)), String.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void listReviewsForGaragePublic() {
        TestSetup s = setupAcceptedJob();
        completeJob(s.garageToken(), s.jobId());

        var reviewBody = Map.of("rating", 4, "comment", "Bun!");
        rest.exchange("/api/job-requests/" + s.jobId + "/reviews", HttpMethod.POST,
                new HttpEntity<>(reviewBody, bearerHeaders(s.ownerToken)), MAP_REF);

        var resp = rest.exchange("/api/garages/" + s.garageId + "/reviews", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<Map<String, Object>> reviews = resp.getBody();
        assertFalse(reviews.isEmpty());
        assertEquals(4, reviews.get(0).get("rating"));
    }

    @Test
    void reviewReplyThread() {
        TestSetup s = setupAcceptedJob();
        completeJob(s.garageToken(), s.jobId());

        var reviewBody = Map.of("rating", 5, "comment", "Great service!");
        var reviewResp = rest.exchange("/api/job-requests/" + s.jobId + "/reviews", HttpMethod.POST,
                new HttpEntity<>(reviewBody, bearerHeaders(s.ownerToken)), MAP_REF);
        String reviewId = reviewResp.getBody().get("id").toString();

        var garageReplyBody = Map.of("message", "Thank you for your feedback!");
        var garageReplyResp = rest.exchange("/api/reviews/" + reviewId + "/replies", HttpMethod.POST,
                new HttpEntity<>(garageReplyBody, bearerHeaders(s.garageToken)), MAP_REF);
        assertEquals(HttpStatus.CREATED, garageReplyResp.getStatusCode());
        assertEquals("GARAGE", garageReplyResp.getBody().get("authorRole"));
        String garageReplyId = garageReplyResp.getBody().get("id").toString();

        var ownerReplyBody = Map.of("message", "You're welcome!", "parentReplyId", garageReplyId);
        var ownerReplyResp = rest.exchange("/api/reviews/" + reviewId + "/replies", HttpMethod.POST,
                new HttpEntity<>(ownerReplyBody, bearerHeaders(s.ownerToken)), MAP_REF);
        assertEquals(HttpStatus.CREATED, ownerReplyResp.getStatusCode());
        assertEquals("CAR_OWNER", ownerReplyResp.getBody().get("authorRole"));

        var listResp = rest.exchange("/api/garages/" + s.garageId + "/reviews", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> replies = (List<Map<String, Object>>) listResp.getBody().get(0).get("replies");
        assertEquals(2, replies.size());
    }

    @Test
    void carOwnerCannotPostFirstReply() {
        TestSetup s = setupAcceptedJob();
        completeJob(s.garageToken(), s.jobId());

        var reviewBody = Map.of("rating", 5, "comment", "Great!");
        var reviewResp = rest.exchange("/api/job-requests/" + s.jobId + "/reviews", HttpMethod.POST,
                new HttpEntity<>(reviewBody, bearerHeaders(s.ownerToken)), MAP_REF);
        String reviewId = reviewResp.getBody().get("id").toString();

        var replyBody = Map.of("message", "I should not be first");
        var resp = rest.exchange("/api/reviews/" + reviewId + "/replies", HttpMethod.POST,
                new HttpEntity<>(replyBody, bearerHeaders(s.ownerToken)), String.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void garageCannotReplyTwiceInARow() {
        TestSetup s = setupAcceptedJob();
        completeJob(s.garageToken(), s.jobId());

        var reviewBody = Map.of("rating", 5, "comment", "Great!");
        var reviewResp = rest.exchange("/api/job-requests/" + s.jobId + "/reviews", HttpMethod.POST,
                new HttpEntity<>(reviewBody, bearerHeaders(s.ownerToken)), MAP_REF);
        String reviewId = reviewResp.getBody().get("id").toString();

        rest.exchange("/api/reviews/" + reviewId + "/replies", HttpMethod.POST,
                new HttpEntity<>(Map.of("message", "Thanks!"), bearerHeaders(s.garageToken)), MAP_REF);

        var resp = rest.exchange("/api/reviews/" + reviewId + "/replies", HttpMethod.POST,
                new HttpEntity<>(Map.of("message", "Again?"), bearerHeaders(s.garageToken)), String.class);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    @Test
    void fullMarketplaceLoop() {
        String ownerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        Map<String, Object> car = addCar(ownerToken);

        Map<String, Object> job = createJobRequest(ownerToken, car.get("id"));
        assertEquals("OPEN", job.get("status"));
        String jobId = job.get("id").toString();

        String garageToken = registerAndGetToken(uniqueEmail(), "GARAGE");
        createGarageAndGetProfile(garageToken);

        Map<String, Object> quote = submitQuote(garageToken, jobId, 200.00);
        assertEquals("PENDING", quote.get("status"));
        String quoteId = quote.get("id").toString();

        acceptQuote(ownerToken, quoteId, false);

        var quotesResp = rest.exchange("/api/job-requests/" + jobId + "/quotes", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(ownerToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertEquals(HttpStatus.OK, quotesResp.getStatusCode());
        assertEquals("ACCEPTED", quotesResp.getBody().get(0).get("status"));

        var jobsResp = rest.exchange("/api/job-requests", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(ownerToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        assertEquals(HttpStatus.OK, jobsResp.getStatusCode());
        var thisJob = jobsResp.getBody().stream()
                .filter(j -> jobId.equals(j.get("id").toString()))
                .findFirst().orElseThrow();
        assertEquals("BOOKED", thisJob.get("status"));
    }
}
