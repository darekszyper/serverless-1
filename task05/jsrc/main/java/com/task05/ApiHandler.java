package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "api_handler",
        roleName = "api_handler-role",
        isPublishVersion = false,
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final DynamoDB dynamoDB;
    private final Table table;
    private final ObjectMapper objectMapper;

    public ApiHandler() {
        this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
        this.table = dynamoDB.getTable("cmtr-7a75be14-Events-test");
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            // Parse the input
            JsonNode body = objectMapper.readTree(request.getBody());
            int principalId = body.get("principalId").asInt();
            JsonNode content = body.get("content");

            // Create the event item
            String id = UUID.randomUUID().toString();
            OffsetDateTime now = OffsetDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            String createdAt = now.format(formatter);


            Item item = new Item()
                    .withPrimaryKey("id", id)
                    .withInt("principalId", principalId)
                    .withString("createdAt", createdAt)
                    .withMap("body", objectMapper.convertValue(content, Map.class));

            // Write the item to DynamoDB
            table.putItem(item);

            // Prepare the response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("statusCode", 201);
            responseBody.put("event", item.asMap());

            response.setStatusCode(201);
            response.setBody(objectMapper.writeValueAsString(responseBody));
        } catch (AmazonDynamoDBException e) {
            e.printStackTrace();
            response.setStatusCode(500);
            response.setBody("{\"error\":\"Internal Server Error\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatusCode(400);
            response.setBody("{\"error\":\"Bad Request\"}");
        }

        return response;
    }
}