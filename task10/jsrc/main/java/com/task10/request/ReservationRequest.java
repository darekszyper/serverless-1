package com.task10.request;

import java.util.Map;
import java.util.UUID;

public class ReservationRequest {

    private String id;
    private int tableNumber;
    private String clientName;
    private String phoneNumber;
    private String date;
    private String slotTimeStart;
    private String slotTimeEnd;

    public ReservationRequest() {
    }

    public ReservationRequest(Map<String, Object> body) {
        this.id = UUID.randomUUID().toString();
        this.tableNumber = (int) body.get("tableNumber");
        this.clientName = (String) body.get("clientName");
        this.phoneNumber = (String) body.get("phoneNumber");
        this.date = (String) body.get("date");
        this.slotTimeStart = (String) body.get("slotTimeStart");
        this.slotTimeEnd = (String) body.get("slotTimeEnd");
    }



    public void setTableNumber(int tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setSlotTimeStart(String slotTimeStart) {
        this.slotTimeStart = slotTimeStart;
    }

    public void setSlotTimeEnd(String slotTimeEnd) {
        this.slotTimeEnd = slotTimeEnd;
    }
}
