package kasselsensors;

public class Sensor {

	private String id;
	private String location;
	private double lat;
	private double lng;
	
	public Sensor(String id, String loc, double lat, double lng){
		this.id = id;
		this.location = loc;
		this.lat = lat;
		this.lng = lng;
	}
	
	public void setId(String id){
		this.id = id;
	}
	
	public void setLocation(String loc){
		this.location = loc;
	}
	
	public void setLat(double lat){
		this.lat = lat;
	}
	
	public void setLng(double lng){
		this.lng = lng;
	}
	
	public String getId(){
		return id;
	}
	
	public String getLocation(){
		return location;
	}
	
	public double getLat(){
		return lat;
	}
	
	public double getLng(){
		return lng;
	}
}
