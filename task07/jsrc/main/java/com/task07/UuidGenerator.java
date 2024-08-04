package com.task07;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.syndicate.deployment.annotations.events.RuleEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@LambdaHandler(lambdaName = "uuid_generator",
		roleName = "uuid_generator-role",
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@RuleEventSource(targetRule = "uuid_trigger")
public class UuidGenerator implements RequestHandler<Object, String> {

	private static final int UUID_COUNT = 10;
	private static final String S3_BUCKET_NAME = "cmtr-7a75be14-uuid-storage";
	private static final Regions AWS_REGION = Regions.EU_CENTRAL_1;

	@Override
	public String handleRequest(Object input, Context context) {
		context.getLogger().log("Generating file with UUIDs and saving to S3...");

		// Generate UUIDs
		List<String> uuidList = new ArrayList<>();
		for (int i = 0; i < UUID_COUNT; i++) {
			uuidList.add(UUID.randomUUID().toString());
		}

		// Create file name
		LocalDateTime currentTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		String fileName = currentTime.format(formatter);

		// Create file content
		JsonObject jsonObject = new JsonObject();
		JsonArray jsonArray = new JsonArray();

		uuidList.forEach(jsonArray::add);
		jsonObject.add("ids", jsonArray);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String fileContent = gson.toJson(jsonObject);

		// Upload file to S3
		AmazonS3 s3Client = new AmazonS3Client();
		s3Client.setRegion(Region.getRegion(AWS_REGION));

		InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(fileContent.getBytes().length);

		s3Client.putObject(S3_BUCKET_NAME, fileName, inputStream, metadata);

		return "File saved to S3: " + fileName;
	}
}
