package acs.project.simulation.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import acs.project.simulation.common.Location;

public class RequestInitPropertyRetriever 
{
	private HashMap<LocationPair,RequestInitProperty> dataMap = null;
	public final static String filename = "./config/RequestInitProperty.csv";
	
	private class LocationPair
	{
		private Location loc1;
		private Location loc2;
		
		public LocationPair(Location l1,Location l2)
		{
			loc1 = l1;
			loc2 = l2;
		}
		
		public boolean equals(Object obj)
		{
			LocationPair p = (LocationPair)obj;
			if(p.loc1.equals(loc1) && p.loc2.equals(loc2))
			{
				return true;
			}
			else if(p.loc1.equals(loc2) && p.loc2.equals(loc1))
			{
				return true;
			}
			return false;
			
		}
		
		public int hashCode()
		{
			return loc1.ordinal()+loc2.ordinal();
		}
	}
	
	public RequestInitPropertyRetriever() throws IOException
	{
		dataMap = new HashMap<LocationPair, RequestInitProperty>();
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String csvline = reader.readLine();
		String[] col = csvline.split(",");
		assert col.length == Location.values().length + 1;
		int i = 1;
		csvline = reader.readLine();
		while(i<col.length)
		{
			 String[] row = csvline.split(",");
			 Location loc = Location.valueOf(row[0]);
			 for(int j=i;j<col.length;j++)
			 {
				 String[] value = row[j].split(":");
				 RequestInitProperty prop = new RequestInitProperty();
				 prop.setConnEstTime(Long.parseLong(value[0]));
				 prop.setRampUpTime(Long.parseLong(value[1]));
				 prop.setSpeedInit(Long.parseLong(value[2]));
				 prop.setSpeedLimit(Long.parseLong(value[3]));
				 
				 LocationPair key = new LocationPair(Location.valueOf(col[j]),loc);
				 assert dataMap.get(key) == null;
				 //System.out.println("add:["+key.loc1.name()+", "+key.loc2.name()+"]");
				 dataMap.put(key , prop);
			 }
			 csvline = reader.readLine();
			 i++;
		}
	}

	public RequestInitProperty getProperty(Location loc1,Location loc2)
	{
		return dataMap.get(new LocationPair(loc1,loc2));
	}
	
	public int getNumOfProperties()
	{
		return dataMap.size();
	}
	
	public static void main(String args[]) throws IOException
	{
		RequestInitPropertyRetriever rev = new RequestInitPropertyRetriever();
		System.out.println("size : "+rev.getNumOfProperties());
		RequestInitProperty prop = rev.getProperty(Location.JAPAN, Location.ASIAN);
		System.out.println(prop.getConnEstTime());
	}
	
}

