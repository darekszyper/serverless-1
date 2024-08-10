package com.task11;// CognitoService.java
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.Map;

public class CognitoService {

    private final CognitoIdentityProviderClient cognitoClient;

    public CognitoService(CognitoIdentityProviderClient cognitoClient) {
        this.cognitoClient = cognitoClient;
    }

//    public void signUpUser(String firstName, String lastName, String email, String password) {
//        AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
//                .userPoolId(System.getenv("COGNITO_ID"))
//                .username(email)
//                .userAttributes(
//                        AttributeType.builder().name("given_name").value(firstName).build(),
//                        AttributeType.builder().name("family_name").value(lastName).build(),
//                        AttributeType.builder().name("email").value(email).build(),
//                        AttributeType.builder().name("email_verified").value("true").build()
//                )
//                .temporaryPassword(password)
//                .messageAction(MessageActionType.SUPPRESS)
//                .build();
//        cognitoClient.adminCreateUser(createUserRequest);
//    }
//
//    public AdminInitiateAuthResponse signInUser(String email, String password) {
//        AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
//                .userPoolId(System.getenv("COGNITO_ID"))
//                .clientId(System.getenv("CLIENT_ID"))
//                .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
//                .authParameters(Map.of("USERNAME", email, "PASSWORD", password))
//                .build();
//        return cognitoClient.adminInitiateAuth(authRequest);
//    }
//
//    public CognitoIdentityProviderClient getCognitoClient() {
//        return cognitoClient;
//    }
}
