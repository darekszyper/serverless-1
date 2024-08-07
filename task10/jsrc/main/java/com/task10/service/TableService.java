package com.task10.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.task10.entity.TableEntity;
import com.task10.request.TableRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.task10.utils.Constants.REGION;
import static com.task10.utils.Constants.TABLES_TABLE_NAME;


public class TableService {
    private AmazonDynamoDB amazonDynamoDB;

    private AmazonDynamoDB getAmazonDynamoDB() {
        if (amazonDynamoDB == null) {
            this.amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
                    .withRegion(REGION)
                    .build();
        }
        return amazonDynamoDB;
    }

    public APIGatewayProxyResponseEvent getTables() {
        ScanRequest scanRequest = new ScanRequest().withTableName(TABLES_TABLE_NAME);
        ScanResult result = getAmazonDynamoDB().scan(scanRequest);
        List<TableRequest> tables = new ArrayList<>();

        for (Map<String, AttributeValue> item : result.getItems()) {
            TableRequest tableRequest = new TableRequest();
            tableRequest.setId(Integer.parseInt(item.get("id").getN()));
            tableRequest.setNumber(Integer.parseInt(item.get("number").getN()));
            tableRequest.setPlaces(Integer.parseInt(item.get("places").getN()));
            tableRequest.setVip(Boolean.parseBoolean(String.valueOf(item.get("isVip").getBOOL())));
            if (item.containsKey("minOrder")) {
                tableRequest.setMinOrder(Integer.parseInt(item.get("minOrder").getN()));
            }
            tables.add(tableRequest);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(tables);
        return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(json);
    }

    public APIGatewayProxyResponseEvent saveTable(String requestBody) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        TableRequest table = objectMapper.readValue(requestBody, TableRequest.class);

        TableEntity savedTable = new TableEntity();
        savedTable.setId(table.getId());
        savedTable.setNumber(table.getNumber());
        savedTable.setVip(table.isVip());
        savedTable.setPlaces(table.getPlaces());
        savedTable.setMinOrder(table.getMinOrder());
        DynamoDBMapper dbMapper = new DynamoDBMapper(getAmazonDynamoDB());
        try {
            dbMapper.save(savedTable);
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("{\"id\":" + table.getId() + "}");
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400);
        }
    }

    public APIGatewayProxyResponseEvent getTablesById(int id) {
        Gson gson = new Gson();
        try {
            TableEntity response = getTableById(id);
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(gson.toJson(response));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400);
        }
    }

    public TableEntity getTableById(int tableId) throws Exception {
        DynamoDB dynamoDB = new DynamoDB(getAmazonDynamoDB());
        com.amazonaws.services.dynamodbv2.document.Table dynamoTable = dynamoDB.getTable(TABLES_TABLE_NAME);

        GetItemSpec getItemSpec = new GetItemSpec().withPrimaryKey("id", tableId);
        Item item = dynamoTable.getItem(getItemSpec);
        if (item == null) {
            throw new Exception("Table not found");
        }
        TableEntity table = new TableEntity();
        table.setId(item.getInt("id"));
        table.setNumber(item.getInt("number"));
        table.setPlaces(item.getInt("places"));
        table.setVip(item.getNumber("isVip").equals(BigDecimal.ONE));
        if (item.isPresent("minOrder")) {
            table.setMinOrder(item.getInt("minOrder"));
        }
        return table;
    }

    public List<TableRequest> getAllTables() {
        ScanRequest scanRequest = new ScanRequest().withTableName(TABLES_TABLE_NAME);
        ScanResult result = getAmazonDynamoDB().scan(scanRequest);
        List<TableRequest> tables = new ArrayList<>();
        for (Map<String, AttributeValue> item : result.getItems()) {
            TableRequest tableRequest = new TableRequest();
            tableRequest.setId(Integer.parseInt(item.get("id").getN()));
            tableRequest.setNumber(Integer.parseInt(item.get("number").getN()));
            tableRequest.setPlaces(Integer.parseInt(item.get("places").getN()));
            tableRequest.setVip(Boolean.parseBoolean(String.valueOf(item.get("isVip").getBOOL())));
            if (item.containsKey("minOrder")) {
                tableRequest.setMinOrder(Integer.parseInt(item.get("minOrder").getN()));
            }
            tables.add(tableRequest);
        }
        return tables;

    }
}
