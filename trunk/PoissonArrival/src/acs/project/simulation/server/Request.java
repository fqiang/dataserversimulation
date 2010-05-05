package acs.project.simulation.server;

import java.lang.reflect.Field;
import java.util.Arrays;

import acs.project.simulation.common.OrderComparator;
import acs.project.simulation.common.RequestEvent;

public class Request {

	private RequestEvent event  = null;
	
	//status
	private long sizeLeft = 0;
	private long timeAlive = 0;
	private long currSpeed = 0;
	private Phase state = Phase.INIT;
	
	//request properties
	private long connEstTime = 0;
	private long rampUpTime = 0;
	private long initSpeed = 0;
	private long maxSpeed = 0;
	
	public Request(RequestEvent aEvent,long connEst,long rampUp,long maxspeed,long initspeed) {
		//delegated
		event = aEvent;
		
		//status init
		sizeLeft = event.getSize();
		timeAlive = 0;
		currSpeed = 0;
		state = Phase.INIT;
		
		//property init
		connEstTime = connEst;
		rampUpTime = rampUp;
		maxSpeed = maxspeed;
		initSpeed = initspeed;
	}
	
	public String toString(){
		StringBuffer buf = new StringBuffer();
		
		Field fields[] = this.getClass().getDeclaredFields();
		for(Field f:fields)
		{
			buf.append(f.getName()+"[");
			try {
				buf.append(f.get(this).toString());
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
	
	public void setEvent(RequestEvent event) {
		this.event = event;
	}

	public RequestEvent getEvent() {
		return event;
	}

	public void setSizeLeft(long size) {
		this.sizeLeft = size;
	}

	public long getSizeLeft() {
		return sizeLeft;
	}

	public void setTimeAlive(long timeAlive) {
		this.timeAlive = timeAlive;
	}

	public long getTimeAlive() {
		return timeAlive;
	}

	public void setCurrSpeed(long currSpeed) {
		this.currSpeed = currSpeed;
	}

	public long getCurrSpeed() {
		return currSpeed;
	}

	public void setRamupTime(long rampuptime) {
		this.rampUpTime = rampuptime;
	}

	public long getRamupTime() {
		return rampUpTime;
	}

	public void setMaxSpeed(long maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	//request bound speed
	public long getMaxSpeed() {
		return maxSpeed;
	}

	public void setState(Phase state) {
		this.state = state;
	}

	public Phase getState() {
		return state;
	}

	public void setConnEstTime(long connEstTime) {
		this.connEstTime = connEstTime;
	}

	public long getConnEstTime() {
		return connEstTime;
	}

	public void setInitSpeed(long initSpeed) {
		this.initSpeed = initSpeed;
	}

	public long getInitSpeed() {
		return initSpeed;
	}

}
