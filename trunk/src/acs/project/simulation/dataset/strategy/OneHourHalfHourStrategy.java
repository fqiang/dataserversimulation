package acs.project.simulation.dataset.strategy;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class OneHourHalfHourStrategy implements RequestEventGenerationStrategy {

	public final Logger log  = Logger.getLogger(OneHourHalfHourStrategy.class);
	
	private ArrayList<Double> lamdas = new ArrayList<Double>();
	private Iterator<Double> iter = null;
	private final long SIM_END_TIME = 3600*1000;
	
	private long nextLamdaChangeTime = 0;
	private long lamdaChangeInterval = 0;
	
	
	public OneHourHalfHourStrategy(double[] lamdas)
	{
		for(double d:lamdas)
		{
			this.lamdas.add(new Double(d));
		}
		iter = this.lamdas.iterator();
		nextLamdaChangeTime = SIM_END_TIME/2;
		lamdaChangeInterval = SIM_END_TIME/2;
	}
	
	@Override
	public double getNextLamda() {
		return iter.next().doubleValue();
	}

	@Override
	public boolean isGenerationEnd(long currtime) {
		if(currtime < SIM_END_TIME)
		{
			return false;
		}
		else
		{
			return true;
		}			
	}

	@Override
	public boolean isProceedToNextArrivalRate(long currtime) {
		if(currtime < this.nextLamdaChangeTime)
		{
			return false;
		}
		else
		{
			this.nextLamdaChangeTime += this.lamdaChangeInterval;
			this.trySkipLamda(currtime);
			return true;
		}
	}

	private void trySkipLamda(long currtime)
	{
		if(currtime < this.nextLamdaChangeTime)
		{
			return;
		}
		else
		{
			this.log.debug("Skipping lamda:"+this.iter.next());
			this.nextLamdaChangeTime += this.lamdaChangeInterval;
			this.trySkipLamda(currtime);
		}
	}
}
