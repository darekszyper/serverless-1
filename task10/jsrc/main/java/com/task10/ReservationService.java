package com.task10;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    TableService tableService;

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    public ReservationService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableService = new TableService(dynamoDbClient);
    }

    public String createReservation(Reservation reservation) {
        logger.debug("Entering createReservation with reservation: {}", reservation);

        try {
            // Check if the table exists
            if (!tableExists(reservation.getTableNumber())) {
                String errorMsg = "Table does not exist.";
                logger.error(errorMsg);
                throw new Exception(errorMsg);
            }

            logger.debug("Table exists check passed for tableNumber: {}", reservation.getTableNumber());

            // Check if there is an overlapping reservation
            if (isOverlappingReservation(reservation)) {
                String errorMsg = "The reservation time overlaps with an existing reservation.";
                logger.error(errorMsg);
                throw new Exception(errorMsg);
            }

            logger.debug("No overlapping reservation found for reservation: {}", reservation);

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

            logger.debug("DynamoDB before PutItemRequest: {}", request);

            dynamoDbClient.putItem(request);
            logger.info("Reservation created with ID: {}", reservationId);

            return reservationId;
        } catch (Exception e) {
            logger.error("Error creating reservation", e);
            throw new RuntimeException(e); // Rethrow or handle as needed
        }
    }

    private boolean tableExists(int tableNumber) {
        logger.debug("Checking if table exists for tableNumber: {}", tableNumber);

        try {
            // Fetch all tables
            List<Table> allTables = tableService.getAllTables();

            // Check if any table in the list matches the given table number
            boolean exists = allTables.stream()
                    .anyMatch(table -> table.getNumber() == tableNumber);

            logger.debug("Table exists check result for tableNumber {}: {}", tableNumber, exists);
            return exists;
        } catch (Exception e) {
            logger.error("Error checking if table exists", e);
            throw new RuntimeException(e); // Handle or rethrow the exception as needed
        }

//        GetItemRequest request = GetItemRequest.builder()
//                .tableName(tablesTableName)
//                .key(Map.of("number", AttributeValue.builder().n(String.valueOf(tableNumber)).build()))
//                .build();

//        try {
//            Map<String, AttributeValue> item = dynamoDbClient.getItem(request).item();
//            boolean exists = item != null && !item.isEmpty();
//            logger.debug("Table exists check result for tableNumber {}: {}", tableNumber, exists);
//            return exists;
//        } catch (DynamoDbException e) {
//            logger.error("Error checking if table exists", e);
//            throw new RuntimeException(e); // Rethrow or handle as needed
//        }
    }

    private boolean isOverlappingReservation(Reservation newReservation) {
        logger.debug("Checking for overlapping reservations with newReservation: {}", newReservation);

        List<Reservation> existingReservations = getAllReservations();
        for (Reservation reservation : existingReservations) {
            if (reservation.getTableNumber() == newReservation.getTableNumber() &&
                    reservation.getDate().equals(newReservation.getDate()) &&
                    timeOverlaps(newReservation.getSlotTimeStart(), newReservation.getSlotTimeEnd(),
                            reservation.getSlotTimeStart(), reservation.getSlotTimeEnd())) {
                logger.debug("Overlapping reservation found with existingReservation: {}", reservation);
                return true;
            }
        }
        logger.debug("No overlapping reservations found");
        return false;
    }

    private boolean timeOverlaps(String start1, String end1, String start2, String end2) {
        logger.debug("Checking time overlap: start1={}, end1={}, start2={}, end2={}", start1, end1, start2, end2);
        boolean overlaps = (start1.compareTo(end2) < 0 && end1.compareTo(start2) > 0);
        logger.debug("Time overlap result: {}", overlaps);
        return overlaps;
    }

    public List<Reservation> getAllReservations() {
        logger.debug("Fetching all reservations from table: {}", reservationTableName);

        ScanRequest request = ScanRequest.builder().tableName(reservationTableName).build();

        try {
            ScanResponse response = dynamoDbClient.scan(request);
            logger.debug("DynamoDB ScanResponse: {}", response);

            List<Reservation> reservations = response.items().stream().map(this::mapToReservation).collect(Collectors.toList());
            logger.debug("Fetched reservations: {}", reservations);

            return reservations;
        } catch (DynamoDbException e) {
            logger.error("Error fetching reservations", e);
            throw new RuntimeException(e); // Rethrow or handle as needed
        }
    }

    private Reservation mapToReservation(Map<String, AttributeValue> item) {
        logger.debug("Mapping item to Reservation: {}", item);
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
