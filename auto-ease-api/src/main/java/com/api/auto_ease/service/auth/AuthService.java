package com.api.auto_ease.service.auth;

import com.api.auto_ease.domain.appUser.AppUser;
import com.api.auto_ease.domain.profile.Profile;
import com.api.auto_ease.dto.auth.AuthResponse;
import com.api.auto_ease.dto.auth.LoginRequest;
import com.api.auto_ease.dto.auth.RegisterRequest;
import com.api.auto_ease.repository.appUser.AppUserRepository;
import com.api.auto_ease.repository.profile.ProfileRepository;
import com.api.auto_ease.security.JwtService;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        AppUser user = new AppUser(
                UUID.randomUUID().toString(),
                null, null,
                request.getEmail(),
                null,
                request.getUserType(),
                passwordEncoder.encode(request.getPassword())
        );
        appUserRepository.save(user);

        Profile profile = new Profile(
                null,
                user.getUuid(),
                request.getFullName(),
                request.getEmail(),
                request.getPhone(),
                null, null, null
        );
        profileRepository.save(profile);

        String token = jwtService.generateToken(user.getUuid(), user.getEmail(), user.getType());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getUuid())
                .email(user.getEmail())
                .fullName(profile.getFullName())
                .userType(user.getType())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        Profile profile = profileRepository.findByUserId(user.getUuid()).orElse(null);
        String fullName = profile != null ? profile.getFullName() : null;

        String token = jwtService.generateToken(user.getUuid(), user.getEmail(), user.getType());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getUuid())
                .email(user.getEmail())
                .fullName(fullName)
                .userType(user.getType())
                .build();
    }
}
