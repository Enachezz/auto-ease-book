package com.api.auto_ease.service.jobrequest;

import com.api.auto_ease.domain.booking.Booking;
import com.api.auto_ease.domain.booking.BookingStatus;
import com.api.auto_ease.domain.car.Car;
import com.api.auto_ease.domain.carMake.CarMake;
import com.api.auto_ease.domain.carModel.CarModel;
import com.api.auto_ease.domain.garage.Garage;
import com.api.auto_ease.domain.garageCategory.GarageCategoryAssignment;
import com.api.auto_ease.domain.jobrequest.JobRequest;
import com.api.auto_ease.domain.jobrequest.JobRequestStatus;
import com.api.auto_ease.domain.jobrequest.Urgency;
import com.api.auto_ease.domain.quote.Quote;
import com.api.auto_ease.domain.quote.QuoteStatus;
import com.api.auto_ease.domain.review.Review;
import com.api.auto_ease.domain.serviceCategory.ServiceCategory;
import com.api.auto_ease.dto.jobrequest.CreateJobRequestRequest;
import com.api.auto_ease.dto.jobrequest.JobRequestResponse;
import com.api.auto_ease.dto.jobrequest.UpdateJobRequestRequest;
import com.api.auto_ease.repository.booking.BookingRepository;
import com.api.auto_ease.repository.car.CarRepository;
import com.api.auto_ease.repository.carMake.CarMakeRepository;
import com.api.auto_ease.repository.carModel.CarModelRepository;
import com.api.auto_ease.repository.garageCategory.GarageCategoryAssignmentRepository;
import com.api.auto_ease.repository.jobrequest.JobRequestRepository;
import com.api.auto_ease.repository.quote.QuoteRepository;
import com.api.auto_ease.repository.review.ReviewRepository;
import com.api.auto_ease.repository.serviceCategory.ServiceCategoryRepository;
import com.api.auto_ease.service.garage.GarageJobAuthorization;
import com.api.auto_ease.service.garage.GarageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobRequestService {

    private final JobRequestRepository jobRequestRepository;
    private final CarRepository carRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final QuoteRepository quoteRepository;
    private final CarMakeRepository carMakeRepository;
    private final CarModelRepository carModelRepository;
    private final GarageCategoryAssignmentRepository garageCategoryAssignmentRepository;
    private final GarageService garageService;
    private final GarageJobAuthorization garageJobAuthorization;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public JobRequestResponse createJobRequest(String userId, CreateJobRequestRequest request) {
        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Car not found"));

        if (!car.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this car");
        }

        if (request.getCategoryId() != null) {
            serviceCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category ID"));
        }

        Urgency urgency = Urgency.NORMAL;
        if (request.getUrgency() != null) {
            try {
                urgency = Urgency.valueOf(request.getUrgency());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid urgency value");
            }
        }

        JobRequest jobRequest = new JobRequest(null, userId, request.getCarId(),
                request.getCategoryId(), request.getTitle(), request.getDescription(),
                urgency, request.getPreferredDate(), request.getBudgetMin(), request.getBudgetMax(),
                JobRequestStatus.OPEN, request.getLocationAddress(), request.getLocationCity(),
                request.getLocationState(), null, null);

        jobRequest = jobRequestRepository.save(jobRequest);
        return toResponse(jobRequest, car);
    }

    public JobRequestResponse getJobRequest(String userId, UUID id) {
        JobRequest jobRequest = jobRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job request not found"));

        if (!jobRequest.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this job request");
        }

        return toResponse(jobRequest);
    }

    public List<JobRequestResponse> getMyJobRequests(String userId, UUID categoryId) {
        List<JobRequest> jobRequests;
        if (categoryId != null) {
            jobRequests = jobRequestRepository.findByUserIdAndCategoryIdOrderByCreatedDateDesc(userId, categoryId);
        } else {
            jobRequests = jobRequestRepository.findByUserIdOrderByCreatedDateDesc(userId);
        }
        return jobRequests.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Open job requests visible to this garage: optionally filtered by query {@code categoryId},
     * then restricted to categories accepted on the garage when at least one is configured.
     */
    @Transactional(readOnly = true)
    public List<JobRequestResponse> getOpenJobRequestsForGarageUser(String garageUserId, UUID categoryId) {
        Garage garage = garageService.requireApprovedGarageForUser(garageUserId);
        Set<UUID> acceptedIds = new HashSet<>();
        for (GarageCategoryAssignment assignment : garageCategoryAssignmentRepository.findByGarage_Id(garage.getId())) {
            acceptedIds.add(assignment.getCategory().getId());
        }

        List<JobRequest> jobRequests;
        if (categoryId != null) {
            jobRequests = jobRequestRepository.findByStatusAndCategoryIdOrderByCreatedDateDesc(JobRequestStatus.OPEN, categoryId);
        } else {
            jobRequests = jobRequestRepository.findByStatusOrderByCreatedDateDesc(JobRequestStatus.OPEN);
        }

        return jobRequests.stream()
                .filter(jr -> isJobVisibleToGarage(jr, acceptedIds))
                .map(this::toResponse)
                .toList();
    }

    private boolean isJobVisibleToGarage(JobRequest jobRequest, Set<UUID> acceptedCategoryIds) {
        if (jobRequest.getCategoryId() == null) {
            return true;
        }
        if (acceptedCategoryIds.isEmpty()) {
            return true;
        }
        return acceptedCategoryIds.contains(jobRequest.getCategoryId());
    }

    @Transactional
    public JobRequestResponse updateJobRequest(String userId, UUID id, UpdateJobRequestRequest request) {
        JobRequest jobRequest = jobRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job request not found"));

        if (!jobRequest.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this job request");
        }

        if (jobRequest.getStatus() != JobRequestStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only update OPEN requests");
        }

        if (request.getCategoryId() != null) {
            serviceCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category ID"));
            jobRequest.setCategoryId(request.getCategoryId());
        }
        if (request.getTitle() != null) {
            jobRequest.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            jobRequest.setDescription(request.getDescription());
        }
        if (request.getUrgency() != null) {
            try {
                jobRequest.setUrgency(Urgency.valueOf(request.getUrgency()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid urgency value");
            }
        }
        if (request.getPreferredDate() != null) {
            jobRequest.setPreferredDate(request.getPreferredDate());
        }
        if (request.getBudgetMin() != null) {
            jobRequest.setBudgetMin(request.getBudgetMin());
        }
        if (request.getBudgetMax() != null) {
            jobRequest.setBudgetMax(request.getBudgetMax());
        }
        if (request.getLocationAddress() != null) {
            jobRequest.setLocationAddress(request.getLocationAddress());
        }
        if (request.getLocationCity() != null) {
            jobRequest.setLocationCity(request.getLocationCity());
        }
        if (request.getLocationState() != null) {
            jobRequest.setLocationState(request.getLocationState());
        }

        jobRequest = jobRequestRepository.save(jobRequest);
        return toResponse(jobRequest);
    }

    @Transactional
    public JobRequestResponse completeJob(String garageUserId, UUID jobRequestId) {
        garageJobAuthorization.assertGarageUserMayMutateJob(garageUserId, jobRequestId);

        JobRequest jobRequest = jobRequestRepository.findById(jobRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job request not found"));

        JobRequestStatus status = jobRequest.getStatus();
        if (status != JobRequestStatus.BOOKED && status != JobRequestStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Job can only be completed when status is BOOKED or IN_PROGRESS");
        }

        if (quoteRepository.existsByJobRequestIdAndStatus(jobRequestId, QuoteStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "All addendum quotes must be accepted or rejected before completing the job");
        }

        List<Quote> acceptedQuotes = quoteRepository.findByJobRequestIdAndStatus(jobRequestId, QuoteStatus.ACCEPTED);
        List<UUID> acceptedQuoteIds = acceptedQuotes.stream().map(Quote::getId).toList();
        List<Booking> bookings = bookingRepository.findByQuoteIdIn(acceptedQuoteIds);

        if (bookings.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No bookings exist for this job request");
        }

        for (Booking booking : bookings) {
            booking.setStatus(BookingStatus.COMPLETED);
        }
        bookingRepository.saveAll(bookings);

        jobRequest.setStatus(JobRequestStatus.COMPLETED);
        jobRequest = jobRequestRepository.save(jobRequest);

        return toResponse(jobRequest);
    }

    @Transactional
    public void deleteJobRequest(String userId, UUID id) {
        JobRequest jobRequest = jobRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job request not found"));

        if (!jobRequest.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this job request");
        }

        jobRequestRepository.delete(jobRequest);
    }

    private JobRequestResponse toResponse(JobRequest jobRequest) {
        Car car = carRepository.findById(jobRequest.getCarId()).orElse(null);
        return toResponse(jobRequest, car);
    }

    private JobRequestResponse toResponse(JobRequest jobRequest, Car car) {
        String makeName = "Unknown";
        String modelName = "Unknown";
        Integer carYear = null;

        if (car != null) {
            makeName = carMakeRepository.findById(car.getMakeId()).map(CarMake::getName).orElse("Unknown");
            modelName = carModelRepository.findById(car.getModelId()).map(CarModel::getName).orElse("Unknown");
            carYear = car.getYear();
        }

        String categoryName = null;
        if (jobRequest.getCategoryId() != null) {
            categoryName = serviceCategoryRepository.findById(jobRequest.getCategoryId())
                    .map(ServiceCategory::getName).orElse(null);
        }

        int quoteCount = quoteRepository.countByJobRequestId(jobRequest.getId());

        boolean reviewEligible = jobRequest.getStatus() == JobRequestStatus.COMPLETED
                && !reviewRepository.existsByJobRequestId(jobRequest.getId());
        UUID reviewId = reviewRepository.findByJobRequestId(jobRequest.getId())
                .map(Review::getId)
                .orElse(null);

        return JobRequestResponse.builder()
                .id(jobRequest.getId())
                .carId(jobRequest.getCarId())
                .makeName(makeName)
                .modelName(modelName)
                .carYear(carYear)
                .categoryId(jobRequest.getCategoryId())
                .categoryName(categoryName)
                .title(jobRequest.getTitle())
                .description(jobRequest.getDescription())
                .urgency(jobRequest.getUrgency().name())
                .preferredDate(jobRequest.getPreferredDate())
                .budgetMin(jobRequest.getBudgetMin())
                .budgetMax(jobRequest.getBudgetMax())
                .status(jobRequest.getStatus().name())
                .locationAddress(jobRequest.getLocationAddress())
                .locationCity(jobRequest.getLocationCity())
                .locationState(jobRequest.getLocationState())
                .quoteCount(quoteCount)
                .reviewEligible(reviewEligible)
                .reviewId(reviewId)
                .build();
    }
}
