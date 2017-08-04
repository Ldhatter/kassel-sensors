package kasselsensors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.bson.Document;
import org.json.JSONObject;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class ThreadPool {

	public static void main(String[] args) {

		// Sensors
		List<Sensor> sensors = new ArrayList<Sensor>();

		// MongoDB connection
		MongoClient client = new MongoClient(
				new MongoClientURI("mongodb://kasselpi:kpi@ds129442.mlab.com:29442/kpisensors")); //store e.w
		MongoDatabase db = client.getDatabase("kpisensors");

		// Access to collection
		MongoCollection<Document> sensorsDB = db.getCollection("raspi");

		// Get sensor information from db
		sensorsDB.find(new Document()).forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {

				String id = document.get("_id").toString();
				String loc = (String) document.get("location");
				Document coords = (Document) document.get("coordinates");
				double lat = (double) coords.get("lat");
				double lng = (double) coords.get("lng");

				sensors.add(new Sensor(id, loc, lat, lng));

			}
		});

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(sensors.size());

		for (int i = 0; i < sensors.size(); i++) {
			Runnable worker = new WorkerThread(sensors.get(i).getId(), sensors.get(i).getLocation(), sensors.get(i).getLat(),sensors.get(i).getLng(), db);
			executor.execute(worker);
		}

		// TODO: check this implementation.
		while (!executor.isShutdown()) {
			if (executor.getTaskCount() == executor.getCompletedTaskCount()) {
				executor.shutdown();
			}
		}

		while (!executor.isTerminated()) { // returns true if all tasks
											// shutdown, must use shutdown first
		}

		System.out.println("Finished all threads");
	}

}
