package acs.project.simulation.optimization.strategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import acs.project.simulation.dataset.common.RequestEvent;
import acs.project.simulation.optimization.ServerProfile;

public interface EnergyAwareStrategyInterface {
	public abstract ServerProfile selectServer(List<ArrayList<ServerProfile>> serverlist,RequestEvent event) throws IOException, ClassNotFoundException;
}