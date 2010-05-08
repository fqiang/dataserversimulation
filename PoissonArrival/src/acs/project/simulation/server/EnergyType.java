package acs.project.simulation.server;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import acs.project.simulation.common.Order;
import acs.project.simulation.common.OrderComparator;

public class EnergyType implements Serializable {
	/**
	 * 
	 */
	@Order(value=1)
	private String name;
	@Order(value=2)
	private double ghRate;
	@Order(value=3)
	private static final long serialVersionUID = -4517066873880351748L;
	
	public EnergyType(String aName,double rate)
	{
		name = aName;
		ghRate = rate;
	}
	
	public String toString(){
		String val = "";
		Field[] fields = this.getClass().getDeclaredFields();
		Arrays.sort(fields,new OrderComparator());
		for(Field f:fields){
			try {
				if(!Modifier.isFinal(f.getModifiers())){
					val += f.get(this) +",";
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return val;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setGhRate(double ghRate) {
		this.ghRate = ghRate;
	}
	public double getGhRate() {
		return ghRate;
	}
}
