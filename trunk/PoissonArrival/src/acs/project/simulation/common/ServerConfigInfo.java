package acs.project.simulation.common;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
	private double maxPower;
	@Order(value=4)
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
				if(!Modifier.isFinal(f.getModifiers())){
					val += f.get(this) +",";
				}
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
	
}
