package com.task11;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.task11.Constants.TABLES_TABLE_NAME;

public class TableService {

    private static final Logger logger = LoggerFactory.getLogger(TableService.class);

    private final DynamoDbClient dynamoDbClient;
    private final String tableName = TABLES_TABLE_NAME;

    public TableService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public void createTable(Table table) {
        logger.info("Creating table with ID: {}", table.getId());
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(String.valueOf(table.getId())).build());
        item.put("number", AttributeValue.builder().n(String.valueOf(table.getNumber())).build());
        item.put("places", AttributeValue.builder().n(String.valueOf(table.getPlaces())).build());
        item.put("isVip", AttributeValue.builder().bool(table.isVip()).build());

        if (table.getMinOrder() != null) {
            item.put("minOrder", AttributeValue.builder().n(String.valueOf(table.getMinOrder())).build());
        }

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        logger.info("Table with ID: {} created successfully.", table.getId());
    }

    public Table getTable(int tableId) {
        logger.info("Retrieving table with ID: {}", tableId);
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("id", AttributeValue.builder().s(String.valueOf(tableId)).build()))  // Ensure the ID is stored as a string
                .build();

        Map<String, AttributeValue> item = dynamoDbClient.getItem(request).item();

        if (item == null || item.isEmpty()) {
            logger.warn("Table with ID: {} not found.", tableId);
            return null;
        }

        logger.info("Table with ID: {} retrieved successfully.", tableId);
        return mapToTable(item);
    }

    public List<Table> getAllTables() {
        logger.info("Retrieving all tables.");
        ScanRequest request = ScanRequest.builder().tableName(tableName).build();
        List<Map<String, AttributeValue>> items = dynamoDbClient.scan(request).items();

        if (items.isEmpty()) {
            logger.warn("No tables found in the database.");
        } else {
            logger.info("Retrieved {} tables.", items.size());
        }

        return items.stream().map(this::mapToTable).collect(Collectors.toList());
    }

    private Table mapToTable(Map<String, AttributeValue> item) {
        return new Table(
                Integer.parseInt(item.get("id").s()),  // Since we stored 'id' as a string, we retrieve it as a string
                Integer.parseInt(item.get("number").n()),
                Integer.parseInt(item.get("places").n()),
                Boolean.parseBoolean(item.get("isVip").bool().toString()),
                item.get("minOrder") != null ? Integer.parseInt(item.get("minOrder").n()) : null
        );
    }
}
