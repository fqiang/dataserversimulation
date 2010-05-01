package acs.project.simulation.optimization;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;

import acs.project.simulation.dataset.common.Location;
import acs.project.simulation.dataset.common.ServerConfigInfo;
import acs.project.simulation.dataset.common.ServerStatus;

public class ServerProfile {
	
	//server property
	private ServerConfigInfo info = null;
	
	//simulation property
	private Socket socket = null;  //the socket to the server
	private ObjectOutputStream oos = null; //ouput to the server
	private ObjectInputStream ois = null;   //input to the lb from server
	
	
	//server status
	ServerStatus status = null;

	public ServerProfile()
	{		
	}
	
	public ServerStatus getStatus() {
		return status;
	}
	
	public void setStatus(ServerStatus status) {
		this.status = status;
	}
	
	public Socket getSocket() {
		return socket;
	}
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	public ObjectOutputStream getOos() {
		return oos;
	}
	public void setOos(ObjectOutputStream oos) {
		this.oos = oos;
	}
	public ObjectInputStream getOis() {
		return ois;
	}
	public void setOis(ObjectInputStream ois) {
		this.ois = ois;
	}
	
	public ServerConfigInfo getInfo() {
		return info;
	}

	public void setInfo(ServerConfigInfo info) {
		this.info = info;
	}
}
