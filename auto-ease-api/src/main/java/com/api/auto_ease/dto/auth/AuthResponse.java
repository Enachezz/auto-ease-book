package com.api.auto_ease.dto.auth;

import com.api.auto_ease.domain.appUser.AppUserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String userId;
    private String email;
    private String fullName;
    private AppUserType userType;
}
