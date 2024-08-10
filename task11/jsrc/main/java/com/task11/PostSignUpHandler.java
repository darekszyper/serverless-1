package com.task11;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

public class PostSignUpHandler extends CognitoSupport implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LoggerFactory.getLogger(PostSignUpHandler.class);

    public PostSignUpHandler(CognitoIdentityProviderClient cognitoClient) {
        super(cognitoClient);
        logger.info("23253253 - PostSignUpHandler initialized.");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        logger.info("23253253 - Handling signup request with body: {}", requestEvent.getBody());

        try {
            SignUp signUp = SignUp.fromJson(requestEvent.getBody());
            logger.debug("23253253 - Parsed signup data: {}", signUp);

            // Sign up
            String userId = cognitoSignUp(signUp)
                    .user().attributes().stream()
                    .filter(attr -> "sub".equals(attr.name()))
                    .map(AttributeType::value)
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("Sub not found."));
            logger.info("23253253 - User signup successful for email: {}, UserID: {}", signUp.email(), userId);

            // Confirm signup
            String idToken = confirmSignUp(signUp)
                    .authenticationResult()
                    .idToken();
            logger.info("23253253 - User confirmed signup and obtained token for email: {}", signUp.email());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(new JSONObject()
                            .put("message", "User has been successfully signed up.")
                            .put("userId", userId)
                            .put("accessToken", idToken)
                            .toString());
        } catch (Exception e) {
            logger.error("23253253 - Signup failed for email: {}, error: {}", requestEvent.getBody(), e.getMessage(), e);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(new JSONObject().put("error", e.getMessage()).toString());
        }
    }
}
