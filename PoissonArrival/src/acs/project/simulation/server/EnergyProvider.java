package acs.project.simulation.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


public class EnergyProvider {
	
	
	public final static String filename = "./config/EnergySupplyData.csv";
	private ArrayList<EnergySupply> supplies = null;
	
	public EnergyProvider() throws IOException
	{
		supplies = new ArrayList<EnergySupply>();
		BufferedReader reader =new BufferedReader(new FileReader(filename));
		String csvline = reader.readLine();  //get rid of the column name row;
		csvline = reader.readLine();
		do
		{
			String[] col = csvline.split(",");
			assert col.length == 3;
			String name = col[0];
			long startTime = Long.parseLong(col[1]);
			double costRate = Double.parseDouble(col[2]);			
			EnergySupply energySup = new EnergySupply(new EnergyType(name,costRate),startTime);
			supplies.add(energySup);
			csvline = reader.readLine();
		}while(csvline!=null);
	}
	
	public long nextEnergyStartTime(long currTime)
	{
		int index = supplies.indexOf(getEnergySupply(currTime));
		assert index < supplies.size() && index >= 0;
		index ++; //proceed to next EnergySupply
		if(index == supplies.size())
		{
			return Long.MAX_VALUE;
		}
		else
		{
			return supplies.get(index).startTime;
		}
	}
	
	public EnergyType getEnergyType(long currTime)
	{
		EnergySupply energySup = getEnergySupply(currTime);
		return energySup.energy;
	}

	private EnergySupply getEnergySupply(long currTime) 
	{
		int currIndex = 0;
		EnergySupply curr = supplies.get(currIndex);
		EnergySupply next = supplies.size()==1?null:supplies.get(currIndex+1);
		while(true)
		{
			if(next==null)
			{
				return curr;
			}
			if(curr.startTime <= currTime && currTime < next.startTime)
			{
				return curr;
			}
			currIndex++;
			curr = supplies.get(currIndex);
			next = supplies.size()-1==currIndex?null:supplies.get(currIndex+1);
		}
	}
	
	public static void main(String args[]) throws IOException
	{
		EnergyProvider pro = new EnergyProvider();
		
		long testTime = 0;
		EnergyType eng = pro.getEnergyType(testTime);
		System.out.println(testTime+"|| "+eng.getName() + "||" +eng.getGhRate() + "||" +pro.nextEnergyStartTime(testTime));

		testTime = 34222;
		eng = pro.getEnergyType(testTime);
		System.out.println(testTime+"|| "+eng.getName() + "||" +eng.getGhRate() + "||" +pro.nextEnergyStartTime(testTime));
		
		testTime = 1800000;
		eng = pro.getEnergyType(testTime);
		System.out.println(testTime+"|| "+eng.getName() + "||" +eng.getGhRate() + "||" +pro.nextEnergyStartTime(testTime));

		testTime = 1900000;
		eng = pro.getEnergyType(testTime);
		System.out.println(testTime+"|| "+eng.getName() + "||" +eng.getGhRate() + "||" +pro.nextEnergyStartTime(testTime));

		testTime = 3600000;
		eng = pro.getEnergyType(testTime);
		System.out.println(testTime+"|| "+eng.getName() + "||" +eng.getGhRate() + "||" +pro.nextEnergyStartTime(testTime));

		testTime = 3600001;
		eng = pro.getEnergyType(testTime);
		System.out.println(testTime+"|| "+eng.getName() + "||" +eng.getGhRate() + "||" +pro.nextEnergyStartTime(testTime));
		
	}
	
	class EnergySupply{
		
		EnergyType energy;
		long startTime;
		
		public EnergySupply(EnergyType en,long time)
		{
			energy = en;
			startTime = time;
		}
	}
}
