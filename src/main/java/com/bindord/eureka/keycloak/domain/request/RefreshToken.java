package com.bindord.eureka.keycloak.domain.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class RefreshToken {

    @Size(min = 1, max = 9999)
    @NotBlank
    private String refreshToken;
}
