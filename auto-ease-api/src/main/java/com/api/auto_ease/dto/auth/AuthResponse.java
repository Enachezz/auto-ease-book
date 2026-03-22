package com.api.auto_ease.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.api.auto_ease.domain.appUser.AppUserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String token;
    private String userId;
    private String email;
    private String fullName;
    private AppUserType userType;
}
