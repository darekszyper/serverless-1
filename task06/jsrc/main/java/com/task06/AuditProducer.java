package com.task06;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "audit_producer",
		roleName = "audit_producer-role",
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@DynamoDbTriggerEventSource(targetTable = "Configuration", batchSize = 1)
public class AuditProducer implements RequestHandler<DynamodbEvent, Void> {

	private final AmazonDynamoDB dynamoDB;

	public AuditProducer() {
		this.dynamoDB = AmazonDynamoDBClientBuilder.defaultClient();
	}

	@Override
	public Void handleRequest(DynamodbEvent event, Context context) {
		event.getRecords().forEach(streamRecord -> {
			switch (streamRecord.getEventName()) {
				case "INSERT":
					handleInsert(streamRecord.getDynamodb().getNewImage());
					break;
				case "MODIFY":
					handleUpdate(streamRecord.getDynamodb().getOldImage(), streamRecord.getDynamodb().getNewImage());
					break;
				default:
					break;
			}
		});
		return null;
	}

	private void handleInsert(Map<String, AttributeValue> newItem) {
		Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> item = createCommonAuditLogItem(newItem);
		item.put("newValue", createMapAttributeValue(newItem.get("key").getS(), newItem.get("value").getN()));
		dynamoDB.putItem(new PutItemRequest().withTableName("cmtr-7a75be14-Audit-test").withItem(item));
	}

	private void handleUpdate(Map<String, AttributeValue> oldItem, Map<String, AttributeValue> newItem) {
		Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> item = createCommonAuditLogItem(newItem);
		item.put("updatedAttribute", new com.amazonaws.services.dynamodbv2.model.AttributeValue("value"));
		item.put("oldValue", createMapAttributeValue(oldItem.get("key").getS(), oldItem.get("value").getN()));
		item.put("newValue", createMapAttributeValue(newItem.get("key").getS(), newItem.get("value").getN()));
		dynamoDB.putItem(new PutItemRequest().withTableName("cmtr-7a75be14-Audit-test").withItem(item));
	}

	private Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> createCommonAuditLogItem(Map<String, AttributeValue> newItem) {
		Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> item = new HashMap<>();
		item.put("id", new com.amazonaws.services.dynamodbv2.model.AttributeValue(UUID.randomUUID().toString()));
		item.put("itemKey", new com.amazonaws.services.dynamodbv2.model.AttributeValue(newItem.get("key").getS()));
		item.put("modificationTime", new com.amazonaws.services.dynamodbv2.model.AttributeValue(Instant.now().toString()));
		return item;
	}

	private com.amazonaws.services.dynamodbv2.model.AttributeValue createMapAttributeValue(String key, String value) {
		Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> map = new HashMap<>();
		map.put("key", new com.amazonaws.services.dynamodbv2.model.AttributeValue(key));
		map.put("value", new com.amazonaws.services.dynamodbv2.model.AttributeValue().withN(value));
		return new com.amazonaws.services.dynamodbv2.model.AttributeValue().withM(map);
	}
}