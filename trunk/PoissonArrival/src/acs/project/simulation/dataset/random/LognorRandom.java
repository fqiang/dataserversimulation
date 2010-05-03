package acs.project.simulation.dataset.random;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;


public class LognorRandom {
	
	private Random rand;
	private double mean;
	private double stdd;
	
	public LognorRandom(long seed,double mean,double stdd)
	{
		this.rand = new Random(seed);
		this.mean = mean;
		this.stdd = stdd;
	}
	
	public double getPDF(double x)
	{
		return
		Math.exp(-(Math.pow(Math.log(x)-this.mean, 2)/(2*this.stdd*this.stdd)))/(x*Math.sqrt(2*Math.PI*this.stdd*this.stdd));
	}
	
	public double nextLognorDouble()
	{
		return Math.exp(this.mean+this.stdd*rand.nextGaussian());
	}
	
	public static void main(String[] args) throws FileNotFoundException
	{
		long seed = 1000;
		double mean = 1;
		
		LognorRandom rand1 = new LognorRandom(seed,mean,10);
		LognorRandom rand2 = new LognorRandom(seed,mean,1.5);
		LognorRandom rand3 = new LognorRandom(seed,mean,0.125);
		
		PrintStream file = new PrintStream(new FileOutputStream(new File("lognor.csv")),true);
		file.println("Number,mean=1 stdd=10,mean=1 stdd=1.5,mean=1 stdd=0.125");
		
		int size = 100;
		for(int i=1;i<=size;i++)
		{
			file.println(i*0.05+","+rand1.getPDF(i*0.05)+","+rand2.getPDF(i*0.05)+","+rand3.getPDF(i*0.05));
		}
	}
}
