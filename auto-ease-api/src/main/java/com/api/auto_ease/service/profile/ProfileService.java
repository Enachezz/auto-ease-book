package com.api.auto_ease.service.profile;

import com.api.auto_ease.domain.appUser.AppUser;
import com.api.auto_ease.domain.profile.Profile;
import com.api.auto_ease.dto.profile.ProfileResponse;
import com.api.auto_ease.dto.profile.UpdateProfileRequest;
import com.api.auto_ease.repository.appUser.AppUserRepository;
import com.api.auto_ease.repository.profile.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final AppUserRepository appUserRepository;

    public ProfileResponse getProfile(String userId) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return toResponse(profile, user);
    }

    @Transactional
    public ProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));

        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }

        profileRepository.save(profile);

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return toResponse(profile, user);
    }

    private ProfileResponse toResponse(Profile profile, AppUser user) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .avatarUrl(profile.getAvatarUrl())
                .userType(user.getType())
                .build();
    }
}
