package acs.project.simulation.optimization.strategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import acs.project.simulation.common.RequestEvent;
import acs.project.simulation.common.ServerStatus;
import acs.project.simulation.common.StatusRequest;
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
		ArrayList<ServerProfile> local_servers = serverlist.get(event.getLocation().ordinal());
		for(ServerProfile server:local_servers)
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
		
		//if not found in local , now try nonlocal nodes
		for(ArrayList<ServerProfile> servers :serverlist)
		{
			for(ServerProfile server:servers)
			{
				if(server.getInfo().getLocation().ordinal()!=event.getLocation().ordinal())
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
			}
		}
		return profile;
	}
	
	@Override
	public void management(List<ArrayList<ServerProfile>> serverlist) throws IOException, ClassNotFoundException
	{
		
		for(Iterator<ArrayList<ServerProfile>> iter = serverlist.iterator();iter.hasNext();)
		{
			ArrayList<ServerProfile> servers = iter.next();
    		for(ServerProfile server:servers)
    		{
    			StatusRequest req = new StatusRequest();
    			server.getOos().writeObject(req);
    			ServerStatus status = (ServerStatus)server.getOis().readObject();
    			server.setStatus(status);
    			assert status.getCurrLoad() <= 1;
    		}
		}
	}

	@Override
	public void init(List<ArrayList<ServerProfile>> serverlist) {
		log.debug("Init all server nodes");
	}

}
