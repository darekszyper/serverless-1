package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.syndicate.deployment.annotations.events.SqsTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

//@SqsTriggerEventSource - the annotation that allows to subscribe the Lambda to an SQS queue:
//
//targetQueue* – the name of the SQS queue to which the Lambda is subscribed.
//batchSize* – the number of records passed used at the Lambda event.
//functionResponseTypes* - a list of current response type enums applied to the event source mapping. The default value is an empty list. Available response types: FunctionResponseType.REPORT_BATCH_ITEM_FAILURES.

@LambdaHandler(
		lambdaName = "sqs_handler",
        roleName = "sqs_handler-role",
        isPublishVersion = false,
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@SqsTriggerEventSource(
		targetQueue = "async_queue",
        batchSize = 1
)
public class SqsHandler implements RequestHandler<SQSEvent, Void> {

	@Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
		for (SQSMessage msg : sqsEvent.getRecords()) {
			sendMessageToCloudWatchLogs(msg, context);
		}
		return null;
    }

	private void sendMessageToCloudWatchLogs(SQSEvent.SQSMessage msg, Context context) {
		try {
			context.getLogger().log(msg.getBody());
		} catch (Exception e) {
			context.getLogger().log("An error occurred");
			throw e;
		}
	}
}
