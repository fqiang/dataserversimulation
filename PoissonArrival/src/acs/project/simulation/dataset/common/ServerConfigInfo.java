package acs.project.simulation.dataset.common;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;


public class ServerConfigInfo implements Serializable {

	/**
	 * 
	 */
	@Order(value=1)	
	private String serverName;
	@Order(value=2)
	private Location location;
	@Order(value=3)
	private int maxConcurrentRequest;
	@Order(value=4)
	private long maxBW;
	@Order(value=5)
	private double maxPower;
	@Order(value=6)
	private double idlePowerRate;
	@Order(value=7)
	private double sleepPowerRate;
	@Order(value=8)
	private static final long serialVersionUID = -7541603586424204701L;
	
	public ServerConfigInfo()
	{
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
	
	public void setLocation(Location location) {
		this.location = location;
	}
	public Location getLocation() {
		return location;
	}
	public void setMaxBW(long max_bindwidth) {
		this.maxBW = max_bindwidth;
	}
	public long getMaxBW() {
		return maxBW;
	}
	public void setMaxConcurrentRequest(int maxConRequest) {
		this.maxConcurrentRequest = maxConRequest;
	}
	public int getMaxConcurrentRequest() {
		return maxConcurrentRequest;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getServerName() {
		return serverName;
	}

	public void setMaxPower(double maxPower) {
		this.maxPower = maxPower;
	}

	public double getMaxPower() {
		return maxPower;
	}

	public void setIdlePowerRate(double idlePowerRate) {
		this.idlePowerRate = idlePowerRate;
	}

	public double getIdlePowerRate() {
		return idlePowerRate;
	}

	public void setSleepPowerRate(double sleepPowerRate) {
		this.sleepPowerRate = sleepPowerRate;
	}

	public double getSleepPowerRate() {
		return sleepPowerRate;
	}
	
	
}
