package acs.project.simulation.optimization.strategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import acs.project.simulation.common.RequestEvent;
import acs.project.simulation.common.ServerStatus;
import acs.project.simulation.common.StateChangeRequest;
import acs.project.simulation.common.StatusRequest;
import acs.project.simulation.optimization.ServerProfile;

public class SustainableStrategy implements EnergyAwareStrategyInterface{

	public final static Logger log = Logger.getLogger(SimpleStrategy.class);
	
	@Override
	public void init(List<ArrayList<ServerProfile>> serverlist) throws IOException {
		for(ArrayList<ServerProfile> servers:serverlist)
		{
			ServerProfile server0 = servers.get(0);
			StateChangeRequest reql1 = new StateChangeRequest("RUNNING_L1");
			server0.getOos().writeObject(reql1);
			
			ServerProfile server1 = servers.get(1);
			StateChangeRequest reqsleep = new StateChangeRequest("SLEEP");
			server1.getOos().writeObject(reqsleep);
		}
	}

	@Override
	public void management(List<ArrayList<ServerProfile>> serverlist) throws IOException, ClassNotFoundException {
		double guard = 0.90d;
		for(ArrayList<ServerProfile> servers:serverlist)
		{
			ServerProfile server0 = servers.get(0);
			ServerProfile server1 = servers.get(1);
			StatusRequest req_s0 = new StatusRequest();
			server0.getOos().writeObject(req_s0);
			ServerStatus status0 = (ServerStatus)server0.getOis().readObject();
			server0.setStatus(status0);
			
			if(status0.getCurrLoad()>=guard && status0.getState().getName().equals("RUNNING_L1")){
				StateChangeRequest req_r0 = new StateChangeRequest("RUNNING");
				server0.getOos().writeObject(req_r0);
			}
			else if(status0.getCurrLoad()>=guard && status0.getState().getName().equals("RUNNING"))
			{
				StatusRequest req_s1 = new StatusRequest();
				server1.getOos().writeObject(req_s1);
				ServerStatus status1 = (ServerStatus)server1.getOis().readObject();
				server1.setStatus(status1);
				
				if(status1.getState().getName().equals("SLEEP"))
				{
					StateChangeRequest req_rl1 = new StateChangeRequest("RUNNING_L1");
					server1.getOos().writeObject(req_rl1);
				}
				else if(status1.getState().getName().equals("RUNNING_L1") && status1.getCurrLoad() >= guard)
				{
					StateChangeRequest req_r1 = new StateChangeRequest("RUNNING");
					server1.getOos().writeObject(req_r1);
				}
				else
				{
					log.debug("95-95 Reached! - Location["+server0.getInfo().getLocation().name()+"]");
				}
			}
			else if(status0.getCurrLoad() < guard )
			{
				StatusRequest req_s1 = new StatusRequest();
				server1.getOos().writeObject(req_s1);
				ServerStatus status1 = (ServerStatus)server1.getOis().readObject();
				if(status1.getCurrLoad()==0d && !status1.getState().getName().equals("SLEEP"))
				{
					assert status1.getCurrNumReqs() == 0;
					StateChangeRequest req_sleep = new StateChangeRequest("SLEEP");
					server1.getOos().writeObject(req_sleep);
				}
			}
		}
		
	}

	@Override
	public ServerProfile selectServer(List<ArrayList<ServerProfile>> serverlist, RequestEvent event) throws IOException, ClassNotFoundException {
		
		ArrayList<ServerProfile> best_servers = serverlist.get(0);
		double curr_cost = serverlist.get(0).get(0).getStatus().getEnergy().getGhRate();
		boolean all_equal = true;
		
		for(ArrayList<ServerProfile> servers:serverlist)
		{
			double prev_cost = curr_cost;
			
			ServerProfile server0 = servers.get(0);
			curr_cost = server0.getStatus().getEnergy().getGhRate();
			
			double min_cost = Math.min(prev_cost,curr_cost);
			
			if(min_cost==prev_cost && min_cost == curr_cost)
			{
				
			}
			else if(min_cost == curr_cost)
			{
				best_servers = servers;
				all_equal = false;
			}
			else if(min_cost == prev_cost)
			{
				all_equal = false;
			}
		}
		
		if(all_equal)
		{
			best_servers = serverlist.get(event.getLocation().ordinal());
		}
		
		ServerProfile server0 = best_servers.get(0);
		if(server0.getStatus().getCurrLoad()<1)
		{
			return server0;
		}
		else
		{
			assert server0.getStatus().getCurrLoad()==1;
			ServerProfile server1 = best_servers.get(1);
			if(server1.getStatus().getCurrLoad()<1)
			{
				return server1;
			}
			log.debug("Server Busy - Location["+server1.getInfo().getLocation().name()+"]");
		}
		return null;
	}

}
