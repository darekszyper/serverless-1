package com.task10;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;
import com.task10.service.ReservationService;
import com.task10.service.SignInService;
import com.task10.service.SignUpService;
import com.task10.service.TableService;

import java.io.IOException;


@LambdaHandler(lambdaName = "api_handler",
        roleName = "api_handler-role",
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)

public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SignInService signInService;
    private final SignUpService signUpService;
    private final TableService tableService;
    private final ReservationService reservationService;

    public ApiHandler() {
        this.signInService = new SignInService();
        this.signUpService = new SignUpService();
        this.tableService = new TableService();
        this.reservationService = new ReservationService();
    }

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {

        String path = apiGatewayProxyRequestEvent.getPath();
        String httpMethod = apiGatewayProxyRequestEvent.getHttpMethod();

        if (path.equals("/signup") && httpMethod.equals("POST")) {
            return signUpService.handleSignUp(apiGatewayProxyRequestEvent);
        } else if (path.equals("/signin") && httpMethod.equals("POST")) {
            return signInService.handleSignIn(apiGatewayProxyRequestEvent);
        } else if (path.equals("/tables") && httpMethod.equals("GET")) {
            return tableService.getTables();
        } else if (path.equals("/tables") && httpMethod.equals("POST")) {
            try {
                return tableService.saveTable(apiGatewayProxyRequestEvent.getBody());
            } catch (IOException e) {
                return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.toString());
            }
        } else if (path.matches("/tables/\\d+") && httpMethod.equals("GET")) {
            int tableId = Integer.parseInt(apiGatewayProxyRequestEvent.getPathParameters().get("tableId"));
            try {
                return tableService.getTablesById(tableId);
            } catch (Exception e) {
                return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.toString());
            }
        } else if (path.equals("/reservations") && httpMethod.equals("POST")) {
            try {
                return reservationService.saveReservation(apiGatewayProxyRequestEvent.getBody());
            } catch (IOException e) {
                return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.toString());
            }
        } else if (path.equals("/reservations") && httpMethod.equals("GET")) {
            try {
                return reservationService.getAllReservations();
            } catch (Exception e) {
                return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody(e.toString());
            }
        } else {
            return new APIGatewayProxyResponseEvent().withStatusCode(404).withBody("Error request");
        }
    }
}
