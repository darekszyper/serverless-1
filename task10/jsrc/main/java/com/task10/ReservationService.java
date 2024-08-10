package com.task10;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.task10.Constants.RESERVATION_TABLE_NAME;
import static com.task10.Constants.TABLES_TABLE_NAME;

public class ReservationService {

    private final DynamoDbClient dynamoDbClient;
    private final String reservationTableName = RESERVATION_TABLE_NAME;
    private final String tablesTableName = TABLES_TABLE_NAME;

    public ReservationService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public String createReservation(Reservation reservation) throws Exception {
        // Check if the table exists
        if (!tableExists(reservation.getTableNumber())) {
            throw new Exception("Table does not exist.");
        }

        // Check if there is an overlapping reservation
        if (isOverlappingReservation(reservation)) {
            throw new Exception("The reservation time overlaps with an existing reservation.");
        }

        // Proceed with creating the reservation if all checks pass
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

    private boolean tableExists(int tableNumber) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tablesTableName)
                .key(Map.of("number", AttributeValue.builder().s(String.valueOf(tableNumber)).build()))  // Ensure the ID is stored as a string
                .build();

        Map<String, AttributeValue> item = dynamoDbClient.getItem(request).item();
        return item != null && !item.isEmpty();
    }

    private boolean isOverlappingReservation(Reservation newReservation) {
        List<Reservation> existingReservations = getAllReservations();
        for (Reservation reservation : existingReservations) {
            if (reservation.getTableNumber() == newReservation.getTableNumber() &&
                    reservation.getDate().equals(newReservation.getDate()) &&
                    timeOverlaps(newReservation.getSlotTimeStart(), newReservation.getSlotTimeEnd(),
                            reservation.getSlotTimeStart(), reservation.getSlotTimeEnd())) {
                return true;
            }
        }
        return false;
    }

    private boolean timeOverlaps(String start1, String end1, String start2, String end2) {
        return (start1.compareTo(end2) < 0 && end1.compareTo(start2) > 0);
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
