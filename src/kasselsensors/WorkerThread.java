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

public class WorkerThread implements Runnable {

	private final static String QUEUE_NAME = "sensorData";

	private String location;
	private String coordinates;
	private MongoDatabase db;
	private JSONObject sensor;
	private boolean hasResults;
	private boolean isCancelled;
	
	public WorkerThread(String location, double lat, double lng, MongoDatabase db) {
		this.location = location;
		this.coordinates = getCoordinates(lat, lng);
		this.db = db;
		this.hasResults = false;
		this.isCancelled = true;
	}

	@Override
	public void run() {

		// TODO: used for debugging
		//int i = 50;

		while (!isCancelled) {
			// Get sensor
			getSensor();

			// Get sensor data
			JSONObject data = getSensorData();

			if (hasResults) {
				// Store data to DB
				updateDB(data);

				// Publish data
				publishData(data);
			}
		}

		//System.out.println("done");

	}

	private JSONObject getSensorData() {

		Instant lastUpdated = getSensorDate();

		JSONObject result = new JSONObject();

		UriBuilder builder = UriBuilder.fromPath("//api.openaq.org").scheme("https").path("/v1/measurements")
				.queryParam("location", location).queryParam("coordinates", coordinates)
				.queryParam("date_from", lastUpdated);
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

		JSONObject meta = (JSONObject) result.get("meta");
		hasResults = (meta.getInt("found") > 0) ? true : false;

		return result;
	}

	private void updateDB(JSONObject data) {

		// Access to collection
		MongoCollection<Document> sensorData = db.getCollection("sensorData");

		JSONArray results = new JSONArray(data.get("results").toString());

		List<Document> documents = new ArrayList<Document>();

		for (int i = 0; i < results.length(); i++) {

			// Get result data 
			JSONObject result = results.getJSONObject(i);
			JSONObject date = result.getJSONObject("date");
			String utc = date.getString("utc");
			String unit = result.getString("unit");
			double value = result.getDouble("value");

			// Make Document
			Document dataDocument = new Document("sensorId", getSensorId()).append("updated", utc).append("unit", unit)
					.append("value", value);

			documents.add(dataDocument);
		}

		// Add sensor data to db
		sensorData.insertMany(documents);

		// Update sensor's lastModified date
		setSensorDate();
	}

	private void getSensor() {

		// Access to collection
		MongoCollection<Document> sensors = db.getCollection("sensors");

		Document sensorDoc = sensors.find(new Document("location", location)).first();
		sensor = new JSONObject(sensorDoc.toJson());

	}

	private String getSensorId() {

		String id = (new JSONObject(sensor.get("_id").toString())).getString("$oid");
		return id;

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

	private void setSensorDate() {

		// Access to collection
		MongoCollection<Document> sensors = db.getCollection("sensors");

		sensors.updateOne(Filters.eq("_id", new ObjectId(getSensorId())), Updates.currentDate("lastModified"));

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
