package com.task10.request;

import java.util.Map;

public class SignInRequest {

    private final String email;
    private final String password;

    public SignInRequest(Map<String, String> body) {
        this.email = body.get("email");
        this.password = body.get("password");
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
