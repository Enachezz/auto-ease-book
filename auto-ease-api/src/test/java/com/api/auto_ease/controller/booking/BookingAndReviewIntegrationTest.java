package com.api.auto_ease.controller.booking;

import com.api.auto_ease.domain.booking.Booking;
import com.api.auto_ease.domain.booking.BookingStatus;
import com.api.auto_ease.repository.booking.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class BookingAndReviewIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private BookingRepository bookingRepository;

    private String toyotaMakeId;
    private String corollaModelId;
    private String oilChangeCategoryId;

    private String uniqueEmail() {
        return "bk-test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
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
                .filter(m -> "Toyota".equals(m.get("name")))
                .map(m -> m.get("id").toString())
                .findFirst().orElseThrow();

        var modelsResp = rest.exchange("/api/car-makes/" + toyotaMakeId + "/models", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        corollaModelId = modelsResp.getBody().stream()
                .filter(m -> "Corolla".equals(m.get("name")))
                .map(m -> m.get("id").toString())
                .findFirst().orElseThrow();

        var categoriesResp = rest.exchange("/api/service-categories", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        oilChangeCategoryId = categoriesResp.getBody().stream()
                .filter(c -> "Oil Change".equals(c.get("name")))
                .map(c -> c.get("id").toString())
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
        return resp.getBody();
    }

    private Map<String, Object> submitQuote(String garageToken, String jobId, double price) {
        var body = Map.of("price", price, "estimatedDuration", "2h", "description", "Full service");
        var resp = rest.exchange("/api/job-requests/" + jobId + "/quotes", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(garageToken)), Map.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return resp.getBody();
    }

    // -- Helpers that set up the full chain --

    record TestSetup(String ownerToken, String garageToken, String jobId, String quoteId, String garageId) {}

    private TestSetup fullSetup() {
        String ownerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER");
        Map<String, Object> car = addCar(ownerToken);
        Map<String, Object> job = createJobRequest(ownerToken, car.get("id"));

        String garageToken = registerAndGetToken(uniqueEmail(), "GARAGE");
        Map<String, Object> garage = createGarageAndGetProfile(garageToken);
        Map<String, Object> quote = submitQuote(garageToken, job.get("id").toString(), 250.00);

        return new TestSetup(ownerToken, garageToken,
                job.get("id").toString(), quote.get("id").toString(), garage.get("id").toString());
    }

    // Test 1: Accept quote — happy path
    @Test
    void acceptQuoteHappyPath() {
        TestSetup s = fullSetup();

        var body = Map.of("scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        var resp = rest.exchange("/api/quotes/" + s.quoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(s.ownerToken)), Map.class);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        Map<String, Object> booking = resp.getBody();
        assertNotNull(booking.get("id"));
        assertEquals("CONFIRMED", booking.get("status"));
        assertNotNull(booking.get("garageName"));
    }

    // Test 2: Accept quote — not your job request
    @Test
    void acceptQuoteNotYourJobRequest() {
        TestSetup s = fullSetup();
        String otherOwnerToken = registerAndGetToken(uniqueEmail(), "CAR_OWNER");

        var body = Map.of("scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        var resp = rest.exchange("/api/quotes/" + s.quoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(otherOwnerToken)), String.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // Test 3: Accept quote — garage user rejected
    @Test
    void acceptQuoteGarageRejected() {
        TestSetup s = fullSetup();

        var body = Map.of("scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        var resp = rest.exchange("/api/quotes/" + s.quoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(s.garageToken)), String.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // Test 4: Accept quote — already accepted (second quote for same job)
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

        var body = Map.of("scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        var resp1 = rest.exchange("/api/quotes/" + quote1.get("id") + "/accept", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(ownerToken)), Map.class);
        assertEquals(HttpStatus.CREATED, resp1.getStatusCode());

        var resp2 = rest.exchange("/api/quotes/" + quote2.get("id") + "/accept", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(ownerToken)), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp2.getStatusCode());
    }

    // Test 5: List own bookings — car owner
    @Test
    void listOwnBookingsCarOwner() {
        TestSetup s = fullSetup();

        var body = Map.of("scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        rest.exchange("/api/quotes/" + s.quoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(s.ownerToken)), Map.class);

        var resp = rest.exchange("/api/bookings", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.ownerToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<Map<String, Object>> bookings = resp.getBody();
        assertFalse(bookings.isEmpty());
        assertNotNull(bookings.get(0).get("garageName"));
        assertNotNull(bookings.get(0).get("jobTitle"));
    }

    // Test 6: List own bookings — garage
    @Test
    void listOwnBookingsGarage() {
        TestSetup s = fullSetup();

        var body = Map.of("scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        rest.exchange("/api/quotes/" + s.quoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(s.ownerToken)), Map.class);

        var resp = rest.exchange("/api/bookings", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(s.garageToken)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<Map<String, Object>> bookings = resp.getBody();
        assertFalse(bookings.isEmpty());
    }

    // Test 7: Create review — happy path (set booking to COMPLETED via repo)
    @Test
    void createReviewHappyPath() {
        TestSetup s = fullSetup();

        var acceptBody = Map.of("scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        var acceptResp = rest.exchange("/api/quotes/" + s.quoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(acceptBody, bearerHeaders(s.ownerToken)), Map.class);
        String bookingId = acceptResp.getBody().get("id").toString();

        Booking booking = bookingRepository.findById(UUID.fromString(bookingId)).orElseThrow();
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        var reviewBody = Map.of("rating", 5, "comment", "Serviciu excelent!");
        var resp = rest.exchange("/api/bookings/" + bookingId + "/reviews", HttpMethod.POST,
                new HttpEntity<>(reviewBody, bearerHeaders(s.ownerToken)), Map.class);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        Map<String, Object> review = resp.getBody();
        assertEquals(5, review.get("rating"));
        assertEquals("Serviciu excelent!", review.get("comment"));
        assertNotNull(review.get("garageId"));
    }

    // Test 8: Create review — not your booking
    @Test
    void createReviewNotYourBooking() {
        TestSetup s = fullSetup();

        var acceptBody = Map.of("scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        var acceptResp = rest.exchange("/api/quotes/" + s.quoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(acceptBody, bearerHeaders(s.ownerToken)), Map.class);
        String bookingId = acceptResp.getBody().get("id").toString();

        Booking booking = bookingRepository.findById(UUID.fromString(bookingId)).orElseThrow();
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        String otherOwner = registerAndGetToken(uniqueEmail(), "CAR_OWNER");

        var reviewBody = Map.of("rating", 5, "comment", "Not my booking");
        var resp = rest.exchange("/api/bookings/" + bookingId + "/reviews", HttpMethod.POST,
                new HttpEntity<>(reviewBody, bearerHeaders(otherOwner)), String.class);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // Test 9: Create review — duplicate rejected
    @Test
    void createReviewDuplicateRejected() {
        TestSetup s = fullSetup();

        var acceptBody = Map.of("scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        var acceptResp = rest.exchange("/api/quotes/" + s.quoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(acceptBody, bearerHeaders(s.ownerToken)), Map.class);
        String bookingId = acceptResp.getBody().get("id").toString();

        Booking booking = bookingRepository.findById(UUID.fromString(bookingId)).orElseThrow();
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        var reviewBody = Map.of("rating", 5, "comment", "Great!");
        rest.exchange("/api/bookings/" + bookingId + "/reviews", HttpMethod.POST,
                new HttpEntity<>(reviewBody, bearerHeaders(s.ownerToken)), Map.class);

        var reviewBody2 = Map.of("rating", 4, "comment", "Second review");
        var resp = rest.exchange("/api/bookings/" + bookingId + "/reviews", HttpMethod.POST,
                new HttpEntity<>(reviewBody2, bearerHeaders(s.ownerToken)), String.class);

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    // Test 10: List reviews for garage (public)
    @Test
    void listReviewsForGaragePublic() {
        TestSetup s = fullSetup();

        var acceptBody = Map.of("scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        var acceptResp = rest.exchange("/api/quotes/" + s.quoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(acceptBody, bearerHeaders(s.ownerToken)), Map.class);
        String bookingId = acceptResp.getBody().get("id").toString();

        Booking booking = bookingRepository.findById(UUID.fromString(bookingId)).orElseThrow();
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        var reviewBody = Map.of("rating", 4, "comment", "Bun!");
        rest.exchange("/api/bookings/" + bookingId + "/reviews", HttpMethod.POST,
                new HttpEntity<>(reviewBody, bearerHeaders(s.ownerToken)), Map.class);

        var resp = rest.exchange("/api/garages/" + s.garageId + "/reviews", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<Map<String, Object>> reviews = resp.getBody();
        assertFalse(reviews.isEmpty());
        assertEquals(4, reviews.get(0).get("rating"));
    }

    // Test 11: Full marketplace loop
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

        var acceptBody = Map.of("scheduledDate", "2025-03-20", "scheduledTime", "10:00");
        var bookingResp = rest.exchange("/api/quotes/" + quoteId + "/accept", HttpMethod.POST,
                new HttpEntity<>(acceptBody, bearerHeaders(ownerToken)), Map.class);
        assertEquals(HttpStatus.CREATED, bookingResp.getStatusCode());
        assertEquals("CONFIRMED", bookingResp.getBody().get("status"));

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
