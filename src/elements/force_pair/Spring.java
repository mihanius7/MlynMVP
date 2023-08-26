package elements.force_pair;

import static constants.PhysicalConstants.cm;
import static evaluation.MyMath.cube;
import static evaluation.MyMath.defineDistance;
import static evaluation.MyMath.defineReducedMass;
import static evaluation.MyMath.sqr;
import static java.lang.Math.PI;
import static java.lang.Math.sqrt;
import static simulation.Simulation.interactionProcessor;

import java.awt.Color;
import java.awt.geom.Point2D;

import constants.PhysicalConstants;
import elements.Element;
import elements.Interactable;
import elements.Selectable;
import elements.point_mass.Particle;
import gui.Colors;
import gui.ConsoleWindow;
import gui.lang.GUIStrings;
import gui.shapes.SpringShape;
import simulation.Simulation;

public class Spring extends ForcePair implements Element, Selectable, Interactable {

	public static double DEFAULT_VISIBLE_WIDTH = 2 * cm;
	protected double l0 = 0, k = 0, c = 0, uSquared = 0, dx = 0;
	protected double visibleWidth = DEFAULT_VISIBLE_WIDTH;
	protected double fn;
	protected double maxStress = Double.MAX_VALUE;
	protected boolean visible = true, isSelected = false, isLine = true, canCollide = false;
	protected GapType gapType = GapType.NONE;
	protected SpringShape shape;

	public enum GapType {
		NONE, ONE_SIDED, TWO_SIDED
	};

	public Spring(Particle i, Particle j, double l0, double k, double c) {
		super(i, j);
		initSpring(k, l0, c);
		distance = defineDistance(i, j);
	}

	public Spring(int i, int j, double l0, double k, double c) {
		super(i, j);
		initSpring(k, l0, c);
		distance = defineDistance(i, j);
	}

	public Spring(Particle i, Particle j, double k, double c) {
		super(i, j);
		initSpring(k, defineDistance(i, j), c);
	}

	public Spring(int i, int j, double k, double c) {
		super(i, j);
		initSpring(k, defineDistance(i, j), c);
	}

	public Spring(double k, double c) {
		super();
		initSpring(k, c);
	}

	private void initSpring(double k, double l0, double c) {
		this.k = k;
		this.l0 = l0;
		this.c = c;
		distance = l0;
		lastDistance = distance;
		shape = new SpringShape(this);
		ConsoleWindow.println(GUIStrings.SPRING_CREATED + ": ");
		refreshResonantFrequency();
		ConsoleWindow.println(String.format("	" + GUIStrings.SPRING_DAMPING_RATIO + " %.3f", defineDampingRatio()));
		checkOverCriticalDamping();
	}

	private void initSpring(double k, double c) {
		this.k = k;
		this.c = c;
		lastDistance = distance;
		ConsoleWindow.println(GUIStrings.REFERENCE_SPRING_CREATED);
	}

	public double getAbsoluteDeformation() {
		return dx;
	}

	private void defineForce() {
		dx = distance - l0;
		double fd = defineVelocityProjection() * c;
		double fs;
		if (uSquared == 0)
			fs = -k * dx;
		else
			fs = -k * (dx + uSquared * cube(dx));
		force = fs + fd;
	}

	public void applyForce() {
		super.applyForce();
		defineForce();
		interactionProcessor.applyForceParallelToDistance(p1, p2, force, distance);
		interactionProcessor.tryToSetMaxSpringForce(force);
		if (force >= maxStress)
			Simulation.removeSpringSafety(this);
	}

	public boolean isVisible() {
		return visible;
	}

	public void makeVisible(boolean v) {
		visible = v;
	}

	public double getVisibleWidth() {
		return visibleWidth;
	}

	public double getParticlesMeanWidth() {
		return (p1.getRadius() + p2.getRadius()) / 2;
	}

	public double getNominalLength() {
		return l0;
	}

	public void setNominalLength(double length) {
		this.l0 = length;
		ConsoleWindow.println(String.format(GUIStrings.SPRING_NOMINAL_LENGTH + " %.6e", length));
	}

	public double getDeformatedLength() {
		return l0 + dx;
	}

	public void saveDeformation(double coeficient) {
		this.l0 += coeficient * dx;
	}

	public double getStiffnes() {
		return k;
	}

	public void setStiffnes(double k) {
		this.k = k;
		refreshResonantFrequency();
		ConsoleWindow.println(String.format(GUIStrings.SPRING_STIFFNES + "  %.3f", k));
	}

	public double getHardening() {
		return sqrt(uSquared);
	}

	public void setHardening(double u) {
		this.uSquared = sqr(u);
		ConsoleWindow.println(GUIStrings.SPRING_HARDENING_COEFFICIENT + " " + u);
	}

	public double getMaxStress() {
		return maxStress;
	}

	public void setMaxStress(double maxStress) {
		this.maxStress = maxStress;
	}

	public double refreshResonantFrequency() {
		fn = (1 / (2 * PI)) * sqrt(k / defineReducedMass(p1, p2));
		return fn;
	}

	public double refreshGravityResonantFrequency() {
		fn = (1 / (2 * PI)) * sqrt(Math.abs(PhysicalConstants.gn / dx));
		return fn;
	}

	public double getResonantFrequency() {
		return fn;
	}

	public void setResonantFrequency(double f) {
		if (f > 0)
			k = 4 * sqr(Math.PI) * sqr(f) * defineReducedMass(p1, p2);
		ConsoleWindow.println(String.format(GUIStrings.SPRING_STIFFNES + " %.3e N/m", k));
		refreshResonantFrequency();
		ConsoleWindow.println(String.format(GUIStrings.SPRING_RESONANT_FREQUENCY + " %.1f Hz", fn));
	}

	public double defineDampingRatio() {
		return getDamping() / defineCriticalDamping();
	}

	public double defineCriticalDamping() {
		return 2 * sqrt(k * defineReducedMass(p1, p2));
	}

	public double getDamping() {
		return c;
	}

	public void setDamping(double c) {
		this.c = c;
	}

	public void setDampingCritical() {
		setDampingRatio(1);
	}

	public void setDampingRatio(double ksi) {
		setDamping(ksi * defineCriticalDamping());
		checkOverCriticalDamping();
		ConsoleWindow.println(String.format(GUIStrings.SPRING_DAMPING_RATIO + " %.3f", defineDampingRatio())
				+ String.format(", " + GUIStrings.SPRING_DAMPING + " %.3e N/(m/s)", c));
	}

	public void setQualityFactor(double Q) {
		if (Q > 0) {
			ConsoleWindow.println(String.format(GUIStrings.SPRING_QUALITY_FACTOR + " %.1f...", Q));
			setDampingRatio(1f / 2 / Q);
		}
	}

	public void setVisibleWidth(double visibleWidth) {
		this.visibleWidth = visibleWidth;
	}

	public Color getColor() {
		if (!isSelected)
			return shape.getColor();
		else
			return Colors.SELECTED;
	}

	public Color getEigeneColor() {
		return shape.getColor();
	}

	public void setColor(Color color) {
		// this.color = color;
	}

	private void checkOverCriticalDamping() {
		if (defineDampingRatio() > 20) {
			setDampingRatio(20);
			ConsoleWindow.println(GUIStrings.HYPERCRITICAL_DAMPING_FIXED);
		}
	}

	@Override
	public void select() {
		isSelected = true;
	}

	@Override
	public void deselect() {
		isSelected = false;
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setIsLine(boolean b) {
		isLine = b;
	}

	public boolean isLine() {
		return isLine;
	}

	@Override
	public boolean isCanCollide() {
		return canCollide;
	}

	@Override
	public void setCanCollide(boolean b) {
		canCollide = b;
		if (canCollide)
			visibleWidth = 5 * cm;
		else
			visibleWidth = DEFAULT_VISIBLE_WIDTH;
	}

	public SpringShape getShape() {
		return shape;
	}

	@Override
	public java.awt.geom.Point2D.Double getCenterPoint() {
		return new Point2D.Double(0.5 * p1.getX() + 0.5 * p2.getX(), 0.5 * p1.getY() + 0.5 * p2.getY());
	}
}
