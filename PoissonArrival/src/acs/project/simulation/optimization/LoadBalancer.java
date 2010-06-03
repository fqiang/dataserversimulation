package acs.project.simulation.optimization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import org.apache.log4j.Logger;

import acs.project.simulation.common.Location;
import acs.project.simulation.common.RequestEvent;
import acs.project.simulation.common.ServerConfigInfo;
import acs.project.simulation.common.ServerStatus;
import acs.project.simulation.common.SimulationEnd;
import acs.project.simulation.common.StatusRequest;
import acs.project.simulation.common.TimeStamp;
import acs.project.simulation.optimization.strategy.EnergyAwareStrategyInterface;
import acs.project.simulation.optimization.strategy.SimpleStrategy;

public class LoadBalancer {

	public final static Logger log = Logger.getLogger(LoadBalancer.class);
	
	//simulation strategy
	private EnergyAwareStrategyInterface strategy = null;
	
	//simulation properties constant
	public static final int DEFAULT_LB_PORT = 4000;
	public static final String DEFAULT_LB_ADDR = "localhost";
	public static final int DEAFAULT_TOTAL_SERVERS = 5;
	
	//simulation properties
	private int numOfServers = 0;
	private int port = 0;  //lb lisening port
	private PrintStream reportPrinter;
	private ServerSocket server_socket = null;
	
	//system status
	private List<ArrayList<ServerProfile>> serverlist = null;
	private long requestDiscarded = 0;
	private long requestTotal = 0;
	private long requestDispatched = 0;
	
	public LoadBalancer(int numServers, int aPort, PrintStream report) throws IOException
	{
		//init strategy
		strategy = new SimpleStrategy();
		
		//init simulation properties
		this.numOfServers = numServers;
		this.port = aPort;
		this.reportPrinter = report;
		this.server_socket = new ServerSocket(port);
		
		//init system status
		serverlist = Collections.synchronizedList(new ArrayList<ArrayList<ServerProfile>>());
		requestDiscarded = 0;
		requestTotal = 0;
		requestDispatched = 0;
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
		
		//event feeder helper obj
		EventFeeder efeeder = new EventFeeder();
		
		//initialize server
		strategy.init(serverlist);
		
		//main loop
		while(true)
		{
			RequestEvent[] events = efeeder.nextEvents();
			
			//no more events
			if(events.length==0){  break; }
			
			long nowtime = events[0].getTime();
			long pretime = 0;
			
			//first broad cast and synch the new timestamp
			log.debug("Event Time["+nowtime +"] Num["+events.length+"]");
			
			//synch time
			TimeStamp ts = new TimeStamp(nowtime);
			broadcast(ts);
			
			//record trace
			record(ts); 
			
			//management stratgy
			strategy.management(serverlist);
			
			//then dispatching the event
			for (RequestEvent event:events)
			{
				requestTotal++;
				nowtime = event.getTime();
				assert pretime==0||nowtime==pretime;
				pretime = nowtime;
				
				ServerProfile profile = strategy.selectServer(serverlist,event);
				if(profile==null){
					requestDiscarded++;
					log.debug("Discard Event at EventTimeTick["+nowtime+"]");
				}
				else{
					requestDispatched++;
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

	private void record(TimeStamp ts) throws IOException, ClassNotFoundException {	
		long totEngConsumption = 0;
		long totEngCost = 0;
		for(ArrayList<ServerProfile> servers:serverlist)
		{
			for(ServerProfile server:servers)
			{
				//request lastest status
				StatusRequest request = new StatusRequest();
				server.getOos().writeObject(request);
				//get status report
				ServerStatus status = (ServerStatus)server.getOis().readObject();
				server.setStatus(status);
				assert ts.getCurrTime() == status.getCurrTime();
				totEngConsumption += status.getCurrTolConsumption();
				totEngCost += status.getCurrEnvCost();
			}
		}
		reportPrinter.println(ts.getCurrTime()+","+totEngConsumption+","+totEngCost);
		
	}

	private void cleanup() throws IOException, ClassNotFoundException {
		long totEngConsumption = 0;
		long totEngCost = 0;
		long maxTime = 0;
		
		StringBuilder sb = new StringBuilder();
		
		for(ArrayList<ServerProfile> servers:serverlist)
		{
			for(ServerProfile server:servers)
			{
				//read last status
				ServerStatus status = (ServerStatus)server.getOis().readObject();
				server.setStatus(status);
				
				server.getSocket().close();
				server.getOos().close();
				server.getOis().close();
				sb.append(server.getInfo().getServerName()+","+server.status.toString());
				sb.append(System.getProperty("line.separator"));
				
				totEngConsumption += status.getCurrTolConsumption();
				totEngCost += status.getCurrEnvCost();
				maxTime = Math.max(status.getCurrTime(), maxTime);
			}
		}
		
		reportPrinter.println(maxTime+","+totEngConsumption+","+totEngCost);
		reportPrinter.println("====,====,====,====,====,====,====,");
		reportPrinter.println("Simulation Finished,Final Status");
		reportPrinter.println("Server Name,"+ServerStatus.getColName());
		reportPrinter.print(sb.toString());
		reportPrinter.println("====,====,====,====,====,====,====,");
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
			report.println("currTime,TotalEnergyConsumption,TotalEnvCost");
			
			LoadBalancer lb = new LoadBalancer(numServers,port,report);		
			log.debug("LoadBalancer created ...");
			lb.register();
			lb.running();
		}
	}
}
