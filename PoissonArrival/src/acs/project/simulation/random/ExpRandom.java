package acs.project.simulation.random;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import org.apache.log4j.Logger;

public class ExpRandom {
	
	private Random rand;
	private double lamda;
	public final Logger log = Logger.getLogger(ExpRandom.class);
	
	public ExpRandom(long seed,double lamda)
	{
		rand = new Random(seed);
		this.lamda = lamda;
	}
	
	public void reSeed(long seed)
	{
		rand.setSeed(seed);
	}
	
	public double nextExpDouble()
	{
		return -Math.log(rand.nextDouble())/lamda;	
	}
	
	public double getLamda()
	{
		return this.lamda;
	}
	public void setLamda(double lamda)
	{
		this.lamda = lamda;
	}
	
	public double getCDF(double i)
	{
		return 1-Math.pow(Math.E, -this.lamda*i);
	}
	
	public static void main(String[] args) throws FileNotFoundException
	{
		long seed = 1000;
		ExpRandom rand1 = new ExpRandom(seed,0.5);
		ExpRandom rand5 = new ExpRandom(seed,1);
		ExpRandom rand10 = new ExpRandom(seed,1.5);
		
		int times = 100;
		
		PrintStream file = new PrintStream(new FileOutputStream(new File("exp.csv")),true);
		file.println("X,(lambda=0.5),(lambda=1),(lambda=1.5)");
		for(int i=1;i<=times;i++)
		{
			file.println(i*0.05+","+rand1.getCDF(i*0.05)+","+rand5.getCDF(i*0.05)+","+rand10.getCDF(i*0.05));
		}
		rand1.log.debug("Done -!");
		rand5.log.debug("Done -!");
		
		for(int i=0;i<10;i++){
			rand1.log.debug(rand1.nextExpDouble());
		}
	}
}
