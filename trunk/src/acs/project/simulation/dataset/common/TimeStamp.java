package acs.project.simulation.dataset.common;

import java.io.Serializable;

public class TimeStamp implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3238910887710777224L;

	private long currTime = 0;

	public TimeStamp(long time)
	{
		currTime = time;
	}
	
	public void setCurrTime(long currTime) {
		this.currTime = currTime;
	}

	public long getCurrTime() {
		return currTime;
	}
}
