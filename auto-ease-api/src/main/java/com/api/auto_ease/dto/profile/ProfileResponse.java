package com.api.auto_ease.dto.profile;

import com.api.auto_ease.domain.appUser.AppUserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private UUID id;
    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private AppUserType userType;
}
