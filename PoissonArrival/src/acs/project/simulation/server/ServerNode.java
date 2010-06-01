package acs.project.simulation.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.Vector;

import org.apache.log4j.*;

import acs.project.simulation.common.Location;
import acs.project.simulation.common.RequestEvent;
import acs.project.simulation.common.ServerConfigInfo;
import acs.project.simulation.common.ServerStatus;
import acs.project.simulation.common.SimulationEnd;
import acs.project.simulation.common.State;
import acs.project.simulation.common.StateChangeRequest;
import acs.project.simulation.common.StatusRequest;
import acs.project.simulation.common.TimeStamp;
import acs.project.simulation.optimization.LoadBalancer;

public class ServerNode {
	
	public final static Logger log = Logger.getLogger(ServerNode.class);
	
	//simulation connections
	private Socket socket = null;    //the communication socket with LB
	private ObjectOutputStream oos = null;
	private ObjectInputStream  ois = null;
	private int lbPort;
	private String lbAddr;
	
	//system property
	private String serverName;
	private Location location;	
	private double maxPower;  //in J/millisecond
		
	//report printer
	private PrintStream reportPrinter;
	
	//system status
	private Vector<Request> currRequests = null;
	private long requestRecv;
	private long requestDiscard;
	private long requestHandled;
	private State state = null;
	private long currTime = 0;          //simulation time
	private long  currBW = 0;       //in byte       = sum of all req.getCurrSpeed()
	private double currPower = 0;   //in Joule per millisecond = (idelPowerRate + (1-idelPowerRate)*currLoad)*maxPower
	private double currLoad = 0;    // the req load = currRequest.size()/maxConRequest
	private double totalConsumption = 0;  //in Joule
	private double totalEnvCost = 0;
	private EnergyType currEnergy = null;

	//global temporary variable
	private long nextDepartureTime = 0; //next request(s) depature time
	
	//helper obj
	private RequestInitPropertyRetriever propRet = null;
	private ServerStateRetiever stateRet = null;
	private EnergyProvider engProvider = null;
	
	public ServerNode(String name,Location loc,double max_power,int lb_port,String lb_addr,PrintStream report) throws IOException
	{
		//helper obj init
		propRet = new RequestInitPropertyRetriever();
		stateRet = new ServerStateRetiever();
		engProvider = new EnergyProvider();
		
		//system config init
		this.serverName = name;
		this.location = loc;
		this.maxPower = max_power;
		this.lbPort = lb_port;
		this.lbAddr = lb_addr;
		this.reportPrinter = report;
		
		//system status init
		this.currRequests = new Vector<Request>();
		this.requestRecv = 0;
		this.requestDiscard = 0;
		this.requestHandled = 0;
		this.state = stateRet.getState("RUNNING");
		this.currTime = 0;
		this.currBW = 0;
		this.currLoad = 0;
		this.currPower = calcPower(maxPower,currLoad,state);
		this.totalConsumption = 0;
		this.totalEnvCost = 0;
		this.currEnergy = engProvider.getEnergyType(currTime);
	}

	private static double calcPower(double maxP,double load,State state) {
		return (maxP*state.getPowerRate())*(state.getIdlePowerRate()+(1-state.getIdlePowerRate())*load);
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
		info.setMaxPower(maxPower);
		return info;
	}
	
	public void running() throws IOException, ClassNotFoundException 
	{
		log.debug("Server runnning ...localport["+this.socket.getLocalPort()+"]");
		
		while(true) 
		{
			Object obj = this.ois.readObject();
			if(obj instanceof StatusRequest)
			{//Query effect
				log.debug("[StatusRequest]  - CurrTime["+currTime+"]");
				//StatusRequest req = (StatusRequest)obj;
				ServerStatus status = createServerStatus();
				this.oos.writeObject(status);
			}
			else if(obj instanceof StateChangeRequest)
			{//instantaneous effect
				StateChangeRequest req = (StateChangeRequest)obj;
				log.debug("[StateChangeRequest] serverName["+serverName+"] requestState["+req.getRequestState()+"] currState["+state.getName()+"] currTime["+currTime+"]");
				handleStateChangeRequest(req);
			}
			else if(obj instanceof RequestEvent)
			{//instantaneous effect
				RequestEvent event = (RequestEvent)obj;
				log.debug("[RequestArrivalEvent] - CurrTime["+currTime+"]    Event["+event.toString()+"]" );
				assert currTime == event.getTime();
				assert !state.getName().equals("SLEEP");
				handleIncomingEvent(event);
			}
			else if(obj instanceof TimeStamp)
			{//time elapse effect
				TimeStamp stamp = (TimeStamp)obj;
				long synchTime = stamp.getCurrTime();
				assert synchTime > currTime : "synchTime="+synchTime + "currTime="+currTime;
				log.debug("[TimeStamp] - TimeStamp["+synchTime+"] CurrTime["+currTime+"] CurrLoad["+currLoad+"] reqHandled["+requestHandled+"] outstandReq["+currRequests.size()+"]");
				handleTimeStamp(synchTime);
			}
			else if(obj instanceof SimulationEnd)
			{
				log.debug("[SimulationEnd]- At State:["+state.toString()+"]");
				log.debug("[SimulationEnd]- OutstandingRequest["+this.currRequests.size()+"]");
				break;
			}
		}
		
		handleOutStandingRequests();
		cleanup();
	}

	private void handleStateChangeRequest(StateChangeRequest req) 
	{
		assert !state.getName().equals(req.getRequestState()) : "state["+state.getName()+"] req["+req.getRequestState()+"]";
		//update state
		state = stateRet.getState(req.getRequestState());
		if(currRequests.size() <= state.getMaxConRequests())
		{//no need to discard curr request
		}
		else
		{//discard some of the current requests
			long discard = currRequests.size() - state.getMaxConRequests();
			assert discard > 0  : "Discard:["+discard+"]";
			currRequests.subList(state.getMaxConRequests(), currRequests.size()).clear();
			assert currRequests.size() == state.getMaxConRequests() : currRequests.size() + "|" + state.getMaxConRequests();
			requestDiscard += discard;	
		}
		//state = newState;
		nextDepartureTime = 0;
		currLoad = calcLoad(currRequests.size(),state);
		currPower = calcPower(maxPower,currLoad,state);
		currBW = 0;
	}

	private static double calcLoad(int requestSize,State state) {
		return state.getMaxConRequests()==0?0:(double)requestSize / (double)state.getMaxConRequests();
	}

	private void handleTimeStamp(long synchTime) 
	{
		if(!state.getName().equals("SLEEP"))
		{
			assert state.getName().equals("RUNNING");
			while(true) 
			{
    			if(currTime==synchTime)  break;
    			
    			HashSet<Request> toRemove = this.getNextDepartureRequests();
    			long nextDepartTime = toRemove.size()==0?Long.MAX_VALUE:nextDepartureTime;
    			long nextEnergyTime = engProvider.nextEnergyStartTime(currTime);
    			long minTime = Math.min(Math.min(nextDepartTime, synchTime),nextEnergyTime);
    			
    			if(minTime == nextDepartTime) 
    			{//depart requests in toRemove		
    				long elapse = nextDepartTime - currTime;
    				assert elapse > 0;
    				assert currLoad == (double)currRequests.size()/(double)state.getMaxConRequests();
    				assert currPower == calcPower(maxPower,currLoad,state);
    				
    				long globalMax = this.deteCurrGlobalMaxSpeed();
    				//update each request in outstanding request list
    				for(Request req:currRequests)
    				{
    					if(!toRemove.contains(req)){
    						this.updateRequest(elapse, req, globalMax);
    					}
    				}
    				
    				//update status 
    				requestHandled += toRemove.size();
    				currRequests.removeAll(toRemove);
    				totalEnvCost += currPower * elapse * currEnergy.getGhRate();
    				totalConsumption += currPower * elapse;
    				currTime += elapse;
    				//update load power bw
    				currLoad = (double)currRequests.size()/(double)state.getMaxConRequests();
    				currPower = calcPower(maxPower,currLoad,state);
    				currBW = 0;
    				for(Request req:currRequests)
    				{
    					currBW += req.getCurrSpeed();
    				}
    				assert currTime == nextDepartTime;
    			}
   
    			if(minTime == synchTime)
    			{//proceed to synchTime
    				//assert no more departure before synchTime
    				assert nextDepartTime >= synchTime;
    				long elapse = synchTime - currTime;
    				if(elapse!=0) {
        				long globalMaxSpeed = this.deteCurrGlobalMaxSpeed();
        				for(Request req:currRequests)
        				{
        					this.updateRequest(elapse, req, globalMaxSpeed);
        				}
        				//update status 
        				totalEnvCost += currPower * elapse * currEnergy.getGhRate();
        				totalConsumption += currPower * elapse;
        				currTime += elapse;
        				currBW = 0;
        				for(Request req:currRequests)
        				{
        					currBW += req.getCurrSpeed();
        				}
    				}
    				assert currTime == synchTime;
    			}
    			
    			if(minTime == nextEnergyTime)
    			{//proceed to nextEnergyTime and switch energy at the end
    				//assert no more departure or before nextEnergyTime
    				assert nextDepartTime >= nextEnergyTime;
    				long elapse = nextEnergyTime - currTime;
    				if(elapse!=0){
        				long globalMaxSpeed = this.deteCurrGlobalMaxSpeed();
        				for(Request req:currRequests)
        				{
        					this.updateRequest(elapse, req, globalMaxSpeed);
        				}
        				//update status 
        				totalEnvCost += currPower * elapse * currEnergy.getGhRate();
        				totalConsumption += currPower * elapse;
        				currTime += elapse;
        				currBW = 0;
        				for(Request req:currRequests)
        				{
        					currBW += req.getCurrSpeed();
        				}
    				}
    				assert currTime == nextEnergyTime;	
    				EnergyType newEnergy = engProvider.getEnergyType(currTime);
    				assert !newEnergy.getName().equals(currEnergy.getName());
    				currEnergy = newEnergy;
    			}
			}
		}
		else
		{
			while(true) 
			{
    			assert state.getName().equals("SLEEP");
    			assert currRequests.size() == 0;
    			assert nextDepartureTime == 0;
    			assert currBW == 0;
    			assert currLoad == 0;
    			assert currPower == calcPower(maxPower,currLoad,state);
    			
    			if(currTime == synchTime) break;
    			
    			long nextEnergyTime = engProvider.nextEnergyStartTime(currTime);
    			long minTime = Math.min(synchTime, nextEnergyTime);
    			
    			if(minTime == synchTime) 
    			{
        			long elapse = synchTime - currTime;	
        			//update state
        			totalEnvCost += currPower * elapse * currEnergy.getGhRate();
        			totalConsumption += currPower * elapse;
        			currTime += elapse;
        			assert currTime == synchTime;
    			}
    			
    			if(minTime == nextEnergyTime)
    			{
    				long elapse = synchTime - currTime;	
        			//update state
        			totalEnvCost += currPower * elapse * currEnergy.getGhRate();
        			totalConsumption += currPower * elapse;
        			currTime += elapse;
        			
        			assert currTime == nextEnergyTime;	
        			EnergyType newEnergy = engProvider.getEnergyType(currTime);
    				assert !newEnergy.getName().equals(currEnergy.getName());
    				currEnergy = newEnergy;
    			}
			}
		}
	}
	private void handleOutStandingRequests() 
	{
		log.debug("handling outstanding requests");
		while(true)
		{
			if(currRequests.size()==0) break; //no outstanding requests
			
			//calculate nextDepartureTime and make the hashset to be removed
			HashSet<Request> toRemove = this.getNextDepartureRequests();
			long nextDepartTime = toRemove.size()==0?Long.MAX_VALUE:nextDepartureTime;
			long nextEnergyTime = engProvider.nextEnergyStartTime(currTime);
			long minTime = Math.min(nextDepartTime, nextEnergyTime);
			
			if(minTime==nextDepartTime) {
    			long elapse = nextDepartTime - currTime;
    			assert (elapse==0 && toRemove.size()==0) || (elapse!=0 && toRemove.size()!=0);
    			assert currLoad == (double)currRequests.size()/(double)state.getMaxConRequests();
    			assert currPower == calcPower(maxPower,currLoad,state);
    
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
    			totalEnvCost += currPower * elapse * currEnergy.getGhRate();
    			totalConsumption += currPower * elapse;
    			currLoad = (double)currRequests.size()/(double)state.getMaxConRequests();
    			currPower = calcPower(maxPower,currLoad,state);
    			currTime += elapse;
    			currBW = 0;
    			for(Request req:currRequests)
    			{
    				currBW += req.getCurrSpeed();
    			}
    			assert currTime == nextDepartTime;
			}
			if(minTime==nextEnergyTime)
			{
				assert nextDepartTime >= nextEnergyTime;
				long elapse = nextEnergyTime - currTime;
				if(elapse!=0){
    				long globalMaxSpeed = this.deteCurrGlobalMaxSpeed();
    				for(Request req:currRequests)
    				{
    					this.updateRequest(elapse, req, globalMaxSpeed);
    				}
    				//update status 
    				totalEnvCost += currPower * elapse * currEnergy.getGhRate();
    				totalConsumption += currPower * elapse;
    				currTime += elapse;
    				currBW = 0;
    				for(Request req:currRequests)
    				{
    					currBW += req.getCurrSpeed();
    				}
				}
				EnergyType newEnergy = engProvider.getEnergyType(currTime);
				assert !newEnergy.getName().equals(currEnergy.getName());
				currEnergy = newEnergy;
				assert currTime == nextEnergyTime;	
			}	
		}
		log.debug("Done! Server Simulation finished..");
	}
	private ServerStatus createServerStatus() 
	{
		assert requestRecv == requestDiscard + requestHandled + currRequests.size();
		ServerStatus status = new ServerStatus();
		status.setCurrNumReqs(currRequests.size());
		status.setRequestRecv(requestRecv);
		status.setRequestDiscard(requestDiscard);
		status.setRequestHandled(requestHandled);
		status.setState(state);
		status.setCurrTime(currTime);
		status.setCurrBW(currBW);
		status.setCurrPower(currPower);
		status.setCurrLoad(currLoad);
		status.setCurrTolConsumption(totalConsumption);
		status.setCurrEnvCost(totalEnvCost);
		status.setEnergy(currEnergy);
		return status;
	}
	
	private void cleanup() throws IOException
	{
		//send last status
		ServerStatus status = createServerStatus();
		this.oos.writeObject(status);
		
		this.socket.close();
		this.oos.close();
		this.ois.close();
		
		reportPrinter.println("============================================");
		reportPrinter.println("Simulation Finished - at "+currTime);
		reportPrinter.println("Total Consumption:" + totalConsumption);
		reportPrinter.println("Total Environment Cost:" + totalEnvCost);
		reportPrinter.println("Final Server Status:["+status.toString()+"]");
		reportPrinter.println("============================================");
	}

	private void handleIncomingEvent(RequestEvent event) 
	{
		assert currRequests.size()<state.getMaxConRequests();
		RequestInitProperty prop = propRet.getProperty(location, event.getLocation());
		Request request = new Request(event,prop.getConnEstTime(),prop.getRampUpTime(),prop.getSpeedLimit(),prop.getSpeedInit());
		
		//update status
		currRequests.add(request);
		requestRecv++;
		currLoad = (double)currRequests.size() / (double)state.getMaxConRequests();
		currPower = calcPower(maxPower,currLoad,state);
		currBW = 0;
		for(Request req:currRequests)
		{
			currBW += req.getCurrSpeed();
		}
	}
	
	private long deteCurrGlobalMaxSpeed()
	{
		long globalMaxSpeed = 0;
		if(currRequests.size()==0){
			globalMaxSpeed = state.getSpeedLimit();
		}
		else
		{
			globalMaxSpeed = (long)((double)state.getMaxBW()/(double)currRequests.size());
			globalMaxSpeed = Math.min(globalMaxSpeed, state.getSpeedLimit());
		}
		return globalMaxSpeed;
	}
	
	//formula by quadratic equation, area2 = 2*area
	private static long solveHeight(long a, long area2,double rate)
	{
		double da = (double)a;
		double darea2 = (double)area2;
		double time = (Math.sqrt(da*da+darea2*rate)-da)/rate;
		return (long)time;
	}
	
	private static long calcTrapeZoidArea2(long a,long b,long h)
	{
		return (a+b)*h;
	}
	
	private static long solveHeight(long a,long area)
	{
		double darea =(double)area;
		double da = (double)a;
		return (long)(darea/da);
	}
	
	private static long calcRectangleArea(long a,long b)
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
			log.debug("[getNextDepartureRequests] -Request:["+req.toString()+"]");
			
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
				long cantrans2 = calcTrapeZoidArea2(req.getInitSpeed(), maxReqSpeed, req.getRamupTime());
				long actualSize2 = 2*req.getSizeLeft();
				if(cantrans2>=actualSize2){ 
				//finish in phase 2
					double rate = (double)(maxReqSpeed-req.getInitSpeed())/(double)(req.getRamupTime());
					ph2_time = solveHeight(req.getInitSpeed(), actualSize2, rate);
					assert ph2_time <= req.getRamupTime() && ph2_time>=0;
				}
				else {
					ph2_time = req.getRamupTime();
					long leftSize = req.getSizeLeft() - (long)(cantrans2/2);
					assert leftSize > 0;
					ph3_time = solveHeight(maxReqSpeed, leftSize);
				}
				currDepartInterval = ph1_time + ph2_time + ph3_time;
				break;
			case RAMPUP:
				long rsizeleft2 = 2*req.getSizeLeft();
				long rrampupleft = req.getConnEstTime()+req.getRamupTime() - reqAlive;
				long rcurrSpeed = req.getCurrSpeed()==0?req.getInitSpeed():req.getCurrSpeed();
				long rtrans2 = calcTrapeZoidArea2(rcurrSpeed, maxReqSpeed, rrampupleft);
				if(rtrans2 >= rsizeleft2)
				{//finish at phase 2
					double rate = (double)(maxReqSpeed-rcurrSpeed)/(double)(rrampupleft);
					ph2_time = solveHeight(rcurrSpeed, rsizeleft2, rate);
					assert ph2_time >= 0 && ph2_time <= rrampupleft;
				}
				else
				{//to phase 3
					ph2_time = rrampupleft;
					long rleft =(long)((rsizeleft2 - rtrans2)/2d);
					ph3_time = solveHeight(maxReqSpeed, rleft);
					assert ph3_time >= 0;
				}
				currDepartInterval = ph2_time + ph3_time;
				break;
			case STABLE:
				ph3_time = solveHeight(maxReqSpeed, req.getSizeLeft());
				currDepartInterval = ph3_time;
				break;
			}
			log.debug("currDepartInverval["+currDepartInterval+"]");
			//calculating minimum
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
		log.debug("nextDepartureTime["+nextDepartureTime+"] currTime["+currTime+"] minDepartInterval["+minDepartInterval+"]");
		assert (requests.size()==0 && minDepartInterval ==0 ) || (requests.size()!=0 && minDepartInterval!=0);
		return requests;
	}

	
	private void updateRequest(long elapse,Request req,long globalMaxSpeed)
	{
		long timeAlive = req.getTimeAlive();
		long currMaxSpeed = Math.min(globalMaxSpeed, req.getMaxSpeed());
		
		log.debug("[UpdateRequest][BEFORE][elapse="+elapse +"]:"+req.toString());
		
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
					long rampuptrans = (long)((double)(calcTrapeZoidArea2(req.getInitSpeed(), currMaxSpeed, req.getRamupTime()))/2d);
					long stabletrans = calcRectangleArea(currMaxSpeed, stabletime);
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
					long rampuptrans = (long)((double)(calcTrapeZoidArea2(req.getInitSpeed(), currSpeed, rampuptime))/2d);
					long sizeleft = req.getSizeLeft() - rampuptrans;
					assert sizeleft > 0;
					assert timeAlive + elapse < req.getConnEstTime() + req.getRamupTime();
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
				long rampuptrans = (long)((double)(calcTrapeZoidArea2(req.getCurrSpeed(), currMaxSpeed, rampupleft))/2d);
				long stabletrans = calcRectangleArea(currMaxSpeed, stabletime);
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
				long currSpeed = req.getCurrSpeed()+(long)((double)elapse*rate);
				assert currSpeed < currMaxSpeed;
				long rampuptrans = (long)((double)(calcTrapeZoidArea2(req.getCurrSpeed(), currSpeed, elapse))/2d);
				long sizeleft = req.getSizeLeft() - rampuptrans;
				assert sizeleft > 0: "sizeleft["+sizeleft+"]";
				req.setSizeLeft(sizeleft);
				req.setCurrSpeed(currSpeed);
				req.setTimeAlive(timeAlive + elapse);
			}
			break;
		case STABLE:
			long stabletrans = calcRectangleArea(currMaxSpeed, elapse);
			long sizeleft = req.getSizeLeft() - stabletrans;
			req.setSizeLeft(sizeleft);
			req.setCurrSpeed(currMaxSpeed);
			req.setTimeAlive(timeAlive + elapse);
			//still stable
			break;
		}
		log.debug("[UpdateRequest][AFTER][elapse="+elapse +"]:"+req.toString());
		assert req.getSizeLeft() > 0;
	}
	
	public static void main(String[] args) throws Exception
	{
		if(args.length == 0)
		{
			System.out.println("usage: ./java ServerNode [Name] [Location] [MaxPower] [Op[LBAddr:LBPort]]");
		}
		else 
		{
			String name = args[0];
			Location loc = Location.valueOf(args[1]);
			double maxPower = Double.parseDouble(args[2]);//500/1000;  //500 Watt  - unit J/millisecond
			
			String lbAddr = args.length==3?LoadBalancer.DEFAULT_LB_ADDR:args[3].split(":")[0];
			int lbPort = args.length==3?LoadBalancer.DEFAULT_LB_PORT:Integer.parseInt(args[3].split(":")[1]);
			
			String filename = ".//report//"+name + "." + loc.name()+".report";
			PrintStream report = new PrintStream(new FileOutputStream(new File(filename)), true);
			
			ServerNode server = new ServerNode(name,loc,maxPower,lbPort,lbAddr,report);
			server.register();
			server.running();
		}
	}
}