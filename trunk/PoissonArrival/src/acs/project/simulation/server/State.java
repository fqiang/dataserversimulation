package acs.project.simulation.server;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import acs.project.simulation.common.OrderComparator;
import acs.project.simulation.common.Order;

public class State implements Serializable{
	
	/**
	 * 
	 */
	@Order(value=1)
	private String name;
	@Order(value=2)
	private int maxConRequests;
	@Order(value=3)
	private long speedLimit;  //per connection
	@Order(value=4)
	private long maxBW;
	@Order(value=5)
	private double powerRate;     //respect to max power of the server
	@Order(value=6)
	private double idlePowerRate; //represent the zero load running of the server
	@Order(value=7)
	private static final long serialVersionUID = 2649412074112035870L;

	public State(String aName,int maxC,long speed,long maxB,double pr,double ipr)
	{
		init(aName, maxC,speed, maxB, pr,ipr);
	}
	
	public void init(String aName,int maxC,long speed,long maxB,double pr,double ipr)
	{
		name = aName;
		maxConRequests = maxC;
		speedLimit = speed;
		maxBW = maxB;
		powerRate = pr;
		idlePowerRate = ipr;
	}
	
	
	
	public String toString()
	{
		StringBuffer buf = new StringBuffer();	
		Field fields[] = this.getClass().getDeclaredFields();
		Arrays.sort(fields,new OrderComparator());
		for(Field f:fields)
		{
			buf.append(f.getName()+"[");
			try {
				if(Modifier.isFinal(f.getModifiers())){
					buf.append(f.get(this).toString());
				}
			}
			catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			buf.append("]-");
		}		
		return buf.toString();	
	}
	
	public void setMaxConRequests(int maxConRequests) {
		this.maxConRequests = maxConRequests;
	}
	public int getMaxConRequests() {
		return maxConRequests;
	}
	public void setMaxBW(long maxBW) {
		this.maxBW = maxBW;
	}
	public long getMaxBW() {
		return maxBW;
	}
	public void setPowerRate(double statePowerRate) {
		this.powerRate = statePowerRate;
	}
	public double getPowerRate() {
		return powerRate;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		State o = (State)obj;
		return name.equals(o.getName());
	}

	public void setIdlePowerRate(double stateIdlePowerRate) {
		this.idlePowerRate = stateIdlePowerRate;
	}

	public double getIdlePowerRate() {
		return idlePowerRate;
	}

	public void setSpeedLimit(long speedLimit) {
		this.speedLimit = speedLimit;
	}

	public long getSpeedLimit() {
		return speedLimit;
	}

}
