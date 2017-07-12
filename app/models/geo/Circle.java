package models.geo;

public class Circle extends FeatureGeometry {

	private com.vividsolutions.jts.geom.Point center;
	private Double radius;
	private Integer quarterSegments = 8;

	public Circle() {
		setType(Circle.class.getSimpleName());

		return;
	}

	public com.vividsolutions.jts.geom.Point getCenter() {
		return center;
	}

	public void setCenter(com.vividsolutions.jts.geom.Point center) {
		this.center = center;
	}

	public Double getRadius() {
		return radius;
	}

	public void setRadius(Double radius) {
		this.radius = radius;
	}

	public Integer getQuarterSegments() {
		return quarterSegments;
	}

	public void setQuarterSegments(Integer quarterSegments) {
		this.quarterSegments = quarterSegments;
	}
}
