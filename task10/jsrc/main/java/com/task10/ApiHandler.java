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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
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
        @EnvironmentVariable(key = "COGNITO_ID", value = "cmtr-7a75be14-simple-booking-userpool-test", valueTransformer = USER_POOL_NAME_TO_USER_POOL_ID),
        @EnvironmentVariable(key = "CLIENT_ID", value = "cmtr-7a75be14-simple-booking-userpool-test", valueTransformer = USER_POOL_NAME_TO_CLIENT_ID)
})
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ApiHandler.class);

    private final CognitoService cognitoService;
    private final TableService tableService;
    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;
    private final Map<String, String> headersForCORS;
    private final Map<RouteKey, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>> handlersByRouteKey;



    public ApiHandler() {
        logger.info("Initializing ApiHandler...");
        this.cognitoService = new CognitoService(CognitoIdentityProviderClient.builder()
                .region(Region.of(System.getenv("REGION")))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build());
        this.tableService = new TableService(DynamoDbClient.builder().build());
        this.reservationService = new ReservationService(DynamoDbClient.builder().build());
        this.objectMapper = new ObjectMapper();
        this.headersForCORS = initHeadersForCORS();
        this.handlersByRouteKey = initHandlers();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        String path = requestEvent.getPath();
        String httpMethod = requestEvent.getHttpMethod();
        Map<String, String> headers = requestEvent.getHeaders();
        String body = requestEvent.getBody();

        logger.info("Received requestEvent - Path: {}, Method: {}, Headers: {}", path, httpMethod, headers);

        switch (path) {
            case "/signup":
            case "/signin":
                return getHandler(requestEvent)
                        .handleRequest(requestEvent, context)
                        .withHeaders(headersForCORS);
            case "/tables":
                return httpMethod.equals("GET") ? handleGetTables() : handleCreateTable(body);
            case "/reservations":
                return httpMethod.equals("GET") ? handleGetReservations() : handleCreateReservation(body);
            default:
                logger.error("Invalid path: {}", path);
                return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Invalid path.");
        }
    }

    private RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> getHandler(APIGatewayProxyRequestEvent requestEvent) {
        return handlersByRouteKey.get(getRouteKey(requestEvent));
    }

    private RouteKey getRouteKey(APIGatewayProxyRequestEvent requestEvent) {
        return new RouteKey(requestEvent.getHttpMethod(), requestEvent.getPath());
    }

    private APIGatewayProxyResponseEvent handleSignup(String body) {
        logger.info("Handling signup with body: {}", body);
        try {
            Map<String, String> userData = objectMapper.readValue(body, Map.class);
            logger.debug("Parsed signup data: {}", userData);

            cognitoService.signUpUser(
                    userData.get("firstName"),
                    userData.get("lastName"),
                    userData.get("email"),
                    userData.get("password")
            );
            logger.info("Signup successful for email: {}", userData.get("email"));
            return new APIGatewayProxyResponseEvent().withStatusCode(200);
        } catch (Exception e) {
            logger.error("Signup failed: {}", e.getMessage(), e);
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent handleSignin(String body) {
        logger.info("Handling signin with body: {}", body);
        try {
            Map<String, String> credentials = objectMapper.readValue(body, Map.class);
            logger.debug("Parsed signin data: {}", credentials);

            AdminInitiateAuthResponse response = cognitoService.signInUser(
                    credentials.get("email"),
                    credentials.get("password")
            );
            String accessToken = response.authenticationResult().idToken();
            logger.info("Signin successful for email: {}", credentials.get("email"));
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("{\"accessToken\":\"" + accessToken + "\"}");
        } catch (Exception e) {
            logger.error("Signin failed: {}", e.getMessage(), e);
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent handleGetTables() {
        logger.info("Handling getTables request");
        try {
            var tables = tableService.getAllTables();
            logger.debug("Retrieved tables: {}", tables);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(objectMapper.writeValueAsString(Map.of("tables", tables)));
        } catch (Exception e) {
            logger.error("GetTables failed: {}", e.getMessage(), e);
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent handleCreateTable(String body) {
        logger.info("Handling createTable with body: {}", body);
        try {
            Table table = objectMapper.readValue(body, Table.class);
            logger.debug("Parsed table data: {}", table);

            tableService.createTable(table);
            logger.info("Table created with ID: {}", table.getId());
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("{\"id\":" + table.getId() + "}");
        } catch (Exception e) {
            logger.error("CreateTable failed: {}", e.getMessage(), e);
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent handleGetReservations() {
        logger.info("Handling getReservations request");
        try {
            var reservations = reservationService.getAllReservations();
            logger.debug("Retrieved reservations: {}", reservations);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(objectMapper.writeValueAsString(Map.of("reservations", reservations)));
        } catch (Exception e) {
            logger.error("GetReservations failed: {}", e.getMessage(), e);
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent handleCreateReservation(String body) {
        logger.info("Handling createReservation with body: {}", body);
        try {
            Reservation reservation = objectMapper.readValue(body, Reservation.class);
            logger.debug("Parsed reservation data: {}", reservation);

            String reservationId = reservationService.createReservation(reservation);
            logger.info("Reservation created with ID: {}", reservationId);
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("{\"reservationId\":\"" + reservationId + "\"}");
        } catch (Exception e) {
            logger.error("CreateReservation failed: {}", e.getMessage(), e);
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.getMessage());
        }
    }

    private Map<RouteKey, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>> initHandlers() {
        return Map.of(
                new RouteKey("POST", "/signup"), new PostSignUpHandler(cognitoService.getCognitoClient()),
                new RouteKey("POST", "/signin"), new PostSignInHandler(cognitoService.getCognitoClient())
        );
    }

    /**
     * To allow all origins, all methods, and common headers
     * <a href="https://docs.aws.amazon.com/apigateway/latest/developerguide/how-to-cors.html">Using cross-origin resource sharing (CORS)</a>
     */
    private Map<String, String> initHeadersForCORS() {
        return Map.of(
                "Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token",
                "Access-Control-Allow-Origin", "*",
                "Access-Control-Allow-Methods", "*",
                "Accept-Version", "*"
        );
    }
}
