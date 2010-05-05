package acs.project.simulation.dataset.strategy;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class RequestEventGenerationStrategy {
	
	public final Logger log  = Logger.getLogger(RequestEventGenerationStrategy.class);
	
	public final static long DEFAULT_SIM_END_TIME = 3600*1000;  //1 hr
	public final static long DEFAULT_RATE_INTERVAL = 1800*1000; //half hr
	private final static double[] DEFAULT_LAMDAS = { 0.01d, 0.02d };
	
	//properties
	private ArrayList<Double> lamdas = new ArrayList<Double>();
	private Iterator<Double> iter = null;
	
	//state
	private long simEndTime = DEFAULT_SIM_END_TIME;
	private long rateInterval = DEFAULT_RATE_INTERVAL;
	private long nextRateTime = 0;
	
	public RequestEventGenerationStrategy()
	{
		init(DEFAULT_LAMDAS,DEFAULT_SIM_END_TIME,DEFAULT_RATE_INTERVAL);
	}
	
	public RequestEventGenerationStrategy(double[] lamdas, long endtime, long interval)
	{
		init(lamdas,endtime,interval);
	}
	
	private void init(double[] lamdas,long endtime,long interval)
	{
		for(double d:lamdas)
		{
			this.lamdas.add(new Double(d));
		}
		iter = this.lamdas.iterator();
		simEndTime = endtime;
		rateInterval = interval;
		nextRateTime += rateInterval;
		assert this.lamdas.size()==simEndTime/rateInterval;
	}
	
	public boolean isGenerationEnd(long currTime)
	{
		if(currTime < simEndTime)
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	public double  getNextLamda()
	{
		return iter.next().doubleValue();
	}
	public boolean isProceedToNextArrivalRate(long currtime)
	{
		if(currtime < nextRateTime)
		{
			return false;
		}
		else
		{
			nextRateTime += rateInterval;
			trySkipLamda(currtime);
			return true;
		}
	}
	
	private void trySkipLamda(long currtime)
	{
		if(currtime < nextRateTime)
		{
			return;
		}
		else
		{
			this.log.debug("Skipping lamda:"+iter.next());
			nextRateTime += rateInterval;
			trySkipLamda(currtime);
		}
	}
}
