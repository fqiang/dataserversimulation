package acs.project.simulation.server;

public class RequestInitProperty {
	

	public final long DEFAULT_CONN_EST_TIME = 100; //in milli, can be future improved using matrix (location,location)
	public final long DEFAULT_RAMP_UP_TIME = 3000; //in millisecond, speed ramp up
	public final long DEFAULT_SPEED_LIMIT = 300; //bytes per millsecond  - per connection
	public final long DEFAULT_SPEED_INIT = 30;   //bytes per millsecond
	
	private long connEstTime = DEFAULT_CONN_EST_TIME;
	private long rampUpTime = DEFAULT_RAMP_UP_TIME;
	private long speedLimit = DEFAULT_SPEED_LIMIT;
	private long speedInit = DEFAULT_SPEED_INIT;
	
	public RequestInitProperty()
	{
	}
	
	public void setConnEstTime(long connEstTime) {
		this.connEstTime = connEstTime;
	}
	public long getConnEstTime() {
		return connEstTime;
	}
	public void setRampUpTime(long rampUpTime) {
		this.rampUpTime = rampUpTime;
	}
	public long getRampUpTime() {
		return rampUpTime;
	}
	public void setSpeedLimit(long speedLimit) {
		this.speedLimit = speedLimit;
	}
	public long getSpeedLimit() {
		return speedLimit;
	}
	public void setSpeedInit(long speedInit) {
		this.speedInit = speedInit;
	}
	public long getSpeedInit() {
		return speedInit;
	}
	
	
}
