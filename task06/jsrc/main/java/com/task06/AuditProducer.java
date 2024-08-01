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

	private final AmazonDynamoDB ddb;

	public AuditProducer() {
		this.ddb = AmazonDynamoDBClientBuilder.defaultClient();
	}

	@Override
	public Void handleRequest(DynamodbEvent ddbEvent, Context context) {
		ddbEvent.getRecords().forEach(record -> {
			switch (record.getEventName()) {
				case "INSERT":
					handleInsert(record.getDynamodb().getNewImage());
					break;
				case "MODIFY":
					handleUpdate(record.getDynamodb().getOldImage(), record.getDynamodb().getNewImage());
					break;
				default:
					break;
			}
		});
		return null;
	}

	private void handleInsert(Map<String, AttributeValue> newItem) {
		Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> item = createCommonAuditLogItem(newItem);
		item.put("newValue", new com.amazonaws.services.dynamodbv2.model.AttributeValue(newItem.get("value").getN()));
		ddb.putItem(new PutItemRequest().withTableName("cmtr-7a75be14-Audit-test").withItem(item));
	}

	private void handleUpdate(Map<String, AttributeValue> oldItem, Map<String, AttributeValue> newItem) {
		Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> item = createCommonAuditLogItem(newItem);
		item.put("updatedAttribute", new com.amazonaws.services.dynamodbv2.model.AttributeValue("value"));
		item.put("oldValue", new com.amazonaws.services.dynamodbv2.model.AttributeValue(oldItem.get("value").getN()));
		item.put("newValue", new com.amazonaws.services.dynamodbv2.model.AttributeValue(newItem.get("value").getN()));
		ddb.putItem(new PutItemRequest().withTableName("cmtr-7a75be14-Audit-test").withItem(item));
	}

	private Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> createCommonAuditLogItem(Map<String, AttributeValue> newItem) {
		Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> item = new HashMap<>();
		item.put("id", new com.amazonaws.services.dynamodbv2.model.AttributeValue(UUID.randomUUID().toString()));
		item.put("itemKey", new com.amazonaws.services.dynamodbv2.model.AttributeValue(newItem.get("key").getS()));
		item.put("modificationTime", new com.amazonaws.services.dynamodbv2.model.AttributeValue(Instant.now().toString()));
		return item;
	}
}