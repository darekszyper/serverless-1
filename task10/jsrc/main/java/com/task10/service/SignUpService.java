package com.task10.service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;


public class SignUpService {

    private CognitoIdentityProviderClient cognitoClient;

    public SignUpService() {
        cognitoClient = CognitoIdentityProviderClient.create();
    }

    public APIGatewayProxyResponseEvent handleSignUp(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent) {

        JSONParser parser = new JSONParser();
        JSONObject bodyJson = null;
        try {
            bodyJson = (JSONObject) parser.parse(apiGatewayProxyRequestEvent.getBody());
        } catch (org.json.simple.parser.ParseException e) {
            return errorResponse("User registration failed " + e);
        }
        String firstName = (String) bodyJson.get("firstName");
        String lastName = (String) bodyJson.get("lastName");
        String email = (String) bodyJson.get("email");
        String password = (String) bodyJson.get("password");
        try {
            AdminConfirmSignUpResponse createUserResponse = registerUserInCognito(email, password, firstName, lastName);
            return createUserResponse.sdkHttpResponse().isSuccessful() ?
                    successResponse("User registered successfully") :
                    errorResponse("User registration failed");
        } catch (CognitoIdentityProviderException exc) {
            return errorResponse("User registration failed " + exc);
        }
    }

    private AdminConfirmSignUpResponse registerUserInCognito(String email, String password, String firstName, String lastName) {
        AttributeType userAttrs = AttributeType.builder()
                .name("name").value(firstName + " " + lastName)
                .name("email").value(email)
                .build();
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .userAttributes(userAttrs)
                .username(email)
                .clientId(getClientId())
                .password(password)
                .build();
        getCognitoIdentityProviderClient().signUp(signUpRequest);
        AdminConfirmSignUpRequest confirmSignUpRequest = AdminConfirmSignUpRequest.builder()
                .userPoolId(getPoolId())
                .username(email)
                .build();
        return getCognitoIdentityProviderClient().adminConfirmSignUp(confirmSignUpRequest);
    }

    private String getClientId() {
        ListUserPoolClientsRequest listUserPoolClientsRequest = ListUserPoolClientsRequest.builder()
                .userPoolId(getPoolId())
                .build();
        ListUserPoolClientsResponse listUserPoolClientsResponse = getCognitoIdentityProviderClient().listUserPoolClients(listUserPoolClientsRequest);
        return listUserPoolClientsResponse.userPoolClients().get(0).clientId();
    }

    private String getPoolId() {
        String userPoolName = "cmtr-7a75be14-simple-booking-userpool-test";
        ListUserPoolsRequest listUserPoolsRequest = ListUserPoolsRequest.builder()
                .maxResults(10)
                .build();
        ListUserPoolsResponse listUserPoolsResponse = getCognitoIdentityProviderClient().listUserPools(listUserPoolsRequest);
        return listUserPoolsResponse.userPools().stream()
                .filter(pool -> userPoolName.equals(pool.name()))
                .findFirst()
                .map(UserPoolDescriptionType::id)
                .orElse(null);
    }

    private CognitoIdentityProviderClient getCognitoIdentityProviderClient() {
        if (cognitoClient == null) {
            this.cognitoClient = CognitoIdentityProviderClient.builder()
                    .region(Region.EU_CENTRAL_1)
                    .build();
        }
        return cognitoClient;
    }

    public static APIGatewayProxyResponseEvent successResponse(String message) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(message);
    }

    public static APIGatewayProxyResponseEvent errorResponse(String errorMessage) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody(errorMessage);
    }
}
