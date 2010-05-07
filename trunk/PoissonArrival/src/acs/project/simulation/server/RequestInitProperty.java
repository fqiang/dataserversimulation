package acs.project.simulation.server;

public class RequestInitProperty {
	
	private long connEstTime = 0;
	private long rampUpTime = 0;
	private long speedLimit = 0;
	private long speedInit = 0;
	
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
