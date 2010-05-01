package acs.project.simulation.optimization;

import java.io.File;
import java.io.FileFilter;

public class TraceFileFilter implements FileFilter {

	@Override
	public boolean accept(File pathname) {
		if(pathname.getName().endsWith(".trace"))
		{
			return true;
		}
		return false;
	}
	
}
