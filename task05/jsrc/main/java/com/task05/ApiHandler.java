package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "api_handler",
	roleName = "api_handler-role",
	isPublishVersion = false,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
public class ApiHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

	private final Table table;

	public ApiHandler() {
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
		DynamoDB dynamoDB = new DynamoDB(client);
		this.table = dynamoDB.getTable("cmtr-7a75be14-Events");
	}

	@Override
	public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
		LambdaLogger logger = context.getLogger();
		Map<String, Object> response = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode eventNode = mapper.createObjectNode();

		try {
			logger.log("Received input: " + input);

			// Extracting the body from the input
			String body = (String) input.get("body");
			Map<String, Object> requestBody = mapper.readValue(body, Map.class);

			logger.log("Parsed body: " + requestBody);

			int principalId = (Integer) requestBody.get("principalId");
			Map<String, String> content = (Map<String, String>) requestBody.get("content");

			String id = UUID.randomUUID().toString();
			String createdAt = Instant.now().toString();

			eventNode.put("id", id);
			eventNode.put("principalId", principalId);
			eventNode.put("createdAt", createdAt);
			eventNode.set("body", mapper.convertValue(content, ObjectNode.class));

			Item item = new Item()
					.withPrimaryKey("id", id)
					.withNumber("principalId", principalId)
					.withString("createdAt", createdAt)
					.withMap("body", content);

			logger.log("Writing item to DynamoDB: " + item.toJSON());

			table.putItem(item);

			response.put("statusCode", 201);
			response.put("event", mapper.convertValue(eventNode, Map.class));

		} catch (ClassCastException e) {
			logger.log("Class cast exception: " + e.getMessage());
			response.put("statusCode", 400);
			response.put("error", "Invalid input type: " + e.getMessage());
		} catch (Exception e) {
			logger.log("Error processing request: " + e.toString());
			for (StackTraceElement stackTraceElement : e.getStackTrace()) {
				logger.log(stackTraceElement.toString());
			}
			response.put("statusCode", 500);
			response.put("error", e.getMessage());
		}

		return response;
	}
}
