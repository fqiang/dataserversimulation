package acs.project.simulation.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Vector;

import org.apache.log4j.*;
import acs.project.simulation.dataset.common.Location;
import acs.project.simulation.dataset.common.RequestArrivalEvent;
import acs.project.simulation.dataset.common.ServerConfigInfo;
import acs.project.simulation.dataset.common.ServerStatus;
import acs.project.simulation.dataset.common.SimulationEnd;
import acs.project.simulation.dataset.common.StatusRequest;
import acs.project.simulation.dataset.common.TimeStamp;
import acs.project.simulation.optimization.LoadBalancer;

public class ServerNode {
	
	public final static Logger log = Logger.getLogger(ServerNode.class);
	
	//simulation property
	private final int CONN_EST_TIME = 100; //in milli, can be future improved using matrix (location,location)
	private final int RAMP_UP_TIME = 3000; //in millisecond, speed ramp up
	private final long SPEED_LIMIT = 300; //bytes per millsecond  - per connection
	private final long SPEED_INIT = 30;   //bytes per millsecond
	
	//simulation connections
	private Socket socket = null;    //the communication socket with LB
	private ObjectOutputStream oos = null;
	private ObjectInputStream  ois = null;
	private int lbPort;
	private String lbAddr;
	
	//system property
	private String serverName;
	private Location location;	
	private int maxConRequests;
	private long maxBW; //in bytes per millisecond
	private double maxPower;  //in J/millisecond
	private double idlePowerRate;
	
	//report printer
	private PrintStream reportPrinter;
	
	//system status
	private Vector<Request> currRequests = null;
	private long requestHandled;
	private long currTime = 0;          //simulation time
	private long nextDepartureTime = 0; //next request(s) depature time
	private long  currBW = 0;       //in byte       = sum of all req.getCurrSpeed()
	private double currPower = 0;   //in watt per millisecond = (idelPowerRate + (1-idelPowerRate)*currLoad)*maxPower
	private double currLoad = 0;    // the req load = currRequest.size()/maxConRequest
	private double totalConsumption = 0;  //in Joule


	
	public ServerNode(String name,Location loc,int max_conc_requests,long max_bw,double max_power,double idle_power_rate,int lb_port,String lb_addr,PrintStream report)
	{
		//system config init
		this.serverName = name;
		this.location = loc;
		this.maxConRequests = max_conc_requests;
		this.maxBW = max_bw;
		this.maxPower = max_power;
		this.idlePowerRate = idle_power_rate;
		this.lbPort = lb_port;
		this.lbAddr = lb_addr;
		this.reportPrinter = report;
		
		//system status init
		this.currRequests = new Vector<Request>();
		this.requestHandled = 0;
		this.currTime = 0;
		this.currBW = 0;
		this.currPower = idlePowerRate*maxPower;
		this.currLoad = 0;
		this.totalConsumption = 0;
	}
	
	public void register() throws Exception
	{
		this.socket = new Socket(this.lbAddr,this.lbPort);
		this.oos = new ObjectOutputStream(this.socket.getOutputStream());
		this.ois = new ObjectInputStream(this.socket.getInputStream());
		
		ServerConfigInfo info = createServerConfigInfo();
		oos.writeObject(info);
		ServerStatus status = createServerStatus();
		oos.writeObject(status);
	}

	private ServerConfigInfo createServerConfigInfo() {
		ServerConfigInfo info = new ServerConfigInfo();
		info.setServerName(serverName);
		info.setLocation(location);
		info.setMaxConcurrentRequest(maxConRequests);
		info.setMaxBW(maxBW);
		info.setMaxPower(maxPower);
		info.setIdlePowerRate(idlePowerRate);
		return info;
	}
	
	public void running() throws IOException, ClassNotFoundException 
	{
		log.debug("Server runnning ...localport["+this.socket.getLocalPort()+"]");
		
		while(true) 
		{
			Object obj = this.ois.readObject();
			if(obj instanceof StatusRequest)
			{
				//simulation time not proceeding
				log.debug("[StatusRequest]  - CurrTime["+currTime+"]");
				StatusRequest req = (StatusRequest)obj;
				ServerStatus status = createServerStatus();
				this.oos.writeObject(status);
			}
			else if(obj instanceof SimulationEnd)
			{
				log.debug("Request Event Done! [SimulationEnd]- OutstandingRequest["+this.currRequests.size()+"]");
				break;
			}
			else if(obj instanceof RequestArrivalEvent)
			{
				assert currRequests.size()<this.maxConRequests;
				RequestArrivalEvent event = (RequestArrivalEvent)obj;
				assert currTime == event.getTime();
				log.debug("[RequestArrivalEvent] - CurrTime["+currTime+"]    Event["+event.toString()+"]" );
				incomingRequest(event);
			}
			else if(obj instanceof TimeStamp)
			{
				TimeStamp stamp = (TimeStamp)obj;
				long synchTime = stamp.getCurrTime();
				log.debug("[TimeStamp] - TimeStampe["+synchTime+"] CurrTime["+currTime+"] CurrLoad["+currLoad+"] reqHandled["+requestHandled+"] outstandReq["+currRequests.size()+"]");
				advanceSimulationTo(synchTime);
			}
			log.debug("Server - currTime ["+currTime+"]");
		}
		
		handleOutStandingRequests();
		cleanup();
	}

	private ServerStatus createServerStatus() {
		ServerStatus status = new ServerStatus();
		status.setCurrNumReqs(currRequests.size());
		status.setRequestHandled(requestHandled);
		status.setCurrTime(currTime);
		status.setCurrBW(currBW);
		status.setCurrPower(currPower);
		status.setCurrLoad(currLoad);
		status.setCurrTolConsumption(totalConsumption);
		return status;
	}
	
	private void cleanup() throws IOException
	{
		ServerStatus status = createServerStatus();
		this.oos.writeObject(status);
		this.socket.close();
		this.oos.close();
		this.ois.close();
		reportPrinter.println("============================================");
		reportPrinter.println("Simulation Finished - at "+currTime);
		reportPrinter.println("Total Consumption:" + totalConsumption);
	}

	private void incomingRequest(RequestArrivalEvent event) {
		Request request = new Request(event,CONN_EST_TIME,RAMP_UP_TIME,SPEED_LIMIT,SPEED_INIT);
		
		//update status
		currRequests.add(request);
		currLoad = (double)currRequests.size() / (double)maxConRequests;
		currPower = (idlePowerRate + (1-idlePowerRate)*currLoad)*maxPower;
		currBW = 0;
		for(Request req:currRequests)
		{
			currBW += req.getCurrSpeed();
		}
	}
	
	private void advanceSimulationTo(long synchTime) 
	{
		//step 1:  depart all the request before synch time
		while(true)
		{
			//calculate nextDepartureTime and mkae the hashset to be removed
			HashSet<Request> toRemove = this.getNextDepartureRequests();
			if(toRemove.size()!=0&&nextDepartureTime<synchTime)
			{//depart the requests.
				long elapse = nextDepartureTime - currTime;
				assert elapse > 0;
				assert currLoad == (double)currRequests.size()/(double)maxConRequests;
				assert currPower == (idlePowerRate+(1-idlePowerRate)*currLoad)*maxPower;

				long globalMax = this.deteCurrGlobalMaxSpeed();
				//update each request in outstand request list
				for(Request req:currRequests)
				{
					if(!toRemove.contains(req)){
						this.updateRequest(elapse, req, globalMax);
					}
				}
				
				//update status 
				requestHandled += toRemove.size();
				currRequests.removeAll(toRemove);
				totalConsumption += currPower * elapse;
				currLoad = (double)currRequests.size()/(double)maxConRequests;
				currPower = (idlePowerRate+(1-idlePowerRate)*currLoad)*maxPower;
				currTime += elapse;
				currBW = 0;
				for(Request req:currRequests)
				{
					currBW += req.getCurrSpeed();
				}
			}
			else{//no more to depart
				break;
			}
		}
		assert nextDepartureTime==0||nextDepartureTime > synchTime;
		
		//step 2. proceeding more time left
		long timeleft = synchTime - currTime;
		long globalMaxSpeed = this.deteCurrGlobalMaxSpeed();
		for(Request req:currRequests)
		{
			this.updateRequest(timeleft, req, globalMaxSpeed);
		}
		//update status 
		totalConsumption += currPower * timeleft;
		currLoad = (double)currRequests.size()/(double)maxConRequests;
		currPower = (idlePowerRate+(1-idlePowerRate)*currLoad)*maxPower;
		currTime += timeleft;
		currBW = 0;
		for(Request req:currRequests)
		{
			currBW += req.getCurrSpeed();
		}
		assert currTime == synchTime;
	}

	private long deteCurrGlobalMaxSpeed()
	{
		long globalMaxSpeed = 0;
		if(currRequests.size()==0){
			globalMaxSpeed = SPEED_LIMIT;
		}
		else
		{
			globalMaxSpeed = (long)((double)maxBW/(double)currRequests.size());
			globalMaxSpeed = Math.min(globalMaxSpeed, SPEED_LIMIT);
		}
		return globalMaxSpeed;
	}
	
	//formula by quadratic equation, area2 = 2*area
	private long solveHeight(long a, long area2,double rate)
	{
		double da = (double)a;
		double darea2 = (double)area2;
		double time = (Math.sqrt(da*da+darea2*rate)-da)/rate;
		return (long)time;
	}
	
	private long calcTrapeZoidArea2(long a,long b,long h)
	{
		return (a+b)*h;
	}
	
	private long solveHeight(long a,long area)
	{
		double darea =(double)area;
		double da = (double)a;
		return (long)(darea/da);
	}
	
	private long calcRectangleArea(long a,long b)
	{
		return a*b;
	}
	
	//zero if no departures, ie no outstanding requests
	private HashSet<Request> getNextDepartureRequests()
	{
		HashSet<Request> requests = new HashSet<Request>();	
		long minDepartInterval = 0;
		long maxGlobalSpeed = this.deteCurrGlobalMaxSpeed();
		for(Request req:currRequests)
		{
			long maxReqSpeed = maxGlobalSpeed<req.getMaxSpeed()?maxGlobalSpeed:req.getMaxSpeed();  //whichever is less
			long reqAlive = req.getTimeAlive();
			long currDepartInterval = 0;
			long ph1_time = 0;
			long ph2_time = 0;
			long ph3_time = 0;
			switch(req.getState())
			{
			case INIT:
				assert req.getSizeLeft() == req.getEvent().getSize();
				ph1_time = reqAlive>req.getConnEstTime()?0:req.getConnEstTime()-reqAlive;
				long cantrans2 = this.calcTrapeZoidArea2(req.getInitSpeed(), maxReqSpeed, req.getRamupTime());
				long actualSize2 = 2*req.getSizeLeft();
				if(cantrans2>=actualSize2){ 
				//finish in phase 2
					double rate = (double)(maxReqSpeed-req.getInitSpeed())/(double)(req.getRamupTime());
					ph2_time = this.solveHeight(req.getInitSpeed(), actualSize2, rate);
					assert ph2_time <= req.getRamupTime() && ph2_time>=0;
				}
				else {
					ph2_time = req.getRamupTime();
					long leftSize = req.getSizeLeft() - (long)(cantrans2/2);
					assert leftSize > 0;
					ph3_time = this.solveHeight(maxReqSpeed, leftSize);
				}
				currDepartInterval = ph1_time + ph2_time + ph3_time;
				break;
			case RAMPUP:
				long rsizeleft2 = 2*req.getSizeLeft();
				long rrampupleft = req.getConnEstTime()+req.getRamupTime() - reqAlive;
				long rcurrSpeed = req.getCurrSpeed()==0?req.getInitSpeed():req.getCurrSpeed();
				long rtrans2 = this.calcTrapeZoidArea2(rcurrSpeed, maxReqSpeed, rrampupleft);
				if(rtrans2 >= rsizeleft2)
				{//finish at phase 2
					double rate = (double)(maxReqSpeed-rcurrSpeed)/(double)(rrampupleft);
					ph2_time = this.solveHeight(rcurrSpeed, rsizeleft2, rate);
					assert ph2_time >= 0 && ph2_time <= rrampupleft;
				}
				else
				{//to phase 3
					ph2_time = rrampupleft;
					long rleft =(long)((rsizeleft2 - rtrans2)/2d);
					ph3_time = this.solveHeight(maxReqSpeed, rleft);
					assert ph3_time >= 0;
				}
				currDepartInterval = ph2_time + ph3_time;
				break;
			case STABLE:
				ph3_time = this.solveHeight(maxReqSpeed, req.getSizeLeft());
				currDepartInterval = ph3_time;
				break;
			}
			
			if(minDepartInterval==0){
				minDepartInterval = currDepartInterval;
				requests.add(req);
			}
			else if(currDepartInterval<minDepartInterval)
			{
				minDepartInterval = currDepartInterval;
				requests.clear();
				requests.add(req);
			}
			else if(currDepartInterval==minDepartInterval)
			{
				requests.add(req);
			}
		}
		nextDepartureTime = currTime + minDepartInterval;
		
		return requests;
	}

	
	private void updateRequest(long elapse,Request req,long globalMaxSpeed)
	{
		long timeAlive = req.getTimeAlive();
		long currMaxSpeed = Math.min(globalMaxSpeed, req.getMaxSpeed());
		
		switch(req.getState())
		{
		case INIT:
			long initleft = req.getConnEstTime() - timeAlive;
			long canrampup = elapse - initleft;
			if(canrampup>=0){
				long rampuptime = Math.min(canrampup, req.getRamupTime());
				long stabletime = canrampup-req.getRamupTime();
				if(stabletime>=0)
				{
					assert rampuptime == req.getRamupTime();
					//long currSpeed = currMaxSpeed;
					long rampuptrans = (long)((double)(this.calcTrapeZoidArea2(req.getInitSpeed(), currMaxSpeed, req.getRamupTime()))/2d);
					long stabletrans = this.calcRectangleArea(currMaxSpeed, stabletime);
					long sizeleft = req.getSizeLeft() - rampuptrans - stabletrans;
					assert sizeleft > 0;
					req.setState(Phase.STABLE);
					req.setSizeLeft(sizeleft);
					req.setCurrSpeed(currMaxSpeed);
					req.setTimeAlive(timeAlive + elapse);
				}
				else
				{
					double rate = (double)(currMaxSpeed-req.getInitSpeed())/(double)(req.getRamupTime());
					long currSpeed = req.getInitSpeed()+(long)((double)rampuptime*rate);
					assert currSpeed < currMaxSpeed;
					long rampuptrans = (long)((double)(this.calcTrapeZoidArea2(req.getInitSpeed(), currSpeed, rampuptime))/2d);
					long sizeleft = req.getSizeLeft() - rampuptrans;
					assert sizeleft > 0;
					req.setState(Phase.RAMPUP);
					req.setSizeLeft(sizeleft);
					req.setCurrSpeed(currSpeed);
					req.setTimeAlive(timeAlive + elapse);
				}
			}
			else {//still init
				req.setTimeAlive(timeAlive + elapse);
			}
			break;
		case RAMPUP:
			long rampupleft = req.getConnEstTime() + req.getRamupTime() - timeAlive;
			assert rampupleft > 0;
			long stabletime = elapse - rampupleft;
			if(stabletime>=0)
			{
				long rampuptrans = (long)((double)(this.calcTrapeZoidArea2(req.getCurrSpeed(), currMaxSpeed, rampupleft))/2d);
				long stabletrans = this.calcRectangleArea(currMaxSpeed, stabletime);
				long sizeleft = req.getSizeLeft() - rampuptrans - stabletrans;
				assert sizeleft > 0;
				req.setState(Phase.STABLE);
				req.setSizeLeft(sizeleft);
				req.setCurrSpeed(currMaxSpeed);
				req.setTimeAlive(timeAlive + elapse);
			}
			else
			{//still rampup
				double rate = (double)(currMaxSpeed - req.getCurrSpeed())/(double)(rampupleft);
				long currSpeed = req.getInitSpeed()+(long)((double)rampupleft*rate);
				assert currSpeed < currMaxSpeed;
				long rampuptrans = (long)((double)(this.calcTrapeZoidArea2(req.getCurrSpeed(), currSpeed, elapse))/2d);
				long sizeleft = req.getSizeLeft() - rampuptrans;
				assert sizeleft > 0;
				req.setSizeLeft(sizeleft);
				req.setCurrSpeed(currSpeed);
				req.setTimeAlive(timeAlive + elapse);
			}
			break;
		case STABLE:
			long stabletrans = this.calcRectangleArea(currMaxSpeed, elapse);
			long sizeleft = req.getSizeLeft() - stabletrans;
			req.setSizeLeft(sizeleft);
			req.setCurrSpeed(currMaxSpeed);
			req.setTimeAlive(timeAlive + elapse);
			//still stable
			break;
		}
		req.setTimeAlive(req.getTimeAlive()+elapse);
		assert req.getSizeLeft() > 0;
	}
	
	private void handleOutStandingRequests() 
	{
		log.debug("handling outstanding requests");
		while(true)
		{
			//calculate nextDepartureTime and mkae the hashset to be removed
			HashSet<Request> toRemove = this.getNextDepartureRequests();
			long elapse = nextDepartureTime - currTime;
			assert currLoad == (double)currRequests.size()/(double)maxConRequests;
			assert currPower == (idlePowerRate+(1-idlePowerRate)*currLoad)*maxPower;

			long globalMax = this.deteCurrGlobalMaxSpeed();
			//update each request in outstand request list
			for(Request req:currRequests)
			{
				if(!toRemove.contains(req)){
					this.updateRequest(elapse, req, globalMax);
				}
			}
			
			//update status 
			requestHandled += toRemove.size();
			currRequests.removeAll(toRemove);
			totalConsumption += currPower * elapse;
			currLoad = (double)currRequests.size()/(double)maxConRequests;
			currPower = (idlePowerRate+(1-idlePowerRate)*currLoad)*maxPower;
			currTime += elapse;
			currBW = 0;
			for(Request req:currRequests)
			{
				currBW += req.getCurrSpeed();
			}
			
			if(currRequests.size()==0) break; //all done!
		}
		log.debug("Done! Server Simulation finished..");
	}
	
	
	public static void main(String[] args) throws Exception
	{
		if(args.length == 0)
		{
			System.out.println("usage: ./java ServerNode [Name] [Location] [maxConcurrentRequest] [MaxBindWidth] [MaxPower] [IdlePowerRate] [Op[LBAddr:LBPort]]");
		}
		else 
		{
			String name = args[0];
			Location loc = Location.valueOf(args[1]);
			int maxConcRequests = Integer.parseInt(args[2]);  //500
			long maxBW = Long.parseLong(args[3]);//100*1000;  //bytes per millsecond  approx= 100 MegaBytes/s
			double maxPower = Double.parseDouble(args[4]);//500/1000;  //500 Watt  - unit J/millisecond
			double idlePowerRate = Double.parseDouble(args[5]); //0.7;  
			
			String lbAddr = args.length==6?LoadBalancer.DEFAULT_LB_ADDR:args[6].split(":")[0];
			int lbPort = args.length==6?LoadBalancer.DEFAULT_LB_PORT:Integer.parseInt(args[6].split(":")[1]);
			
			String filename = ".//report//"+name + "." + loc.name()+".report";
			PrintStream report = new PrintStream(new FileOutputStream(new File(filename)), true);
			
			ServerNode server = new ServerNode(name,loc,maxConcRequests,maxBW,maxPower,idlePowerRate, lbPort,lbAddr,report);
			server.register();
			server.running();
		}
	}
}