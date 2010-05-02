package acs.project.simulation.optimization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import acs.project.simulation.dataset.common.RequestEvent;
import acs.project.simulation.optimization.TraceFileFilter;

public class EventFeeder {
	
	public String trancedir = "./trace";
	
	private ArrayList<BufferedReader> traceReaders = new ArrayList<BufferedReader>();
	//private ArrayList<Long> traceTimes = new ArrayList<Long>();
	 
	public EventFeeder() throws FileNotFoundException
	{
		File dir = new File(trancedir);
		File[] files = dir.listFiles(new TraceFileFilter());
		for(File file:files)
		{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			traceReaders.add(reader);
			//traceTimes.add(new Long(0));
		}
	}
	
	public RequestEvent[] nextEvents() throws IOException
	{
		ArrayList<BufferedReader> readers = new ArrayList<BufferedReader>();
		RequestEvent[] events = null;
		
		long min = 0;
		//int min_index = 0;
		for(int i=0;i<traceReaders.size();i++)
		{
			BufferedReader reader = traceReaders.get(i);
			//long time = traceTimes.get(i).longValue();
			reader.mark(5000);
			String csvline = reader.readLine();
			if(csvline != null)
			{
					
				RequestEvent event = RequestEvent.fromString(csvline);
				long time = event.getTime();
				
				if(min==0)
				{
					min = event.getTime();
					readers.add(reader);
				}
				else if(time<min)
				{
					readers.clear();
					readers.add(reader);
					min = time;
				}
				else if(time==min)
				{
					readers.add(reader);
				}	
				reader.reset();
			}
		}
		
		if(readers.size()==0)
		{
			assert min==0;
			return new RequestEvent[]{};//no more event
		}
		else
		{
			events = new RequestEvent[readers.size()];
			
			for(int i=0;i<events.length;i++)
			{
				events[i] = RequestEvent.fromString(readers.get(i).readLine());
			}
			return events;
		}
	}
}
