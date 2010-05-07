package acs.project.simulation.common;

import java.io.Serializable;

public class StateChangeRequest implements Serializable {

	/**
	 * 
	 */
	private String requestState = null;
	
	private static final long serialVersionUID = -8745955099765062297L;

	
	public StateChangeRequest(String state)
	{
		requestState = state;
	}


	public void setRequestState(String requestState) {
		this.requestState = requestState;
	}


	public String getRequestState() {
		return requestState;
	}
}
