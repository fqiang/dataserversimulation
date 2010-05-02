package acs.project.simulation.dataset;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import org.apache.log4j.Logger;

import acs.project.simulation.dataset.common.Location;
import acs.project.simulation.dataset.common.RequestEvent;
import acs.project.simulation.dataset.common.Timezone;
import acs.project.simulation.dataset.strategy.Hour24PerHourStrategy;
import acs.project.simulation.dataset.strategy.OneHourHalfHourStrategy;
import acs.project.simulation.dataset.strategy.RequestEventGenerationStrategy;
import acs.project.simulation.random.*;

public class RequestEventGenerator {

	private Location location;
	private Timezone timezone;
	private long currtime = 0;
	private String filename = null;
	private RequestEventGenerationStrategy strategy;

	private ExpRandom exp_rand = null;
	private LognorRandom lognor_rand = null;
	private ZipfRandom zipf_rand = null;

	public final Logger log = Logger.getLogger(RequestEventGenerator.class);

	public RequestEventGenerator(Location location, Timezone timezone,
			String filename, RequestEventGenerationStrategy strategy) {
		this.location = location;
		this.timezone = timezone;
		this.currtime = 0;
		this.filename = filename;
		this.strategy = strategy;
	}

	public void initGenerator(long seed, double lamda, double mean,
			double stdd, int size, double skew) {
		this.exp_rand = new ExpRandom(seed, lamda);
		this.lognor_rand = new LognorRandom(seed << 4, mean, stdd);
		this.zipf_rand = new ZipfRandom(seed << 8, size, skew);
	}

	public void start() throws FileNotFoundException {
		log.assertLog(this.exp_rand != null, "Generator Must be initilized!");
		PrintStream file = new PrintStream(new FileOutputStream(new File(
				filename)), true);
		// file.println("");
		while (true) {
			long arrival_time = (long) (currtime + exp_rand.nextExpDouble()*1000);
			long content_size = (long) (lognor_rand.nextLognorDouble() * 1000000);
			int content_rank = zipf_rand.nextZipf();
			//exist condition
			if (this.strategy.isGenerationEnd(arrival_time)) {
				break;
			}
			//log.debug("arrival["+arrival_time+"] "+"size["+content_size+"] "+"rank["+content_rank+"] ");
			RequestEvent event = new RequestEvent( location, timezone,
					arrival_time,content_size, content_rank);
			file.println(event.toString());
			
			//system update
			update(arrival_time);
		}
	}

	private void update(long arrival_time) {
		this.currtime = arrival_time;
		if (this.strategy.isProceedToNextArrivalRate(this.currtime)) {
			exp_rand.setLamda(this.strategy.getNextLamda());
		}
	}
	
	public static void testGenerate_1hr(Location loc,Timezone zone, String filename) throws FileNotFoundException 
	{
		double[] lamdas = { 10d, 100d };
		double[] lamdas1 = { 100d };
		RequestEventGenerationStrategy onehr_halfhr_strategy = new OneHourHalfHourStrategy(
				lamdas1);
		RequestEventGenerator onehr_halfhr_gen = new RequestEventGenerator(
				loc,zone,filename,
				onehr_halfhr_strategy);
		onehr_halfhr_gen.initGenerator(1000, lamdas[0], 1, 1.5, 300, 1);
		onehr_halfhr_gen.start();
	}
	
	public static void testGenerate_24hr() throws FileNotFoundException
	{
		/*following code generate the requests from Location:ASIAN, Timezone:GMT_8
		  for 24 hrs and lamda changes for each 1 hr.
		 */
		double[] lamdas = { 0.01d, 0.02d,0.03d,0.04d,0.05d,0.06d,0.07d,10d,15d,20d,25d,100d,100d,100d,100d,100d,100d,100d,100d,150d,200d,300d,10d,5d};
		double[] lamdas1 = { 0.02d,0.03d,0.04d,0.05d,0.06d,0.07d,10d,15d,20d,25d,100d,100d,100d,100d,100d,100d,100d,100d,150d,200d,300d,10d,5d};
		RequestEventGenerationStrategy onehr_halfhr_strategy = new Hour24PerHourStrategy(
				lamdas1);
		RequestEventGenerator onehr_halfhr_gen = new RequestEventGenerator(
				Location.ASIAN, Timezone.GMT_8, "asian_gmt8_24hr_hr.csv",
				onehr_halfhr_strategy);
		onehr_halfhr_gen.initGenerator(1000, lamdas[0], 10, 1.5, 300, 1);
		onehr_halfhr_gen.start();
	}

	public static void main(String[] args) throws FileNotFoundException 
	{
		RequestEventGenerator.testGenerate_1hr(Location.ASIAN,Timezone.GMT_0,"asian_gmt0_onehr_halfhr.trace");	
		RequestEventGenerator.testGenerate_1hr(Location.JAPAN,Timezone.GMT_0,"japan_gmt0_onehr_halfhr.trace");
		RequestEventGenerator.testGenerate_1hr(Location.CHINA,Timezone.GMT_0,"china_gmt0_onehr_halfhr.trace");
		RequestEventGenerator.testGenerate_1hr(Location.EUROPE,Timezone.GMT_0,"europe_gmt0_onehr_halfhr.trace");
		RequestEventGenerator.testGenerate_1hr(Location.AMERICAN,Timezone.GMT_0,"american_gmt0_onehr_halfhr.trace");
	}
}
