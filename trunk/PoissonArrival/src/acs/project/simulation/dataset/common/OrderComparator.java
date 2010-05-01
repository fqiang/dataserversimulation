package acs.project.simulation.dataset.common;

import java.util.Comparator;
import java.lang.reflect.Field;

public class OrderComparator implements Comparator<Field>{

	@Override
	public int compare(Field f0, Field f1) {
		Order o0 = f0.getAnnotation(Order.class);
		Order o1 = f1.getAnnotation(Order.class);
		
		if(o0!=null&&o1!=null){
			return o0.value()-o1.value();
		}
		else if(o0!=null&&o1==null){
			return -1;
		}
		else if(o0==null&&o1!=null){
			return 1;
		}
		else {
			return f0.getName().compareTo(f1.getName());
		}
	}
}
