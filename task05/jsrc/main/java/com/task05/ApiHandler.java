package com.task05;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
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
		this.table = dynamoDB.getTable("Events");
	}

	@Override
	public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
		Map<String, Object> response = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode eventNode = mapper.createObjectNode();

		try {
			int principalId = (int) input.get("principalId");
			Map<String, String> content = (Map<String, String>) input.get("content");

			String id = UUID.randomUUID().toString();
			String createdAt = Instant.now().toString();

			eventNode.put("id", id);
			eventNode.put("principalId", principalId);
			eventNode.put("createdAt", createdAt);
			eventNode.set("body", mapper.convertValue(content, ObjectNode.class));

			table.putItem(new Item()
					.withPrimaryKey("id", id)
					.withNumber("principalId", principalId)
					.withString("createdAt", createdAt)
					.withMap("body", content));

			response.put("statusCode", 201);
			response.put("event", mapper.convertValue(eventNode, Map.class));

		} catch (Exception e) {
			response.put("statusCode", 500);
			response.put("error", e.getMessage());
		}

		return response;
	}
}
