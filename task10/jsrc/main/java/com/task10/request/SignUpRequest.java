package com.task10.request;

import java.util.Map;

public class SignUpRequest {

    private final String firstName;
    private final String lastName;
    private final String email;
    private final String password;

    public SignUpRequest(Map<String, String> body) {
        this.firstName = body.get("firstName");
        this.lastName = body.get("lastName");
        this.email = body.get("email");
        this.password = body.get("password");
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
