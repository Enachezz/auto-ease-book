package com.api.auto_ease.controller.profile;

import com.api.auto_ease.dto.profile.ProfileResponse;
import com.api.auto_ease.dto.profile.UpdateProfileRequest;
import com.api.auto_ease.service.profile.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/api/profiles/me")
    public ProfileResponse getMyProfile(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return profileService.getProfile(userId);
    }

    @PutMapping("/api/profiles/me")
    public ProfileResponse updateMyProfile(Authentication auth,
                                          @RequestBody UpdateProfileRequest request) {
        String userId = (String) auth.getPrincipal();
        return profileService.updateProfile(userId, request);
    }
}
