package acs.project.simulation.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

public class Location implements Serializable{
//	CHINA,
//	JAPAN,
//	ASIAN,
//	EUROPE,
//	AMERICAN
	@Order(value=1)
	private String name;
	@Order(value=2)
	private int id;
	
	private static boolean initialized = false;
	private static HashMap<String,Location> locations = null;
	public final static String filename = "./config/RequestInitProperty.csv";
	
	
	public static void init()
	{
		try {
    		if(!initialized){
        		locations = new HashMap<String,Location>();
        		BufferedReader reader = new BufferedReader(new FileReader(filename));
        		String csvline = reader.readLine();
        		String[] locs = csvline.split(",");
        		for(int i=1;i<locs.length;i++)
        		{
        			Location loc = new Location(locs[i],i-1);
        			System.out.println(loc + " | "+locs[i] );
        			locations.put(locs[i],loc);
        		}
    		}
    		initialized = true;
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public static Location valueOf(String loc) 
	{
		init();
		return locations.get(loc);
	}
	
	public static Location[] values() 
	{
		init();
		Location[] locs = new Location[locations.size()];
		int index = 0;
		for(Iterator<String> it = locations.keySet().iterator();it.hasNext();)
		{
			locs[index] = locations.get(it.next());
		}
		return locs;
	}
	
	public String name()
	{
		return this.getName();
	}
	public int ordinal()
	{
		return this.getId();
	}

	public String toString()
	{
		return this.getName();
	}
	
	private Location(String aName, int aId)
	{
		name = aName;
		id = aId;
	}

	private void setName(String name) {
		this.name = name;
	}

	private String getName() {
		return name;
	}

	private void setId(int id) {
		this.id = id;
	}

	private int getId() {
		return id;
	}
}
