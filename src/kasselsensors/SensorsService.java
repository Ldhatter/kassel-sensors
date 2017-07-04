package kasselsensors;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

import javax.ws.rs.PUT;
import javax.ws.rs.GET;
import javax.ws.rs.POST;

import java.time.Instant;
import java.util.Date;

//import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces; //Type of return xml, json etc.
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response; //Needed for json response

import org.bson.BSON;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException; //double check is this req/std for working with json
import org.json.JSONObject; //Convert data to JSON
import org.bson.conversions.*;

@Path("/sensorsservice")
public class SensorsService {

	/*
	 * MongoDB connection
	 */
	MongoClient client = new MongoClient(
			new MongoClientURI("mongodb://kasselpi:kpi@ds129442.mlab.com:29442/kpisensors"));
	MongoDatabase db = client.getDatabase("kpisensors");

	/*
	 * Access to collection
	 */
	MongoCollection<Document> sensors = db.getCollection("sensors");

	@GET
	@Produces("application/json")
	public Response findSensor() throws JSONException {

		JSONArray data = new JSONArray();

		sensors.find().forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				JSONObject doc = new JSONObject(document.toJson());
				data.put(doc);
			}
		});

		return Response.ok(data.toString()).build();
	}

	@GET
	@Path("/{id}")
	@Produces("application/json")
	public Response findSensor(@PathParam("id") String id) throws JSONException {

		JSONArray data = new JSONArray();

		sensors.find(new Document("_id", new ObjectId(id))).forEach(new Block<Document>() {
			@Override
			public void apply(final Document document) {
				JSONObject doc = new JSONObject(document.toJson());
				data.put(doc);
			}
		});

		return Response.ok(data.toString()).build();
	}

	@POST
	@Produces("application/json")
	public Response addSensor(String s) throws JSONException {

		JSONObject json = new JSONObject(s);
		JSONObject locLatLng = json.getJSONObject("coordinates");

		// Parse sensor data
		Document data = new Document("location", json.get("location"))
				.append("coordinates", new Document("lat", locLatLng.getDouble("lat"))
				.append("lng", locLatLng.getDouble("lng")))
				.append("lastModified", Instant.now().getEpochSecond()*1000);

		// Add sensor to db
		sensors.insertOne(data);

		return Response.status(200).build();
	}

	@PUT
	@Path("/{id}")
	@Produces("application/json")
	public Response updateSensor(@PathParam("id") String id) throws JSONException {

		// Update sensor lastModified date
		UpdateResult result = sensors.updateOne(Filters.eq("_id", new ObjectId(id)), Updates.currentDate("lastModified"));
		// TOOD: handle result/status of the update
		
		return Response.status(200).build();
	}

	@DELETE
	@Path("/{id}")
	@Produces("application/json")
	public Response deleteSensor(@PathParam("id") String id) {

		sensors.deleteOne(new Document("_id", new ObjectId(id)));
		return Response.status(200).build();

	}

}
