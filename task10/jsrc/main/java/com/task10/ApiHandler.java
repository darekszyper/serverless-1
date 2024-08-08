package com.task10;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.environment.EnvironmentVariables;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.resources.DependsOn;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;

import static com.syndicate.deployment.model.environment.ValueTransformer.USER_POOL_NAME_TO_CLIENT_ID;
import static com.syndicate.deployment.model.environment.ValueTransformer.USER_POOL_NAME_TO_USER_POOL_ID;


@LambdaHandler(lambdaName = "api_handler",
        roleName = "api_handler-role",
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@DependsOn(resourceType = ResourceType.COGNITO_USER_POOL, name = "${booking_userpool}")
@EnvironmentVariables(value = {
        @EnvironmentVariable(key = "REGION", value = "${region}"),
        @EnvironmentVariable(key = "COGNITO_ID", value = "${pool_name}", valueTransformer = USER_POOL_NAME_TO_USER_POOL_ID),
        @EnvironmentVariable(key = "CLIENT_ID", value = "${pool_name}", valueTransformer = USER_POOL_NAME_TO_CLIENT_ID)
})
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoService cognitoService;
    private final TableService tableService;
    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;

    public ApiHandler() {
        this.cognitoService = new CognitoService(CognitoIdentityProviderClient.builder().build());
        this.tableService = new TableService(DynamoDbClient.builder().build());
        this.reservationService = new ReservationService(DynamoDbClient.builder().build());
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String path = request.getPath();
        String httpMethod = request.getHttpMethod();
        Map<String, String> headers = request.getHeaders();
        Map<String, String> queryParams = request.getQueryStringParameters();
        String body = request.getBody();

        switch (path) {
            case "/signup":
                return handleSignup(body);
            case "/signin":
                return handleSignin(body);
            case "/tables":
                return httpMethod.equals("GET") ? handleGetTables() : handleCreateTable(body);
            case "/reservations":
                return httpMethod.equals("GET") ? handleGetReservations() : handleCreateReservation(body);
            default:
                return new APIGatewayProxyResponseEvent().withStatusCode(400);
        }
    }

    private APIGatewayProxyResponseEvent handleSignup(String body) {
        try {
            Map<String, String> userData = objectMapper.readValue(body, Map.class);
            cognitoService.signUpUser(
                    userData.get("firstName"),
                    userData.get("lastName"),
                    userData.get("email"),
                    userData.get("password")
            );
            return new APIGatewayProxyResponseEvent().withStatusCode(200);
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent handleSignin(String body) {
        try {
            Map<String, String> credentials = objectMapper.readValue(body, Map.class);
            AdminInitiateAuthResponse response = cognitoService.signInUser(
                    credentials.get("email"),
                    credentials.get("password")
            );
            String accessToken = response.authenticationResult().idToken();
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("{\"accessToken\":\"" + accessToken + "\"}");
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent handleGetTables() {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(objectMapper.writeValueAsString(Map.of("tables", tableService.getAllTables())));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent handleCreateTable(String body) {
        try {
            Table table = objectMapper.readValue(body, Table.class);
            tableService.createTable(table);
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("{\"id\":" + table.getId() + "}");
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent handleGetReservations() {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(objectMapper.writeValueAsString(Map.of("reservations", reservationService.getAllReservations())));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent handleCreateReservation(String body) {
        try {
            Reservation reservation = objectMapper.readValue(body, Reservation.class);
            String reservationId = reservationService.createReservation(reservation);
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("{\"reservationId\":\"" + reservationId + "\"}");
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.getMessage());
        }
    }
}