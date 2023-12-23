package gui.heatmap;

import static simulation.Simulation.getInstance;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import calculation.Functions;
import calculation.Vector;
import calculation.constants.PhysicalConstants;
import calculation.pairforce.PairForce;
import calculation.pairforce.PairForceFactory;
import elements.point.Particle;
import gui.viewport.Colors;
import gui.viewport.CoordinateConverter;
import gui.viewport.Viewport;
import simulation.Simulation;
import simulation.components.InteractionType;

public class HeatMap {

	private static final int AUTORANGE_VALUE_DIVIDER = 2;
	private int updateInterval = 5, resolution = 8, updatesNumber = 0, width, height;
	private Graphics2D heatMapCanvas;
	private BufferedImage heatMapImage;
	private PairForce pairForce;
	private double range = 1000;
	private double minValue, minField;
	private double maxValue, maxField;
	private boolean isGravityFieldMap;
	private boolean isAdaptiveRange = true;
	private Viewport viewport;
	private HeatMapType mapType = HeatMapType.STRENGTH;
	private ProjectionType projectionType = ProjectionType.MAGNITUDE;

	public HeatMap(int w, int h) {
		width = w;
		height = h;
		heatMapImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		heatMapCanvas = heatMapImage.createGraphics();
	}

	public HeatMap(Viewport v) {
		this(v.getWidth(), v.getHeight());
		this.viewport = v;
	}

	public BufferedImage getHeatMapImage() {
		return heatMapImage;
	}

	public void updateHeatMapImage() {
		updatesNumber++;
		if (getInstance().interactionProcessor.getInteractionType() == InteractionType.GRAVITATION)
			isGravityFieldMap = true;
		else if (getInstance().interactionProcessor.getInteractionType() == InteractionType.COULOMB)
			isGravityFieldMap = false;
		int ui = (Simulation.getInstance().isActive()) ? updateInterval : 15;
		if (updatesNumber >= ui) {
			adjustRange();
			updatesNumber = 0;
			int wSteps = (int) (width / resolution);
			int hSteps = (int) (height / resolution);
			for (int stepX = 0; stepX <= wSteps; stepX++) {
				for (int stepY = 0; stepY <= hSteps; stepY++) {
					double xc = (CoordinateConverter.fromScreenX(stepX * resolution)
							+ CoordinateConverter.fromScreenX((stepX + 1) * resolution)) / 2;
					double yc = (CoordinateConverter.fromScreenY(stepY * resolution)
							+ CoordinateConverter.fromScreenY((stepY + 1) * resolution)) / 2;
					Vector fieldVector = defineField(xc, yc);
					double fieldValue = defineProjection(fieldVector);
					heatMapCanvas.setColor(defineColor(fieldValue, range));
					heatMapCanvas.fill(
							new Rectangle2D.Double(stepX * resolution, stepY * resolution, resolution, resolution));
//					viewport.drawArrowLine(heatMapCanvas, (int) (stepX + 0.5) * resolution, (int) (stepY + 0.5) * resolution,
//							fieldVector.multiply(0.0005), Color.BLACK, "");
					if (fieldValue > maxValue)
						maxValue = fieldValue;
					else if (fieldValue < minValue)
						minValue = fieldValue;
				}
			}
			minField = minValue;
			maxField = maxValue;
		}
	}

	private void adjustRange() {
		if (isAdaptiveRange) {
			minValue = Double.MAX_VALUE;
			maxValue = Double.MIN_VALUE;
			range = (Math.max(minField, maxField) - Math.min(minField, maxField)) / AUTORANGE_VALUE_DIVIDER;
		}
	}

	public Vector defineField(double x, double y) {
		Particle testParticle;
		pairForce = PairForceFactory.getCentralForce(getInstance().interactionProcessor.getInteractionType());
		Vector field = new Vector();
		double distance;
		double dF = 0;
		int pNumber = 0;
		while (Simulation.getInstance().content().particle(pNumber) != null) {
			testParticle = Simulation.getInstance().content().particle(pNumber);
			distance = Functions.defineDistance(testParticle, x, y);
			if (distance >= 0.9 * testParticle.getRadius()) {
				if (mapType == HeatMapType.POTENTIAL)
					dF = pairForce.calculatePotential(testParticle, distance);
				else if (mapType == HeatMapType.STRENGTH)
					dF = pairForce.calculateStrength(testParticle, distance);
				field.addToX(dF * (x - testParticle.getX()) / distance);
				field.addToY(dF * (y - testParticle.getY()) / distance);
			}
			pNumber++;
		}
		return field;
	}

	public Vector defineField(int x, int y) {
		return defineField(CoordinateConverter.fromScreenX(x), CoordinateConverter.fromScreenY(y));
	}

	public double defineProjection(Vector v) {
		double result = 0;
		if (projectionType == ProjectionType.MAGNITUDE)
			result = v.norm();
		else if (projectionType == ProjectionType.X)
			result = v.X();
		if (projectionType == ProjectionType.Y)
			result = v.Y();
		return result;
	}

	public Color defineColor(double value, double range) {
		Color c1;
		int colorIndex;
		colorIndex = (isGravityFieldMap) ? (int) Functions.linear2DInterpolation(0, 0, range, 255, value)
				: (int) Functions.linear2DInterpolation(-range / 2, 0, range / 2, 255, value);
		c1 = new Color(Colors.TURBO_SRGB_BYTES[colorIndex][0], Colors.TURBO_SRGB_BYTES[colorIndex][1],
				Colors.TURBO_SRGB_BYTES[colorIndex][2]);
		return c1;
	}

	public int getUpdateInterval() {
		return updateInterval;
	}

	public void setUpdateInterval(int updateInterval) {
		this.updateInterval = updateInterval;
	}

	public int getResolution() {
		return resolution;
	}

	public void setResolution(int resolution) {
		this.resolution = resolution;
	}

	public double getRange() {
		return range;
	}

	public void setRange(double range) {
		this.range = range;
	}

	public HeatMapType getMapType() {
		return mapType;
	}

	public void setMapType(HeatMapType mapType) {
		this.mapType = mapType;
	}

	public ProjectionType getProjectionType() {
		return projectionType;
	}

	public void setProjectionType(ProjectionType projectionType) {
		this.projectionType = projectionType;
	}

}
