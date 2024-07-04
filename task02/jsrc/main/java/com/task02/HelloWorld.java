package com.task02;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.RetentionSetting;

import java.util.Collections;

@LambdaHandler(lambdaName = "hello_world",
        roleName = "hello_world-role",
        isPublishVersion = false,
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
//@LambdaUrlConfig - the annotation that enables Lambda URL.
//
//		authType - default value AuthType.NONE
//		invokeMode - default value InvokeMode.BUFFERED
@LambdaUrlConfig
public class HelloWorld implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent request, Context context) {
        String method = request.getRequestContext().getHttp().getMethod();
        String path = request.getRawPath();

        // Payload structure: https://docs.aws.amazon.com/lambda/latest/dg/urls-invocation.html#urls-payloads
        if (isHelloEndpoint(method, path)) {
            return createSuccessResponse();
        } else {
            return createErrorResponse(path, method);
        }
    }

    private boolean isHelloEndpoint(String method, String path) {
        return "GET".equalsIgnoreCase(method) && "/hello".equals(path);
    }

    private APIGatewayV2HTTPResponse createSuccessResponse() {
        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(200)
                .withBody("{\"statusCode\":200, \"message\":\"Hello from Lambda\"}")
                .withHeaders(Collections.singletonMap("Content-Type", "application/json"))
                .build();
    }

    private APIGatewayV2HTTPResponse createErrorResponse(String path, String method) {
        String errorMessage = String.format(
                "{\"statusCode\":400, \"message\":\"Bad request syntax or unsupported method. Request path: %s. HTTP method: %s\"}",
                path, method);

        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(400)
                .withBody(errorMessage)
                .withHeaders(Collections.singletonMap("Content-Type", "application/json"))
                .build();
    }
}
