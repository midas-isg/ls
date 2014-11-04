package models.geo;

import java.util.ArrayList;
import java.util.List;

public class MultiPolygon extends FeatureGeometry {
	public String type = MultiPolygon.class.getSimpleName();
	public List<List<List<double[]>>> coordinates = new ArrayList<>();
}
