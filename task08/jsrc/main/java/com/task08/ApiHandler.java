package com.task08;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.google.gson.Gson;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@LambdaHandler(
		lambdaName = "api_handler",
		roleName = "api_handler-role",
		layers = {"sdk-layer"}
)
@LambdaLayer(
		layerName = "sdk-layer",
		libraries = {"lib/gson-2.10.1.jar"},
		runtime = DeploymentRuntime.JAVA11,
		artifactExtension = ArtifactExtension.ZIP
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, String> {

	private static final String WEATHER_API_URL = "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&current=temperature_2m,wind_speed_10m&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m";

	@Override
	public String handleRequest(APIGatewayProxyRequestEvent request, Context context) {
		Gson gson = new Gson();
		try {
			return fetchWeatherData();
		} catch (IOException e) {
			context.getLogger().log("Error fetching weather data: " + e.getMessage());
			return buildErrorResponse(e);
		}
	}

	private String fetchWeatherData() throws IOException {
		HttpURLConnection connection = createConnection(WEATHER_API_URL);
		int responseCode = connection.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK) {
			return readResponse(connection);
		} else {
			throw new IOException("HTTP response code: " + responseCode);
		}
	}

	private HttpURLConnection createConnection(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		return connection;
	}

	private String readResponse(HttpURLConnection connection) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			StringBuilder responseBuilder = new StringBuilder();
			reader.lines().forEach(responseBuilder::append);
			return responseBuilder.toString();
		}
	}

	private String buildErrorResponse(Exception e) {
		Map<String, String> errorResponse = new HashMap<>();
		errorResponse.put("error", e.getMessage());
		return new Gson().toJson(errorResponse);
	}
}
