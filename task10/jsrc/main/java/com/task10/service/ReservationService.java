package com.task10.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.task10.entity.ReservationEntity;
import com.task10.request.TableRequest;

import java.io.IOException;
import java.util.*;

import static com.task10.utils.Constants.REGION;
import static com.task10.utils.Constants.RESERVATION_TABLE_NAME;

public class ReservationService {

    private AmazonDynamoDB amazonDynamoDB;
    private TableService tableService = new TableService();

    private AmazonDynamoDB getAmazonDynamoDB() {
        if (amazonDynamoDB == null) {
            this.amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
                    .withRegion(REGION)
                    .build();
        }
        return amazonDynamoDB;
    }

    public APIGatewayProxyResponseEvent saveReservation(String requestBody) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ReservationEntity reservationRequest = objectMapper.readValue(requestBody, ReservationEntity.class);


        List<TableRequest> tables = tableService.getAllTables();
        TableRequest tableDTO = tables.stream()
                .filter(t -> t.getNumber() == reservationRequest.getTableNumber())
                .findFirst()
                .orElse(null);

        if (tableDTO == null) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Table does not exist");
        }

        List<ReservationEntity> reservations = getReservations();


        for (ReservationEntity existingReservation : reservations) {
            if (hasOverlap(reservationRequest, existingReservation)) {
                return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Conflicting reservation");
            }
        }
        reservationRequest.setId(UUID.randomUUID().toString());
        DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(getAmazonDynamoDB());
        dynamoDBMapper.save(reservationRequest);

        Gson gson = new Gson();
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("reservationId", reservationRequest.getId());
        return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(gson.toJson(responseMap));
    }

    private boolean hasOverlap(ReservationEntity newReservation, ReservationEntity existingReservation) {
        return newReservation.getTableNumber() == existingReservation.getTableNumber()
                && newReservation.getDate().equals(existingReservation.getDate())
                && (newReservation.getSlotTimeStart().compareTo(existingReservation.getSlotTimeStart()) <= 0
                && newReservation.getSlotTimeEnd().compareTo(existingReservation.getSlotTimeEnd()) >= 0);
    }

    public List<ReservationEntity> getReservations() {
        ScanRequest scanRequest = new ScanRequest().withTableName(RESERVATION_TABLE_NAME);
        ScanResult result = getAmazonDynamoDB().scan(scanRequest);
        List<ReservationEntity> reservations = new ArrayList<>();

        for (Map<String, AttributeValue> item : result.getItems()) {
            ReservationEntity reservationEntity = new ReservationEntity();
            reservationEntity.setId(item.get("id").getN());
            reservationEntity.setTableNumber(Integer.parseInt(item.get("tableNumber").getN()));
            reservationEntity.setClientName(item.get("clientName").getS());
            reservationEntity.setPhoneNumber(item.get("phoneNumber").getS());
            reservationEntity.setDate(item.get("date").getS());
            reservationEntity.setSlotTimeStart(item.get("slotTimeStart").getS());
            reservationEntity.setSlotTimeEnd(item.get("slotTimeEnd").getS());
            reservations.add(reservationEntity);
        }
        return reservations;
    }

    public APIGatewayProxyResponseEvent getAllReservations() {
        ScanRequest scanRequest = new ScanRequest().withTableName(RESERVATION_TABLE_NAME);
        ScanResult result = getAmazonDynamoDB().scan(scanRequest);
        List<ReservationEntity> reservations = new ArrayList<>();

        for (Map<String, AttributeValue> item : result.getItems()) {
            ReservationEntity reservationDTO = new ReservationEntity();
            reservationDTO.setId(item.get("id").getN());
            reservationDTO.setTableNumber(Integer.parseInt(item.get("tableNumber").getN()));
            reservationDTO.setClientName(item.get("clientName").getS());
            reservationDTO.setPhoneNumber(item.get("phoneNumber").getS());
            reservationDTO.setDate(item.get("date").getS());
            reservationDTO.setSlotTimeStart(item.get("slotTimeStart").getS());
            reservationDTO.setSlotTimeEnd(item.get("slotTimeEnd").getS());
            reservations.add(reservationDTO);
        }

        Gson gson = new Gson();
        Map<String, List<ReservationEntity>> responseMap = new HashMap<>();
        responseMap.put("reservations", reservations);

        return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(gson.toJson(responseMap));
    }
}
