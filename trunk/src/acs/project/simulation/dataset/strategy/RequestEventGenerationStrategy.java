package acs.project.simulation.dataset.strategy;

public interface RequestEventGenerationStrategy {
	public boolean isGenerationEnd(long time);
	public double  getNextLamda();
	public boolean isProceedToNextArrivalRate(long currtime);
}
