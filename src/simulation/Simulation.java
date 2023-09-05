package simulation;

import java.util.ArrayList;

import elements.force_pair.Spring;
import elements.groups.ParticleGroup;
import elements.groups.SpringGroup;
import elements.point_mass.Particle;
import evaluation.MyMath;
import gui.ConsoleWindow;
import gui.MainWindow;
import gui.lang.GUIStrings;
import gui.shapes.SpringShape;
import main.SampleScenes;
import simulation.components.InteractionProcessor;
import simulation.components.SimulationComponent;
import simulation.components.TimeStepController;

public class Simulation implements Runnable {

	private static Simulation instance;

	private SimulationContent content;

	private final ParticleGroup pForRemove = new ParticleGroup();
	private final ParticleGroup pForAdd = new ParticleGroup();
	private final SpringGroup sForRemove = new SpringGroup();
	private final SpringGroup sForAdd = new SpringGroup();

	private final ArrayList<SimulationComponent> simulationComponents = new ArrayList<SimulationComponent>();

	public InteractionProcessor interactionProcessor;
	public TimeStepController timeStepController;

	private double time = 0;
	private double stopTime = Double.MAX_VALUE;
	private long stepEvaluationTime = 1;

	private static boolean isRunning = false, refreshContentNeeded = true;

	public Simulation() {
		instance = this;		
		content = new SimulationContent();		
		interactionProcessor = new InteractionProcessor(content);
		timeStepController = new TimeStepController();		
		simulationComponents.add(timeStepController);
		simulationComponents.add(interactionProcessor);
	}

	public static Simulation getInstance() {
		if (instance != null)
			return instance;
		else
			instance = new Simulation();
		return instance;
	}

	@Override
	public void run() {
		isRunning = true;
		ConsoleWindow.println(GUIStrings.SIMULATION_THREAD_STARTED);
		interactionProcessor.recalculateNeighborsNeeded();
		while (isRunning && time < stopTime) {
			long t0 = System.nanoTime();
			if (refreshContentNeeded)
				refreshContent();
			perfomStep();
			stepEvaluationTime = System.nanoTime() - t0;
		}
		stopTime = Double.MAX_VALUE;
		ConsoleWindow.println(GUIStrings.SIMULATION_THREAD_ENDED);
	}

	public boolean isActive() {
		return isRunning;
	}

	private void perfomStep() {
		time += timeStepController.getTimeStepSize();
		for (SimulationComponent component : simulationComponents)
			component.process();
	}

	public long perfomStep(int stepNumber) {
		long t1 = System.nanoTime();
		int n1 = interactionProcessor.getNeighborSearchsNumber();
		ConsoleWindow.println(GUIStrings.TIMESTEP + " " + timeStepController.getTimeStepSize() + " s");
		for (int i = 1; i < stepNumber; i++)
			perfomStep();
		long t2 = System.nanoTime();
		int n2 = interactionProcessor.getNeighborSearchsNumber();
		ConsoleWindow.println("Done " + stepNumber + " steps");
		ConsoleWindow.println("	elapsed: " + (t2 - t1) / 1E6 + " ms");
		ConsoleWindow.println("	neighbor searches: " + (n2 - n1));
		ConsoleWindow.println(GUIStrings.TIMESTEP + " " + timeStepController.getTimeStepSize() + " s");
		return t2 - t1;
	}

	public void perfomSimulation(double duration) {
		instance.setSimulationDuration(duration);
		instance.run();
	}

	private void waitForStepComplete() {
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			ConsoleWindow.println(GUIStrings.SIMULATION_THREAD_CANT_BE_CONTINUED);
			e.printStackTrace();
		}
	}

	public void stopSimulation() {
		stopSimulationEchoOff();
	}

	private void stopSimulationEchoOff() {
		isRunning = false;
	}

	private void stopSimulationAndWait() {
		if (isRunning) {
			isRunning = false;
			waitForStepComplete();
		}
	}

	public void addToSimulation(Particle p) {
		if (isRunning) {
			pForAdd.add(p);
			refreshContentNeeded = true;
		} else
			content.particles.add(p);
		interactionProcessor.recalculateNeighborsNeeded();
	}

	public void addToSimulation(Spring s) {
		if (isRunning) {
			sForAdd.add(s);
			refreshContentNeeded = true;
		} else
			content.springs.add(s);
		interactionProcessor.recalculateNeighborsNeeded();
	}

	public void addToSimulation(ParticleGroup pp) {
		if (isRunning) {
			pForAdd.addAll(pp);
			refreshContentNeeded = true;
		} else
			content.particles.addAll(pp);
		interactionProcessor.recalculateNeighborsNeeded();
		ConsoleWindow.println(GUIStrings.PARTICLES_ADDED);
	}

	public void addToSimulation(SpringGroup ss) {
		if (isRunning) {
			sForAdd.addAll(ss);
			refreshContentNeeded = true;
		} else
			content.springs.addAll(ss);
		interactionProcessor.recalculateNeighborsNeeded();
		ConsoleWindow.println(GUIStrings.SPRINGS_ADDED);
	}

	public synchronized void addToSimulation(SimulationComponent arg) {
		boolean wasActive = false;
		if (isRunning) {
			stopSimulationAndWait();
			wasActive = true;
		}
		simulationComponents.add((SimulationComponent) arg);
		ConsoleWindow.println(GUIStrings.TO_SIMULATION_ADDED + " " + arg.getClass().getSimpleName());
		if (wasActive)
			MainWindow.getInstance().startSimulationThread();
	}

	public void removeParticleSafety(Particle p) {
		if (!content.springs.isEmpty())
			removeSpringsSafety(findAttachedSprings(p));
		if (isRunning) {
			pForRemove.add(p);
			refreshContentNeeded = true;
		} else
			content.particles.remove(p);
		interactionProcessor.recalculateNeighborsNeeded();
	}

	private void removeParticles(ParticleGroup pp) {
		if (!content.springs.isEmpty())
			for (Particle p : pp)
				removeSprings(findAttachedSprings(p));
		content.particles.removeAll(pp);
		interactionProcessor.recalculateNeighborsNeeded();
	}

	public void removeParticlesSafety(ParticleGroup pp) {
		if (!content.springs.isEmpty())
			for (Particle p : pp)
				removeSpringsSafety(findAttachedSprings(p));
		if (isRunning) {
			pForRemove.addAll(pp);
			refreshContentNeeded = true;
		} else
			content.particles.removeAll(pp);
		interactionProcessor.recalculateNeighborsNeeded();
	}

	public void removeRandomParticles(int number) {
		for (int i = 0; i < number; i++)
			removeParticleSafety(content.getParticle((int) Math.round(Math.random() * content.particles.size())));
	}

	private void removeSpring(Spring s) {
		content.springs.remove(s);
	}

	public void removeSpringSafety(Spring s) {
		if (isRunning) {
			sForRemove.add(s);
			refreshContentNeeded = true;
		} else
			content.springs.remove(s);
	}

	private void removeSprings(SpringGroup ss) {
		for (Spring s : ss)
			removeSpring(s);
	}

	public void removeSpringsSafety(SpringGroup ss) {
		if (isRunning) {
			sForRemove.addAll(ss);
			refreshContentNeeded = true;
		} else
			content.springs.removeAll(ss);
	}

	public void clearSimulation() {
		ConsoleWindow.clearConsole();
		ConsoleWindow.print(GUIStrings.FORCE_SIMULATION_STOP + " ");
		stopSimulationAndWait();
		ConsoleWindow.println(GUIStrings.DONE);
		reset();
	}

	public Spring getLastAddedSpring() {
		return content.springs.get(content.springs.size() - 1);
	}

	public SpringGroup findAttachedSprings(Particle p) {
		SpringGroup returnList = new SpringGroup();
		for (int i = 0; i < content.springs.size(); i++) {
			Spring s = content.springs.get(i);
			if (s != null) {
				if (s.isHasParticle(p))
					returnList.add(s);
			}
		}
		return returnList;
	}

	public Particle findNearestParticle(double x, double y, double maxDistance) {
		double minSqDist = Double.MAX_VALUE, sqDist;
		Particle nearest = null;
		for (Particle p : content.particles) {
			sqDist = MyMath.defineSquaredDistance(p, x, y) - MyMath.sqr(p.getRadius());
			if (sqDist < minSqDist) {
				minSqDist = sqDist;
				nearest = p;
			}
		}
		if (minSqDist > MyMath.sqr(maxDistance))
			nearest = null;
		return nearest;
	}

	public Spring findNearestSpring(double x, double y, final double maxDistance) {
		double dist, margin;
		Spring nearest = null;
		for (Spring s : content.springs) {
			if (s.isLine())
				margin = maxDistance;
			else
				margin = SpringShape.SPRING_ZIGZAG_AMPLITUDE + s.getVisibleWidth() / 2;
			dist = MyMath.defineDistanceToLineSegment(s, x, y);
			if (dist < Math.max(s.getVisibleWidth() / 2, margin)) {
				nearest = s;
			}
		}
		return nearest;
	}

	private void reset() {
		content.deselectAll();
		refreshContentNeeded = false;
		time = 0;		
		stepEvaluationTime = 1;
		content.springs.clear();
		content.particles.clear();
		simulationComponents.clear();
		simulationComponents.add(timeStepController);
		simulationComponents.add(interactionProcessor);
		ConsoleWindow.println(GUIStrings.CLEARED);
		interactionProcessor.reset();
		timeStepController.resetTimeStep();
		SampleScenes sampleScenes = new SampleScenes();
		sampleScenes.emptyScene();
	}

	private void refreshContent() {
		removeParticles(pForRemove);
		content.particles.addAll(pForAdd);
		pForAdd.clear();
		removeSprings(sForRemove);
		content.springs.addAll(sForAdd);
		sForAdd.clear();
		refreshContentNeeded = false;
	}

	public double x(int i) {
		return content.getParticle(i).getX();
	}

	public double y(int i) {
		return content.getParticle(i).getY();
	}

	public double getTime() {
		return time;
	}
	
	public void setSimulationDuration(double duration) {
		instance.stopTime = time + duration;
	}

	public long getEvaluationTimeNanos() {
		return stepEvaluationTime;
	}

	public SimulationContent getContent() {
		return content;
	}

}