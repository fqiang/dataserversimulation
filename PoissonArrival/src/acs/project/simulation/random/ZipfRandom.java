package acs.project.simulation.random;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;
import org.apache.log4j.*;

public class ZipfRandom {

	private Random rand;
	private int size;
	private double skew;
	private double bottom = 0;
	public final Logger log = Logger.getLogger(ZipfRandom.class);

	public ZipfRandom(long seed, int size, double skew) {
		this.rand = new Random(seed);
		this.size = size;
		this.skew = skew;
		for(int i=1;i<size; i++) {
			this.bottom += (1/Math.pow(i, this.skew));
		}
	}

	public void reSeed(long seed) {
		this.rand.setSeed(seed);
	}

	public int nextZipf() {
		int rank;
		double friquency = 0;
		double dice;

		rank = rand.nextInt(size);
		friquency = (1.0d / Math.pow(rank, this.skew)) / this.bottom;
		dice = rand.nextDouble();

		while (!(dice < friquency)) {
			rank = rand.nextInt(size);
			friquency = (1.0d / Math.pow(rank, this.skew)) / this.bottom;
			dice = rand.nextDouble();
		}
		return rank;
	}
	// This method returns a probability that the given rank occurs.
	public double getProbability(int rank) {
		return (1.0d / Math.pow(rank, this.skew)) / this.bottom;
	}
	
	public static void main(String[] args) throws FileNotFoundException
	{
		int size = 300;
		long seed = 1000;
		ZipfRandom rand1 = new ZipfRandom(seed,size,0.1);
		ZipfRandom rand10 = new ZipfRandom(seed,size,0.5);
		ZipfRandom rand100 = new ZipfRandom(seed,size,1);
		PrintStream file = new PrintStream(new FileOutputStream(new File("zipf.csv")),true);
		file.println("Rank,Probability1,Probability2,Probability3");
		for(int i=1;i<=size;i++)
		{
			file.println(i+","+rand1.getProbability(i)+","+rand10.getProbability(i)+","+rand100.getProbability(i));
		}
		rand1.log.debug("Done! - ");
	}
}
