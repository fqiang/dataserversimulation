package acs.project.simulation.common;

import java.io.Serializable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import acs.project.simulation.server.EnergyType;


public class ServerStatus implements Serializable{

	/**
	 * 
	 */

	@Order(value=1)
	private int currNumReqs = 0;
	@Order(value=2)
	private long requestRecv = 0;
	@Order(value=3)
	private long requestDiscard = 0;
	@Order(value=4)
	private long requestHandled = 0;
	@Order(value=5)
	private State state;
	@Order(value=6)
	private long currTime = 0;
	@Order(value=7)
	private long currBW  = 0;
	@Order(value=8)
	private double currPower = 0;
	@Order(value=9)
	private double currLoad = 0;
	@Order(value=10)
	private double currTolConsumption = 0;
	@Order(value=11)
	private double currEnvCost = 0;
	@Order(value=12)
	private EnergyType energy;
	@Order(value=13)
	private static final long serialVersionUID = -964803076899075800L;
	
	public ServerStatus()
	{	
	}

	public static String getColName()
	{
		String val = "";
		Field[] fields = ServerStatus.class.getDeclaredFields();
		for(Field f:fields){
			try {
				if(!Modifier.isFinal(f.getModifiers())){
					val += f.getName()+",";
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} 
		}
		return val;
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
	
	public void setCurrBW(long currBW) {
		this.currBW = currBW;
	}

	public double getCurrBW() {
		return currBW;
	}

	public void setCurrLoad(double currLoad) {
		this.currLoad = currLoad;
	}

	public double getCurrLoad() {
		return currLoad;
	}

	public void setCurrNumReqs(int currNumReqs) {
		this.currNumReqs = currNumReqs;
	}

	public int getCurrNumReqs() {
		return currNumReqs;
	}

	public void setCurrTime(long currTime) {
		this.currTime = currTime;
	}

	public long getCurrTime() {
		return currTime;
	}

	public void setRequestHandled(long requestHandled) {
		this.requestHandled = requestHandled;
	}

	public long getRequestHandled() {
		return requestHandled;
	}

	public void setCurrPower(double currPower) {
		this.currPower = currPower;
	}

	public double getCurrPower() {
		return currPower;
	}

	public void setCurrTolConsumption(double currTolConsumption) {
		this.currTolConsumption = currTolConsumption;
	}

	public double getCurrTolConsumption() {
		return currTolConsumption;
	}

	public void setRequestRecv(long requestRecv) {
		this.requestRecv = requestRecv;
	}

	public long getRequestRecv() {
		return requestRecv;
	}

	public void setRequestDiscard(long requestDiscard) {
		this.requestDiscard = requestDiscard;
	}

	public long getRequestDiscard() {
		return requestDiscard;
	}

	public void setState(State state) {
		this.state = state;
	}

	public State getState() {
		return state;
	}

	public void setEnergy(EnergyType energy) {
		this.energy = energy;
	}

	public EnergyType getEnergy() {
		return energy;
	}

	public void setCurrEnvCost(double currEnvCost) {
		this.currEnvCost = currEnvCost;
	}

	public double getCurrEnvCost() {
		return currEnvCost;
	}


}
