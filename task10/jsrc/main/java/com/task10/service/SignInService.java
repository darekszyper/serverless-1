package com.task10.service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.HashMap;
import java.util.Map;



public class SignInService {

    private CognitoIdentityProviderClient cognitoClient;

    public SignInService() {
        cognitoClient = CognitoIdentityProviderClient.create();
    }

    public APIGatewayProxyResponseEvent handleSignIn(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent) {

        JSONParser parser = new JSONParser();
        JSONObject bodyJson = null;
        try {
            bodyJson = (JSONObject) parser.parse(apiGatewayProxyRequestEvent.getBody());
        } catch (org.json.simple.parser.ParseException e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400);
        }


        String email = (String) bodyJson.get("email");
        String password = (String) bodyJson.get("password");
        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", email);
        authParameters.put("PASSWORD", password);

        AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                .clientId(getClientId())
                .userPoolId(getPoolId())
                .authParameters(authParameters)
                .build();
        try {
            AdminInitiateAuthResponse authResponse = cognitoClient.adminInitiateAuth(authRequest);
            if (authResponse.sdkHttpResponse().statusCode() == 200) {
                String accessToken = authResponse.authenticationResult().idToken();
                return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("{\"accessToken\":\"" + accessToken + "\"}");
            } else {
                return new APIGatewayProxyResponseEvent().withStatusCode(400);
            }
        } catch (CognitoIdentityProviderException e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400);
        }
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
}
