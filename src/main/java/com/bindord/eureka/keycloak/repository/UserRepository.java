package com.bindord.eureka.keycloak.repository;

import com.bindord.eureka.keycloak.advice.CustomValidationException;
import com.bindord.eureka.keycloak.domain.request.EurekaUser;
import com.bindord.eureka.keycloak.domain.request.UserLogin;
import com.bindord.eureka.keycloak.domain.response.UserToken;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.util.Http;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static com.bindord.eureka.keycloak.util.Constants.OAUTH2_CLIENT_ID;
import static com.bindord.eureka.keycloak.util.Constants.OAUTH2_CLIENT_SECRET;
import static com.bindord.eureka.keycloak.util.Constants.OAUTH2_GRANT_TYPE;
import static com.bindord.eureka.keycloak.util.Constants.OAUTH2_GRANT_TYPE_RT;
import static com.bindord.eureka.keycloak.util.Constants.OAUTH2_SECRET;
import static com.bindord.eureka.keycloak.util.Constants.OAUTH2_URI_REALMS;
import static com.bindord.eureka.keycloak.util.Constants.OAUTH2_URI_TOKEN_OPERATION;

@Repository
@Slf4j
public class UserRepository {

    private final Keycloak keycloak;

    private final Configuration keycloakConfig;

    public UserRepository(Keycloak keycloak, Configuration keycloakConfig) {
        this.keycloak = keycloak;
        this.keycloakConfig = keycloakConfig;
    }

    @Value("${keycloak.realm}")
    private String realm;

    public UserToken authenticate(UserLogin user) {

        String username = user.getEmail();
        String pwd = user.getPassword();

        AuthzClient authzClient = AuthzClient.create(keycloakConfig);
        AccessTokenResponse response = authzClient.obtainAccessToken(username, pwd);
        var userToken = new UserToken();
        BeanUtils.copyProperties(response, userToken);
        return userToken;
    }

    public UserRepresentation save(EurekaUser eurekaUser) throws CustomValidationException {
        RealmResource realmResource = keycloak.realm(realm);
        UserRepresentation userRepresentation = new UserRepresentation();
        BeanUtils.copyProperties(eurekaUser, userRepresentation);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(eurekaUser.getPassword());
        credential.setTemporary(false);

        userRepresentation.setCredentials(List.of(credential));
        userRepresentation.setEnabled(true);

        Response response = realmResource.users().create(userRepresentation);
        var resCode = response.getStatus();
        if (resCode == HttpStatus.OK.value() || resCode == HttpStatus.CREATED.value()) {
            var location = response.getLocation().toString();
            var userId = Lists.reverse(Arrays.asList(location.split("/"))).get(0);
            userRepresentation.setId(userId);
            return userRepresentation;
        }
        var errDetail = response.readEntity(ErrorRepresentation.class);
        throw new CustomValidationException(errDetail.getErrorMessage());
    }

    public UserToken refreshToken(String refreshToken) {
        AuthzClient authzClient = AuthzClient.create(keycloakConfig);

        String url = new StringBuilder(
                keycloakConfig.getAuthServerUrl())
                .append(OAUTH2_URI_REALMS)
                .append(keycloakConfig.getRealm())
                .append(OAUTH2_URI_TOKEN_OPERATION).toString();

        String clientId = keycloakConfig.getResource();
        String secret = (String) keycloakConfig.getCredentials().get(OAUTH2_SECRET);
        Http http = new Http(authzClient.getConfiguration(), (params, headers) -> {
        });

        AccessTokenResponse accessTokenResponse = http.<AccessTokenResponse>post(url)
                .authentication()
                .client()
                .form()
                .param(OAUTH2_GRANT_TYPE, OAUTH2_GRANT_TYPE_RT)
                .param(OAUTH2_GRANT_TYPE_RT, refreshToken)
                .param(OAUTH2_CLIENT_ID, clientId)
                .param(OAUTH2_CLIENT_SECRET, secret)
                .response()
                .json(AccessTokenResponse.class)
                .execute();

        var userToken = new UserToken();
        BeanUtils.copyProperties(accessTokenResponse, userToken);
        return userToken;
    }

    public String deleteAllUsers() {
        RealmResource realmResource = keycloak.realm(realm);
        //GET TOTAL USERS
        // --> realmResource.users().count());
        //LIST USERS WHITIN RANGE (INIT, MAX)
        // --> realmResource.users().list(from, until)
        realmResource.users().list(0, realmResource.users().count()).forEach(user -> {
            realmResource.users().delete(user.getId());
            log.info("User deleted with ID {}", user.getId());
        });
        return HttpStatus.OK.toString();
    }
}
