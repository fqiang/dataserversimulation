package acs.project.simulation.dataset.common;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

public class RequestEvent implements Serializable{

	@Order(value=1)
	private Location location;
	@Order(value=2)
	private Timezone timezone;	
	@Order(value=3)
	private long   time;
	@Order(value=4)
	private long    size; //in byte
	@Order(value=5)
	private int	   rank; 
	@Order(value=6)
	private static final long serialVersionUID = 6607490842817004146L;
	
	
	public RequestEvent(Location location,Timezone timezone,long arrival_time,long content_size,int pop)
	{
		this.location = location;
		this.timezone = timezone;
		this.rank = pop;
		this.size = content_size;
		this.time = arrival_time;
	}
	
	private RequestEvent(String csvline)
	{
		StringTokenizer st = new StringTokenizer(csvline,",");
		try {
			this.location = Location.valueOf(st.nextToken());
			this.timezone = Timezone.valueOf(st.nextToken());
			this.time = Long.parseLong(st.nextToken());
			this.size = Long.parseLong(st.nextToken());
			this.rank = Integer.parseInt(st.nextToken());
		}
		catch (Exception e)
		{
			Logger.getLogger(RequestEvent.class).debug("File format missmatched! at current line:["+csvline+"]");
		}
	}
	
	public static RequestEvent fromString(String csvline)
	{
		RequestEvent event = new RequestEvent(csvline);
		return event;
	}
	
	public String toString(){
		String val = "";
		Field[] fields = this.getClass().getDeclaredFields();
		Arrays.sort(fields,new OrderComparator());
		for(Field f:fields){
			try {
				val += f.get(this) +",";
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return val;
	}

	public static void main(String args[])
	{
		System.out.println("Testing request arrival event");
		RequestEvent event = new RequestEvent(Location.ASIAN,Timezone.GMT_0,1000,100,10);
		System.out.println(event.toString());
	}
	
	public void setTime(long time) {
		this.time = time;
	}

	public long getTime() {
		return time;
	}
	public Location getLocation() {
		return location;
	}
	
	public void setLocation(Location location) {
		this.location = location;
	}
	
	public Timezone getTimezone() {
		return timezone;
	}
	
	public void setTimezone(Timezone timezone) {
		this.timezone = timezone;
	}
	
	public long getSize() {
		return size;
	}
	
	public void setSize(long size) {
		this.size = size;
	}
	
	public int getRank() {
		return rank;
	}
	
	public void setRank(int rank) {
		this.rank = rank;
	}
}