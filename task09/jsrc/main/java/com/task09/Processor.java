package com.task09;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.TracingMode;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

@LambdaHandler(lambdaName = "processor", roleName = "processor-role", tracingMode = TracingMode.Active)
@LambdaUrlConfig(authType = AuthType.NONE, invokeMode = InvokeMode.BUFFERED)
public class Processor implements RequestHandler<APIGatewayProxyRequestEvent, String> {

	private final DynamoDB dynamoDatabase;

	public Processor() {
		AmazonDynamoDB amazonDynamoDBClient = new AmazonDynamoDBClient();
		amazonDynamoDBClient.setRegion(Region.getRegion(Regions.EU_CENTRAL_1));
		dynamoDatabase = new DynamoDB(amazonDynamoDBClient);
	}

	@Override
	public String handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
		Table weatherTable = dynamoDatabase.getTable("cmtr-7a75be14-Weather-test");
		String weatherData = fetchWeatherData();

		if (!weatherData.equals("")) {
			saveWeatherData(weatherTable, weatherData);
			return weatherData;
		}
		return "";
	}

	private String fetchWeatherData() {
		try {
			URL weatherApiUrl = new URL("https://api.open-meteo.com/v1/forecast?latitude=50.4547&longitude=30.5238&hourly=temperature_2m&timezone=Europe%2FKyiv");
			HttpURLConnection connection = (HttpURLConnection) weatherApiUrl.openConnection();
			connection.setRequestMethod("GET");

			int statusCode = connection.getResponseCode();
			if (statusCode == HttpURLConnection.HTTP_OK) {
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				StringBuilder responseBuilder = new StringBuilder();

				bufferedReader.lines().forEach(responseBuilder::append);
				bufferedReader.close();

				return responseBuilder.toString();
			}
		} catch (IOException ioException) {
			throw new RuntimeException(ioException);
		}
		return "";
	}

	private void saveWeatherData(Table weatherTable, String weatherData) {
		String uniqueId = UUID.randomUUID().toString();

		Item weatherItem = new Item()
				.withPrimaryKey("id", uniqueId)
				.withJSON("forecast", weatherData);

		weatherTable.putItem(weatherItem);
	}
}
