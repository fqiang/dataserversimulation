package acs.project.simulation.optimization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import acs.project.simulation.common.RequestEvent;
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
		ArrayList<RequestEvent> events = null;
		
		long min = 0;
		for(int i=0;i<traceReaders.size();i++)
		{
			BufferedReader reader = traceReaders.get(i);
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
			events = new ArrayList<RequestEvent>();
			for(BufferedReader r:readers)
			{
				do{
					r.mark(5000);
					String csvline = r.readLine();
					if(csvline==null) {
						break;
					}
					RequestEvent e = RequestEvent.fromString(csvline);
					if(e.getTime() == min){
						events.add(e);
					}
					else{
						assert e.getTime() > min : "e.currTime["+e.getTime()+"] min["+min+"]";
						r.reset();
						break;
					}
				} while(true);
			}
			return events.toArray(new RequestEvent[]{});
		}
	}
	
	public static void main(String[] args) throws IOException
	{
		EventFeeder ef = new EventFeeder();
		int i = 0; 
		while(true){
			int j=0;
			RequestEvent es[] = ef.nextEvents();
			for(RequestEvent e:es)
			{
				System.out.println("["+i+++"]["+j+++"]"+e.toString());
			}
		}
	}
}
