package com.task11;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

public class PostSignInHandler extends CognitoSupport implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LoggerFactory.getLogger(PostSignInHandler.class);

    public PostSignInHandler(CognitoIdentityProviderClient cognitoClient) {
        super(cognitoClient);
        logger.info("2343254543543545535\nPostSignInHandler initialized.");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        logger.info("2343254543543545535\nHandling sign-in request with body: {}", requestEvent.getBody());

        try {
            SignIn signIn = SignIn.fromJson(requestEvent.getBody());
            logger.debug("2343254543543545535\nParsed sign-in data: {}", signIn);

            String accessToken = cognitoSignIn(signIn.email(), signIn.password())
                    .authenticationResult()
                    .idToken();

            logger.info("2343254543543545535\nSign-in successful for email: {} accessToken: {}", signIn.email(), accessToken);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(new JSONObject().put("accessToken", accessToken).toString());
        } catch (Exception e) {
            logger.error("2343254543543545535\nSign-in failed: {}", e.getMessage(), e);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(new JSONObject().put("error", e.getMessage()).toString());
        }
    }
}
