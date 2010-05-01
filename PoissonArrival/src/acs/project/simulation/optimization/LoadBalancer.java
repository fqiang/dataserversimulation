package acs.project.simulation.optimization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import acs.project.simulation.server.ServerNode;
import acs.project.simulation.dataset.common.Location;
import acs.project.simulation.dataset.common.RequestArrivalEvent;
import acs.project.simulation.dataset.common.ServerConfigInfo;
import acs.project.simulation.dataset.common.ServerStatus;
import acs.project.simulation.dataset.common.SimulationEnd;
import acs.project.simulation.dataset.common.StatusRequest;
import acs.project.simulation.dataset.common.TimeStamp;
import acs.project.simulation.optimization.*;

public class LoadBalancer {

	public final static Logger log = Logger.getLogger(LoadBalancer.class);
	
	//simulation properties
	public static final int DEFAULT_LB_PORT = 4000;
	public static final String DEFAULT_LB_ADDR = "localhost";
	public static final int DEAFAULT_TOTAL_SERVERS = 3;
	
	private int numOfServers = 0;
	private int port = 0;  //lb lisening port
	private PrintStream reportPrinter;
	private ServerSocket server_socket = null;
	
	//system status
	private List<ArrayList<ServerProfile>> serverlist = Collections.synchronizedList(new ArrayList<ArrayList<ServerProfile>>());
	private long totDiscardRequest = 0;
	private long totRequest = 0;
	private long totHandledRequest = 0;
	
	public LoadBalancer(int numServers, int aPort, PrintStream report) throws IOException
	{
		this.numOfServers = numServers;
		this.port = aPort;
		this.reportPrinter = report;
		//this.addr = DEFAULT_LB_ADDR;
		this.server_socket = new ServerSocket(port);
		for (Location l : Location.values())
		{
			ArrayList<ServerProfile> servers = new ArrayList<ServerProfile>();
			serverlist.add(servers);
		}
	}
	
	private int getTotalServers()
	{
		int num = 0;
		for(Iterator<ArrayList<ServerProfile>> iter = serverlist.iterator();iter.hasNext();){
			ArrayList<ServerProfile> list = iter.next();
			num += list.size();
		}
		return num;
	}
	
	public void register() throws IOException, ClassNotFoundException
	{
		while(getTotalServers()<numOfServers){
			log.debug("waiting to accept server ...");
			Socket theSocket = this.server_socket.accept();
			ObjectOutputStream oos = new ObjectOutputStream(theSocket.getOutputStream());
			ObjectInputStream ois = new ObjectInputStream(theSocket.getInputStream());
			ServerConfigInfo info = (ServerConfigInfo)ois.readObject();
			
			//set info
			ServerProfile server = new ServerProfile();
			server.setInfo(info);
			
			server.setOis(ois);
			server.setOos(oos);
			server.setSocket(theSocket);
			
			//set status
			ServerStatus status = (ServerStatus)ois.readObject();
			server.setStatus(status);
			
			//into lists (2D)
			ArrayList<ServerProfile> servers = serverlist.get(server.getInfo().getLocation().ordinal());
			log.debug("incoming server node ["+servers.size()+"] -  "+theSocket.getInetAddress().toString());
			servers.add(server);
		}
		log.debug("server register done!");
	}
	
	public void running() throws InterruptedException, IOException, ClassNotFoundException
	{	
		log.debug("LoadBalancer start running");
		EventFeeder efeeder = new EventFeeder();
		
		while(true)
		{
			//Thread.currentThread().sleep(4000);
			RequestArrivalEvent[] events = efeeder.nextEvents();
			if(events.length==0){
				//no more events
				break;
			}
			long nowtime = events[0].getTime();
			long pretime = 0;
			
			//first broad cast the new timestamp
			log.debug("   Time tick: "+nowtime);
			
			TimeStamp ts = new TimeStamp(nowtime);
			broadcast(ts);
			
			//then dispatching the event
			for (RequestArrivalEvent event:events)
			{
				totRequest++;
				nowtime = event.getTime();
				assert pretime==0||nowtime==pretime;
				pretime = nowtime;
				
				ServerProfile profile = this.selectServer(event);
				if(profile==null){
					totDiscardRequest++;
					log.debug("Discard Event at EventTimeTick["+nowtime+"]");
				}
				else{
					totHandledRequest++;
					log.debug("Send to Server["+profile.getInfo().getServerName()+"]  Event["+event.toString()+"]");
					profile.getOos().writeObject(event);
				}
			}	
		}
		
		log.debug("No More Events --");
		SimulationEnd end = new SimulationEnd();
		broadcast(end);
		
		cleanup();
	}

	private void cleanup() throws IOException, ClassNotFoundException {
		reportPrinter.println("*********************************************");
		reportPrinter.println("Simulation Finished - Starting report");
		for(ArrayList<ServerProfile> servers:serverlist)
		{
			for(ServerProfile server:servers)
			{
				ServerStatus status = (ServerStatus)server.getOis().readObject();
				server.setStatus(status);
				reportPrinter.println("+++++++++++++++++++++++++++++++++++++++++++++++");
				reportPrinter.println("+Server Name: "+server.getInfo().getServerName());
				reportPrinter.println("+++++++++++++++++++++++++++++++++++++++++++++++");
				server.getSocket().close();
				server.getOos().close();
				server.getOis().close();
			}
		}
	}

	private void broadcast(Object obj) throws IOException {
		for(ArrayList<ServerProfile> servers:serverlist)
		{
			for(ServerProfile server:servers)
			{
				server.getOos().writeObject(obj);
			}
		}
	}

	public ServerProfile selectServer(RequestArrivalEvent event) throws IOException, ClassNotFoundException
	{
		/*
		 * this is a very simple strategy finding a free server in the area(location)
		 */
		ServerProfile profile = null;
		ArrayList<ServerProfile> servers = this.serverlist.get(event.getLocation().ordinal());
		for(ServerProfile server:servers)
		{
			StatusRequest req = new StatusRequest();
			server.getOos().writeObject(req);
			ServerStatus status = (ServerStatus)server.getOis().readObject();
			server.setStatus(status);
			assert status.getCurrLoad() <= 1;
			if(status.getCurrLoad()<1)
			{
				return server;
			}
			else
			{
				log.debug("Server Busy - ServerName["+server.getInfo().getServerName()+"]"+status.toString());
			}
		}
		
		return profile;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException
	{
		if(args.length == 0)
		{
			System.out.println("./java LoadBalancer [NumOfServer] [op[port]]");
		}
		else
		{
			int numServers = Integer.parseInt(args[0]);
			int port = args.length==1?LoadBalancer.DEFAULT_LB_PORT:Integer.parseInt(args[1]);
			
			String filename = ".//report//LoadBalancer" +".report";
			int endIndex = filename.length();
			File file = new File(filename);
			int i = 1;
			while(file.exists())
			{
				filename = filename.substring(0,endIndex) + "." + i;
				file = new File(filename);
				i++;
			}
			PrintStream report = new PrintStream(new FileOutputStream(file), true);
			
			LoadBalancer lb = new LoadBalancer(numServers,port,report);		
			log.debug("LoadBalancer created ...");
			lb.register();
			lb.running();
		}
	}
}
