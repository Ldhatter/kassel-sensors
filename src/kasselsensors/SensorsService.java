package kasselsensors;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

//import javax.ws.rs.PUT; What would you edit to the raspi? unit?
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
	MongoClient client = new MongoClient(new MongoClientURI("mongodb://kasselpi:kpi@ds129442.mlab.com:29442/kpisensors"));
	MongoDatabase db = client.getDatabase("kpisensors");
	
	//MongoClient client = MongoClients.create(new ConnectionString("mongodb://kasselpi:kpi@ds129442.mlab.com:29442/kpisensors"));
	//MongoDatabase db = client.getDatabase("kpisensors");
	
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
		Document data = new Document("location", json.get("location")).append("updated", json.getInt("updated")).append(
				"coordinates", new Document("lat", locLatLng.getDouble("lat")).append("lng", locLatLng.getDouble("lng")));

		// Add sensor to db
		sensors.insertOne(data);
		
		return Response.status(200).build();
	}

	// @PUT
	// @Produce("application/json")
	// public Response updateSensor(String s) throws JSONException {
	//
	// //Update sensor
	// 
	//
	// return Response.status(200).build();
	// }

	@DELETE
	@Path("/{id}")
	@Produces("application/json")
	public Response deleteSensor(@PathParam("id") String id) {
		
		sensors.deleteOne(new Document("_id", new ObjectId(id)));
		return Response.status(200).build();
		
	}
	
}
