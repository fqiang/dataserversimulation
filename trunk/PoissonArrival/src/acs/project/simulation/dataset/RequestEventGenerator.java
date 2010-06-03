package acs.project.simulation.dataset;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import org.apache.log4j.Logger;

import acs.project.simulation.common.Location;
import acs.project.simulation.common.RequestEvent;
import acs.project.simulation.common.Timezone;
import acs.project.simulation.dataset.random.ExpRandom;
import acs.project.simulation.dataset.random.LognorRandom;
import acs.project.simulation.dataset.random.ZipfRandom;
import acs.project.simulation.dataset.strategy.RequestEventGenerationStrategy;

public class RequestEventGenerator {

	private Location location;
	private Timezone timezone;
	private long currtime = 0;
	private String filename = null;
	private RequestEventGenerationStrategy strategy;

	private ExpRandom exp_rand = null;
	private LognorRandom lognor_rand = null;
	private ZipfRandom zipf_rand = null;

	public final static Logger log = Logger.getLogger(RequestEventGenerator.class);

	public RequestEventGenerator(Location location, Timezone timezone,
			String filename, RequestEventGenerationStrategy strategy) {
		this.location = location;
		this.timezone = timezone;
		this.currtime = 0;
		this.filename = filename;
		this.strategy = strategy;
	}

	public void initRandomVariables(long seed, double mu,
			double teta, int size, double skew) {
		this.exp_rand = new ExpRandom(seed, strategy.getNextLamda());
		this.lognor_rand = new LognorRandom(seed << 4, mu, teta);
		this.zipf_rand = new ZipfRandom(seed << 8, size, skew);
	}

	public void start() throws FileNotFoundException {
		log.assertLog(this.exp_rand != null, "Generator Must be initilized!");
		PrintStream file = new PrintStream(new FileOutputStream(new File(
				filename)), true);
		while (true) {
			long arrival_time = (long) (currtime + exp_rand.nextExpDouble());
			long content_size = (long) (lognor_rand.nextLognorDouble()*5000);
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
		//double[] lamdas = { 0.01d, 0.02d };
		RequestEventGenerationStrategy onehr_halfhr_strategy = new RequestEventGenerationStrategy();
		RequestEventGenerator onehr_halfhr_gen = new RequestEventGenerator(loc,zone,filename,onehr_halfhr_strategy);
		onehr_halfhr_gen.initRandomVariables(1000, 1, 1.5, 300, 1);
		onehr_halfhr_gen.start();
	}
	
	public static void testGenerate_24hr(Location loc,Timezone zone, String filename) throws FileNotFoundException
	{
		double[] lamdas = { 0.01d, 0.02d,0.03d,0.04d,0.05d,0.06d,0.07d,10d,15d,20d,25d,100d,100d,100d,100d,100d,100d,100d,100d,150d,200d,300d,10d,5d};
		RequestEventGenerationStrategy strategy = new RequestEventGenerationStrategy(lamdas,3600*1000*24,3600*1000);
		RequestEventGenerator generator = new RequestEventGenerator(loc,zone,filename,strategy);
		generator.initRandomVariables(1000, 1, 1.5, 300, 1);
		generator.start();
	}

	public static void main(String[] args) throws FileNotFoundException 
	{
		if(args.length==0)
		{
			System.out.println("./java RequestEventGenerator [1|2]");
			System.out.println("   1  - generating 1 hour test trace");
			System.out.println("   2  - generating 24 hour test trace");
		}
		if(args[0].equals("1"))
		{
			System.out.println("Generating 1 hour test trace...");
			for(Location l:Location.values())
			{
				log.debug("Generating Location["+l.name()+"]...");
				Timezone t = Timezone.GMT_0;
				String filename = "./trace/"+l.name().toLowerCase()+"_"+t.name().toLowerCase()+"_onehr_halfhr.trace";
				RequestEventGenerator.testGenerate_1hr(l,t,filename);
			}
		}
		else if(args[0].equals("2"))
		{
			System.out.println("Generating 24 hour test trace...");
			for(Location l:Location.values())
			{
				Timezone t = Timezone.GMT_0;
				String filename = "./trace/"+l.name().toLowerCase()+"_"+t.name().toLowerCase()+"_24hr_1hr.trace";
				System.out.println("Generating Location["+l.name()+"]...");
				RequestEventGenerator.testGenerate_24hr(l,t,filename);	
			}
		}
	}
}
