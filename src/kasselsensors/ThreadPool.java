package kasselsensors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;

public class ThreadPool {

	public static void main(String[] args) {

		List<Sensor> sensors = new ArrayList<Sensor>();
		sensors.add(new Sensor("Hamburg%20Billbrook", 53.529019999999996, 10.081685));
		sensors.add(new Sensor("Hamburg%20Veddel", 53.52326200000001, 10.021227000000001));
		sensors.add(new Sensor("Hamburg%20Wilhelmsburg", 53.507929999999995, 9.990569));
		sensors.add(new Sensor("Hamburg%20Hafen", 53.529157999999995, 9.981598999999997));
		sensors.add(new Sensor("Hamburg%20Altona%20Elbhang", 53.54526899999999, 9.944815));

		ExecutorService executor = Executors.newFixedThreadPool(6);

		// TODO: find proper place to create the connection
		// MongoDB connection
		MongoClient client = new MongoClient(
				new MongoClientURI("mongodb://kasselpi:kpi@ds129442.mlab.com:29442/kpisensors"));
		MongoDatabase db = client.getDatabase("kpisensors");
		

		for (int i = 0; i < sensors.size(); i++) {
			Runnable worker = new WorkerThread(sensors.get(i).getLocation(), sensors.get(i).getLat(),
					sensors.get(i).getLng(), db);
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()) {
		}

		System.out.println("Finished all threads");
	}

}
