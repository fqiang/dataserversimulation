package acs.project.simulation.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import acs.project.simulation.dataset.common.RequestArrivalEvent;
import acs.project.simulation.optimization.LoadBalancer;

public class TestLoadBalancer {
	
	public static final Logger log = Logger.getLogger(TestLoadBalancer.class);
	
	public static void main(String[] args)
	{
		double rate  =0 ;
		int size = 1200;
		int now = 1200;
		rate = now/(double)size;
		System.out.println(rate+"||"+(rate==1d)+"||"+(0d==-0d));
		/*
		try{
			int port = LoadBalancer.DEFAULT_LB_PORT;
			ServerSocket server = new ServerSocket(port);
			log.debug("LoadBalancer listening on port:"+port);
			Socket client = server.accept();
			log.debug("Accepted a connection from:"+client.getInetAddress());
			ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
			ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
			BufferedReader trace = new BufferedReader(new FileReader(new File("asian_gmt8_onehr_halfhr.csv")));
			
			while(true) {
				log.debug("sending event...");
				RequestArrivalEvent event = RequestArrivalEvent.fromString(trace.readLine());
				oos.writeObject(event);
				log.debug("read server's response.");
				//ServerInfo info = (ServerInfo)ois.readObject();
//				if(info.getType()==ServerInfoEnum.REQ)
//				{
//					log.debug("sending the next event.");
//				}
//				else
//				{
//					break;
//				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		*/
	}
}
