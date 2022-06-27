package com.bindord.eureka.keycloak.service;

import com.bindord.eureka.keycloak.advice.CustomValidationException;
import com.bindord.eureka.keycloak.advice.NotFoundValidationException;
import com.bindord.eureka.keycloak.domain.User;
import com.bindord.eureka.keycloak.domain.dto.PasswordDTO;
import com.bindord.eureka.keycloak.domain.request.UserLogin;
import com.bindord.eureka.keycloak.domain.response.UserToken;
import com.bindord.eureka.keycloak.generic.BaseService;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.UUID;

public interface UserService extends BaseService<User, UUID> {

    List<UserRepresentation> getAll();

    UserToken doAuthenticate(UserLogin user);

    User findByUsername(String username);

    void updatePasswordById(UUID id, String nuevaPass);

    String initRecoverPass(String username) throws CustomValidationException, NotFoundValidationException;

    String doChangePassword(PasswordDTO passwordDTO) throws CustomValidationException;
}
