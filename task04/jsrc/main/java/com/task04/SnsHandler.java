package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;
import com.syndicate.deployment.annotations.events.SnsEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

//@SnsEventSource - the annotation which allows to subscribe a Lambda to an SNS topic:
//
//targetTopic* - the name of an SNS topic to which the Lambda function is subscribed.
//regionScope – the region or regions in which the SNS topic is located. The default value
//RegionScope.DEFAULT means that the Lambda is deployed only to the SNS topic in the regions specified in the config during deploy.
@LambdaHandler(
        lambdaName = "sns_handler",
        roleName = "sns_handler-role",
        isPublishVersion = false,
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@SnsEventSource(
        targetTopic = "lambda_topic"
)
public class SnsHandler implements RequestHandler<SNSEvent, Void> {

    @Override
    public Void handleRequest(SNSEvent snsEvent, Context context) {
        for (SNSRecord msg : snsEvent.getRecords()) {
            sendMessageToCloudWatchLogs(msg, context);
        }
        return null;
    }

    private void sendMessageToCloudWatchLogs(SNSRecord msg, Context context) {
        try {
            String message = msg.getSNS().getMessage();
            context.getLogger().log(message);
        } catch (Exception e) {
            context.getLogger().log("An error occurred");
            throw e;
        }
    }
}
