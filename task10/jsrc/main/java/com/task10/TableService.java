package com.task10;// TableService.java

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.task10.Constants.TABLES_TABLE_NAME;

public class TableService {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName = TABLES_TABLE_NAME;

    public TableService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public void createTable(Table table) {
        Map<String, AttributeValue> item = Map.of(
                "id", AttributeValue.builder().s(String.valueOf(table.getId())).build(),  // Use .s() to store as string
                "number", AttributeValue.builder().n(String.valueOf(table.getNumber())).build(),
                "places", AttributeValue.builder().n(String.valueOf(table.getPlaces())).build(),
                "isVip", AttributeValue.builder().bool(table.isVip()).build(),
                // Dynamically include "minOrder" only if it's not null
                "minOrder", table.getMinOrder() != null ? AttributeValue.builder().n(String.valueOf(table.getMinOrder())).build() : null
        );

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }


    public Table getTable(int tableId) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("id", AttributeValue.builder().n(String.valueOf(tableId)).build()))
                .build();

        Map<String, AttributeValue> item = dynamoDbClient.getItem(request).item();
        return mapToTable(item);
    }

    public List<Table> getAllTables() {
        ScanRequest request = ScanRequest.builder().tableName(tableName).build();
        return dynamoDbClient.scan(request).items().stream().map(this::mapToTable).collect(Collectors.toList());
    }

    private Table mapToTable(Map<String, AttributeValue> item) {
        return new Table(
                Integer.parseInt(item.get("id").n()),
                Integer.parseInt(item.get("number").n()),
                Integer.parseInt(item.get("places").n()),
                Boolean.parseBoolean(item.get("isVip").bool().toString()),
                item.get("minOrder") != null ? Integer.parseInt(item.get("minOrder").n()) : null
        );
    }
}
