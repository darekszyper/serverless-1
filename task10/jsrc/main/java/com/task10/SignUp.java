package com.task10;

import org.json.JSONObject;

/**
 * Created by Roman Ivanov on 7/20/2024.
 */
public record SignUp(String email, String password, String firstName, String lastName, String nickName) {

    public SignUp {
        if (email == null || password == null || firstName == null || lastName == null || nickName == null) {
            throw new IllegalArgumentException("Missing or incomplete data.");
        }
    }

    public static SignUp fromJson(String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        String email = json.optString("email", null);
        String password = json.optString("password", null);
        String firstName = json.optString("firstName", null);
        String lastName = json.optString("lastName", null);
        String nickName = json.optString("nickName", null);

        return new SignUp(email, password, firstName, lastName, nickName);
    }

}