package acs.project.simulation.server;

import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import acs.project.simulation.common.RequestEvent;
import acs.project.simulation.optimization.LoadBalancer;

public class MyLittleTest {
	
	public static final Logger log = Logger.getLogger(MyLittleTest.class);
	
	public static void main(String[] args)
	{
		double rate  =0 ;
		int size = 1200;
		int now = 1200;
		rate = now/(double)size;
		System.out.println(rate+"||"+(rate==1d)+"||"+(0d==-0d));
		assert true;
		
		ArrayList<String> test = new ArrayList<String>();
		test.add("a");
		test.add("b");
		test.add("c");
		test.add("d");
		test.add("e");
		test.add("f");
		
		for(int i=0;i<test.size();i++)
		{
			System.out.println("["+i+"] "+test.get(i));
		}
		
		int newSize = 4;
		test.subList(newSize, test.size()+1).clear();
		System.out.println();
		
		for(int i=0;i<test.size();i++)
		{
			System.out.println("["+i+"] "+test.get(i));
		}
		
	}
}
