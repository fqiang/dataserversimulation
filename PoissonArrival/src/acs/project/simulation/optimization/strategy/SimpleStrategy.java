package acs.project.simulation.optimization.strategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import acs.project.simulation.dataset.common.RequestEvent;
import acs.project.simulation.dataset.common.ServerStatus;
import acs.project.simulation.dataset.common.StatusRequest;
import acs.project.simulation.optimization.LoadBalancer;
import acs.project.simulation.optimization.ServerProfile;

public class SimpleStrategy implements EnergyAwareStrategyInterface {

	public final static Logger log = Logger.getLogger(SimpleStrategy.class);
	
	@Override
	public ServerProfile selectServer(List<ArrayList<ServerProfile>> serverlist, RequestEvent event) throws IOException, ClassNotFoundException
	{
		/*
		 * this is a very simple strategy finding a free server in the area(location)
		 */
		ServerProfile profile = null;
		ArrayList<ServerProfile> servers = serverlist.get(event.getLocation().ordinal());
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

}
