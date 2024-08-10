package com.task10;// ReservationService.java
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.task10.Constants.RESERVATION_TABLE_NAME;

public class ReservationService {

    private final DynamoDbClient dynamoDbClient;
    private final String reservationTableName = RESERVATION_TABLE_NAME;

    public ReservationService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public String createReservation(Reservation reservation) {
        String reservationId = UUID.randomUUID().toString();
        Map<String, AttributeValue> item = Map.of(
                "id", AttributeValue.builder().s(reservationId).build(),
                "tableNumber", AttributeValue.builder().n(String.valueOf(reservation.getTableNumber())).build(),
                "clientName", AttributeValue.builder().s(reservation.getClientName()).build(),
                "phoneNumber", AttributeValue.builder().s(reservation.getPhoneNumber()).build(),
                "date", AttributeValue.builder().s(reservation.getDate()).build(),
                "slotTimeStart", AttributeValue.builder().s(reservation.getSlotTimeStart()).build(),
                "slotTimeEnd", AttributeValue.builder().s(reservation.getSlotTimeEnd()).build()
        );

        PutItemRequest request = PutItemRequest.builder()
                .tableName(reservationTableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        return reservationId;
    }

    public List<Reservation> getAllReservations() {
        ScanRequest request = ScanRequest.builder().tableName(reservationTableName).build();
        return dynamoDbClient.scan(request).items().stream().map(this::mapToReservation).collect(Collectors.toList());
    }

    private Reservation mapToReservation(Map<String, AttributeValue> item) {
        return new Reservation(
                Integer.parseInt(item.get("tableNumber").n()),
                item.get("clientName").s(),
                item.get("phoneNumber").s(),
                item.get("date").s(),
                item.get("slotTimeStart").s(),
                item.get("slotTimeEnd").s()
        );
    }
}
