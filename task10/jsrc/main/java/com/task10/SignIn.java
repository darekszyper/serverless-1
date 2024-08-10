package com.task10;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record SignIn(String email, String password) {

    private static final Logger logger = LoggerFactory.getLogger(SignIn.class);

    public SignIn {
        logger.debug("2343254543543545535\nValidating SignIn data: email={}, password={}", email, (password != null ? "[PROVIDED]" : "null"));
        if (email == null || password == null) {
            logger.error("2343254543543545535\nInvalid SignIn data: email or password is null.");
            throw new IllegalArgumentException("Missing or incomplete data.");
        }
    }

    public static SignIn fromJson(String jsonString) {
        logger.info("2343254543543545535\nParsing SignIn data from JSON.");
        JSONObject json = new JSONObject(jsonString);
        String email = json.optString("email", null);
        String password = json.optString("password", null);

        SignIn signIn = new SignIn(email, password);
        logger.debug("2343254543543545535\nParsed SignIn object: {}", signIn);
        return signIn;
    }
}
