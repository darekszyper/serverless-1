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
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.ResourceType;
import com.syndicate.deployment.model.RetentionSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;

import static com.syndicate.deployment.model.environment.ValueTransformer.USER_POOL_NAME_TO_CLIENT_ID;
import static com.syndicate.deployment.model.environment.ValueTransformer.USER_POOL_NAME_TO_USER_POOL_ID;

@LambdaHandler(lambdaName = "api_handler",
        roleName = "api_handler-role",
        runtime = DeploymentRuntime.JAVA17,
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

    private final CognitoIdentityProviderClient cognitoClient;
    private final TableService tableService;
    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;
    private final Map<String, String> headersForCORS;
    private final Map<RouteKey, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>> handlersByRouteKey;

    public ApiHandler() {
        logger.info("Initializing ApiHandler...");
        this.cognitoClient = initCognitoClient();
//        this.cognitoService = new CognitoService(CognitoIdentityProviderClient.builder()
//                .region(Region.of(System.getenv("REGION")))
//                .credentialsProvider(DefaultCredentialsProvider.create())
//                .build());
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

        if (path.equals("/signin"))
            logger.info("2343254543543545535\nReceived requestEvent - Path: {}, Method: {}, Headers: {}, Body: {}", path, httpMethod, headers, body);
        logger.info("Received requestEvent - Path: {}, Method: {}, Headers: {}, Body: {}", path, httpMethod, headers, body);

        try {
            switch (path) {
                case "/signup":
                    logger.info("23253253 - Handling signup request for path: {}", path);
                    return getHandler(requestEvent)
                            .handleRequest(requestEvent, context)
                            .withHeaders(headersForCORS);
                case "/signin":
                    logger.info("2343254543543545535\nHandling authentication request for path: {}", path);
                    return getHandler(requestEvent)
                            .handleRequest(requestEvent, context)
                            .withHeaders(headersForCORS);
                case "/tables":
                    return httpMethod.equals("GET") ? handleGetTables() : handleCreateTable(body);
                case "/reservations":
                    return httpMethod.equals("GET") ? handleGetReservations() : handleCreateReservation(body);
                default:
                    if (httpMethod.equals("GET") && path.matches("/tables/\\d+")) {
                        return handleGetTableById(path);
                    } else {
                        logger.error("Invalid path or method: {} {}", path, httpMethod);
                        return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Invalid path or method.");
                    }
//                case "/tables":
//                    return httpMethod.equals("GET") ? handleGetTables() : handleCreateTable(body);
//                case "/reservations":
//                    return httpMethod.equals("GET") ? handleGetReservations() : handleCreateReservation(body);
//                default:
//                    logger.error("Invalid path: {}", path);
//                    return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Invalid path.");
            }
        } catch (Exception e) {
            if (path.equals("/signup")) logger.error("23253253 - Error handling signup request: {}", e.getMessage(), e);
            if (path.equals("/signin"))
                logger.error("2343254543543545535\nError handling request: {}", e.getMessage(), e);
            logger.error("Error handling request: {}", e.getMessage(), e);
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Internal Server Error");
        }
    }

    private APIGatewayProxyResponseEvent handleGetTableById(String path) {
        logger.info("Handling getTableById request for path: {}", path);
        try {
            // Extract tableId from the path
            String[] pathParts = path.split("/");
            int tableId = Integer.parseInt(pathParts[pathParts.length - 1]);

            // Retrieve table by ID
            Table table = tableService.getTable(tableId);
            if (table == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withBody("Table not found");
            }

            // Return the table information
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(objectMapper.writeValueAsString(table));
        } catch (Exception e) {
            logger.error("GetTableById failed: {}", e.getMessage(), e);
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.getMessage());
        }
    }


    private CognitoIdentityProviderClient initCognitoClient() {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(System.getenv("REGION")))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    private RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> getHandler(APIGatewayProxyRequestEvent requestEvent) {
        RouteKey routeKey = getRouteKey(requestEvent);
        if (requestEvent.getPath().equals("/signin"))
            logger.debug("2343254543543545535\nDetermined RouteKey: {}", routeKey);
        logger.debug("Determined RouteKey: {}", routeKey);
        return handlersByRouteKey.get(routeKey);
    }

    private RouteKey getRouteKey(APIGatewayProxyRequestEvent requestEvent) {
        return new RouteKey(requestEvent.getHttpMethod(), requestEvent.getPath());
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
                new RouteKey("POST", "/signup"), new PostSignUpHandler(cognitoClient),
                new RouteKey("POST", "/signin"), new PostSignInHandler(cognitoClient)
        );
    }

    private Map<String, String> initHeadersForCORS() {
        return Map.of(
                "Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token",
                "Access-Control-Allow-Origin", "*",
                "Access-Control-Allow-Methods", "*",
                "Accept-Version", "*"
        );
    }
}
