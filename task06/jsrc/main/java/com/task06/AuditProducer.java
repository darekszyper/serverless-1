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

	private final AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClientBuilder.defaultClient();
	private static final String AUDIT_TABLE_NAME = "cmtr-7a75be14-Audit-test";

	@Override
	public Void handleRequest(DynamodbEvent dynamoDbEvent, Context context) {
		for (DynamodbEvent.DynamodbStreamRecord record : dynamoDbEvent.getRecords()) {
			if ("INSERT".equals(record.getEventName())) {
				Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();
				createAuditLogForInsert(newImage);
			} else if ("MODIFY".equals(record.getEventName())) {
				Map<String, AttributeValue> oldImage = record.getDynamodb().getOldImage();
				Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();
				createAuditLogForUpdate(oldImage, newImage);
			}
		}
		return null;
	}

	private void createAuditLogForInsert(Map<String, AttributeValue> newImage) {
		Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> auditItem = new HashMap<>();
		auditItem.put("id", new com.amazonaws.services.dynamodbv2.model.AttributeValue(UUID.randomUUID().toString()));
		auditItem.put("itemKey", new com.amazonaws.services.dynamodbv2.model.AttributeValue(newImage.get("key").getS()));
		auditItem.put("modificationTime", new com.amazonaws.services.dynamodbv2.model.AttributeValue(Instant.now().toString()));
		auditItem.put("newValue", new com.amazonaws.services.dynamodbv2.model.AttributeValue(newImage.get("value").getN()));

		putItemInAuditTable(auditItem);
	}

	private void createAuditLogForUpdate(Map<String, AttributeValue> oldImage, Map<String, AttributeValue> newImage) {
		Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> auditItem = new HashMap<>();
		auditItem.put("id", new com.amazonaws.services.dynamodbv2.model.AttributeValue(UUID.randomUUID().toString()));
		auditItem.put("itemKey", new com.amazonaws.services.dynamodbv2.model.AttributeValue(newImage.get("key").getS()));
		auditItem.put("modificationTime", new com.amazonaws.services.dynamodbv2.model.AttributeValue(Instant.now().toString()));
		auditItem.put("updatedAttribute", new com.amazonaws.services.dynamodbv2.model.AttributeValue("value"));
		auditItem.put("oldValue", new com.amazonaws.services.dynamodbv2.model.AttributeValue(oldImage.get("value").getN()));
		auditItem.put("newValue", new com.amazonaws.services.dynamodbv2.model.AttributeValue(newImage.get("value").getN()));

		putItemInAuditTable(auditItem);
	}

	private void putItemInAuditTable(Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> auditItem) {
		dynamoDBClient.putItem(new PutItemRequest().withTableName(AUDIT_TABLE_NAME).withItem(auditItem));
	}
}