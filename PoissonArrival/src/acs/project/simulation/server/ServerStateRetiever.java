package acs.project.simulation.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import acs.project.simulation.common.State;

public class ServerStateRetiever {
	
	private HashMap<String,State> statemap = null;
	public final static String filename = "./config/ServerStatesConfig.csv";
	
	public ServerStateRetiever() throws IOException
	{
		statemap = new HashMap<String,State>();
		BufferedReader reader =new BufferedReader(new FileReader(filename));
		String csvline = reader.readLine();  //get rid of the column name row;
		csvline = reader.readLine();
		do
		{
			String[] col = csvline.split(",");
			assert col.length == 6;
			String name = col[0];
			int maxC = Integer.parseInt(col[1]);
			long speed = Long.parseLong(col[2]);
			long maxBW = Long.parseLong(col[3]);
			double pr = Double.parseDouble(col[4]);
			double ipr = Double.parseDouble(col[5]);
			State newState = new State(name,maxC,speed,maxBW,pr,ipr);
			statemap.put(name, newState);
			csvline = reader.readLine();
		}while(csvline!=null);
	}

	public State getState(String name)
	{
		return statemap.get(name);
	}
	
}
