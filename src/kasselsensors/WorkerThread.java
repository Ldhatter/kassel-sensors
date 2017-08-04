package kasselsensors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.UriBuilder;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

public class WorkerThread extends Thread {

	private final static String QUEUE_NAME = "sensorData";

	private String sensorId;
	private String location;
	private String coordinates;
	private MongoDatabase db;
	private JSONObject sensor;
	private boolean hasResults;
	private boolean isCancelled;

	public WorkerThread(String sensorId, String location, double lat, double lng, MongoDatabase db) {
		this.sensorId = sensorId;
		this.location = location;
		this.coordinates = getCoordinates(lat, lng);
		this.db = db;
		this.hasResults = false;
		this.isCancelled = false;
	}

	@Override
	public void run() {

		while (!isCancelled) {

			// Get sensor data
			JSONObject data = getSensorData();

			// if (hasResults) {
			// Store data to DB
			updateDB(data);

			// Publish data
			publishData(data);
			// }

		}

		System.out.println("done");

	}

	private JSONObject getSensorData() {

		JSONObject result = new JSONObject();

		UriBuilder builder = UriBuilder.fromPath("//172.16.40.118:8080").scheme("http")
				.path("/kasselpi/kpi/tempservice");
		String urlString = builder.build().toString();

		try {
			URL url = new URL(urlString);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");

			// int responseCode = con.getResponseCode();
			// System.out.println(String.valueOf(responseCode));

			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			StringBuffer response = new StringBuffer();
			String data;

			while ((data = reader.readLine()) != null) {
				response.append(data);
			}

			result = new JSONObject(response.toString());

		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	private void updateDB(JSONObject result) {

		// Access to collection
		MongoCollection<Document> sensorData = db.getCollection("pmData");

		// JSONArray results = new JSONArray(data.toString());

		List<Document> documents = new ArrayList<Document>();

		// Get result data
		JSONObject jAverage = result.getJSONObject("average");
		JSONObject jMedian = result.getJSONObject("median");
		JSONObject jMin = result.getJSONObject("min");
		JSONObject jMax = result.getJSONObject("max");
		JSONObject jStd = result.getJSONObject("sd");
		long date = result.getLong("time");
		Document average = new Document("time", jAverage.getLong("time")).append("pm", jAverage.getDouble("pm"));
		Document median = new Document("time", jMedian.getLong("time")).append("pm", jMedian.getDouble("pm"));
		Document min = new Document("time", jMin.getLong("time")).append("pm", jMin.getDouble("pm"));
		Document max = new Document("time", jMax.getLong("time")).append("pm", jMax.getDouble("pm"));
		Document std = new Document("time", jStd.getLong("time")).append("value", jStd.getDouble("value"));

		// Make Document
		Document dataDocument = new Document("sensorId", sensorId).append("date", date).append("average", average)
				.append("median", median).append("min", min).append("max", max).append("std", std);

		documents.add(dataDocument);

		// Add sensor data to db
		sensorData.insertMany(documents);

		System.out.println("inserted?");
	}

	private void getSensor() {

		// Access to collection
		MongoCollection<Document> sensors = db.getCollection("raspi");

		Document sensorDoc = sensors.find(new Document("location", location)).first();
		sensor = new JSONObject(sensorDoc.toJson());

	}

	private void setSensorDate() {

		// Access to collection
		MongoCollection<Document> sensors = db.getCollection("sensors");

		sensors.updateOne(Filters.eq("_id", new ObjectId(sensorId)), Updates.currentDate("lastModified"));

	}

	private Instant getSensorDate() {

		JSONObject updatedDB = new JSONObject(sensor.get("lastModified").toString());
		Instant updatedUnix = Instant.ofEpochMilli((long) updatedDB.get("$date"));
		Instant updatedUTC = Instant.parse(updatedUnix.toString());

		return updatedUTC;

	}

	private String getCoordinates(double lat, double lng) {

		String coords = String.valueOf(lat) + ',' + String.valueOf(lng);
		return coords;

	}

	private void publishData(JSONObject data) {

		JSONObject results = new JSONObject(data, new String[] { "results" });

		try {
			// Connecting to a broker
			ConnectionFactory factory = new ConnectionFactory();
			String uri = System.getenv("CLOUDAMQP_URL");
			if (uri == null)
				uri = "amqp://guest:guest@localhost";
			factory.setUri(uri);
			Connection connection = factory.newConnection();

			// Open a channel
			Channel channel = connection.createChannel();
			channel.queueDeclare(QUEUE_NAME, false, false, false, null);

			// Set message
			String message = results.toString();
			System.out.println(message);

			// Publish data
			channel.basicPublish("", QUEUE_NAME, null, message.getBytes("UTF-8"));
			System.out.println(" [x] Sent '" + message + "'");

			channel.close();
			connection.close();

		} catch (IOException | TimeoutException | KeyManagementException | NoSuchAlgorithmException
				| URISyntaxException e) {
			e.printStackTrace();
		}
	}

}
