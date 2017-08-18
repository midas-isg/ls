package interactors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import models.FeatureKey;
import models.exceptions.BadRequest;
import models.geo.Circle;
import models.geo.Feature;
import models.geo.FeatureCollection;
import models.geo.FeatureGeometry;
import models.geo.GeometryCollection;
import models.geo.MultiPolygon;
import models.geo.Polygon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

import dao.entities.AltName;
import dao.entities.Code;

public class GeoJSONParser {
	private GeoJSONParser() {
		return;
	}

	public static FeatureCollection parse(JsonNode inputJsonNode) throws Exception {
		FeatureCollection featureCollection = new FeatureCollection();
		featureCollection.setType(inputJsonNode.get("type").textValue());
		Map<String, Object> fcProperties = toProperties(inputJsonNode);//new HashMap<>();
		//toProperties(fcProperties, inputJsonNode);
		featureCollection.setProperties(fcProperties);

		JsonNode featuresArrayNode = inputJsonNode.withArray("features");
		for (int i = 0; i < featuresArrayNode.size(); i++) {
			//Map<String, Object> properties = new HashMap<>();
			FeatureGeometry geometry = null;

			JsonNode currentNode = featuresArrayNode.get(i);
			Feature feature = new Feature();
			feature.setType(currentNode.get("type").textValue());
			//toProperties(properties, currentNode);
			Map<String, Object> properties = toProperties(currentNode);

			JsonNode currentGeometry = currentNode.get("geometry");
			if (!currentGeometry.isNull()) {
				String type = currentGeometry.get("type").textValue();
				switch (type) {
				case "MultiPolygon":
					geometry = parseMultiPolygon(currentGeometry);
					break;

				case "Polygon":
					geometry = parsePolygon(currentGeometry);
					break;

				case "GeometryCollection":
					geometry = parseGeometryCollection(currentGeometry);
					break;

				case "Point":
					geometry = parsePoint(currentGeometry);
					break;

				case "Circle":
					geometry = parseCircle(currentGeometry);
					break;

				case "LineString":
				case "MultiLineString":
				case "MultiPoint":
				default:
					throw (new RuntimeException("Unsupported Geometry: " + type + "\n"));
				}
			}
			String id = null;
			JsonNode currentId = currentNode.get("id");
			if (currentId != null && !currentId.isNull()) {
				id = currentId.textValue();
			}

			feature.setId(id);
			feature.setProperties(properties);
			feature.setGeometry(geometry);
			featureCollection.getFeatures().add(feature);
		}

		return featureCollection;
	}

	private static Map<String, Object> toProperties(/*Map<String, Object> properties, */JsonNode inputJson) {
		JsonNode propertiesNode = inputJson.get("properties");
		if (propertiesNode == null)
			return null;
		
		ObjectMapper mapper = new ObjectMapper();
		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) mapper.convertValue(propertiesNode, Map.class);
		
		return result;

//		Iterator<Entry<String, JsonNode>> propertiesIterator = propertiesNode.fields();
//		while (propertiesIterator.hasNext()) {
//			Entry<String, JsonNode> mapping = propertiesIterator.next();
//			if (mapping.getValue().isArray())
//				toArrayProperties(properties, mapping);
//				//map.put(mapping.getKey(), mapping.getValue().textValue()))
//			else
//				properties.put(mapping.getKey(), mapping.getValue().textValue());
//		}

		//return;
	}

	private static void toArrayProperties(Map<String, Object> map, Entry<String, JsonNode> mapping) {
		JsonNode arrayNode = mapping.getValue();
		List<AltName> altNames = new ArrayList<>();
		List<Code> codes = new ArrayList<>();
		ObjectMapper om = new ObjectMapper();
		for (int i = 0; i < arrayNode.size(); i++) {
			JsonNode jsonNode = arrayNode.get(i);
			try {
				if (mapping.getKey().equals(FeatureKey.OTHER_NAMES)) {
					AltName n = om.treeToValue(jsonNode, AltName.class);
					altNames.add(n);
				} else if (mapping.getKey().equals(FeatureKey.OTHER_CODES)) {
					Code c = om.treeToValue(jsonNode, Code.class);
					codes.add(c);
				}
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}
		if (mapping.getKey().equals("otherNames"))
			map.put("otherNames", altNames);
		else if (mapping.getKey().equals("otherCodes"))
			map.put("otherCodes", codes);
	}

	private static models.geo.Point parsePoint(JsonNode geometryNode) {
		JsonNode coordinatesNode = geometryNode.get("coordinates");
		models.geo.Point point = coordinatesNode2Point(coordinatesNode);

		return point;
	}

	private static Circle parseCircle(JsonNode geometryNode) {

		JsonNode centerNode = geometryNode.get("center");
		JsonNode radiusNode = geometryNode.get("radius");
		//JsonNode quadSegsNode = geometryNode.get("quadSegs");
		Circle circle = new Circle();

		Double radius;
		if (radiusNode == null)
			throw new BadRequest("radius is required for type Circle");
		else {
			radius = radiusNode.asDouble();
			if (radius <= 0)
				throw new BadRequest("invalid radius: " + radiusNode.asText());
		}
		circle.setRadius(radius);

		if (centerNode == null)
			throw new BadRequest("center is required for type Circle");

		double x;
		double y;
		try{
			x = centerNode.get(0).asDouble();
			y = centerNode.get(1).asDouble();
		} catch(Exception e){
			throw new BadRequest("invalid center: " + centerNode.asText());
		}
		
		Point point = toPoint(x, y);
		circle.setCenter(point);

		return circle;
	}

	private static com.vividsolutions.jts.geom.Point toPoint(double x, double y) {
		Coordinate[] coordinates = new Coordinate[1];
		coordinates[0] = new Coordinate(x, y);
		CoordinateSequence coordinate = new CoordinateArraySequence(coordinates);
		Point point = new Point((CoordinateSequence) coordinate, new GeometryFactory());
		return point;
	}

	private static models.geo.Point coordinatesNode2Point(JsonNode coordinatesNode) {
		models.geo.Point point = new models.geo.Point();
		double[] coordinates = new double[2];
		coordinates[0] = coordinatesNode.get(0).doubleValue();
		coordinates[1] = coordinatesNode.get(1).doubleValue();
		point.setCoordinates(coordinates);
		return point;
	}

	private static List<List<double[]>> getCoordinatesForPolygon(JsonNode geometryNode) throws Exception {
		JsonNode coordinatesNode = geometryNode.get("coordinates");

		if (coordinatesNode == null) {
			throw new Exception("Malformed Polygon");
		}

		List<List<double[]>> coordinates = new ArrayList<List<double[]>>();

		for (int i = 0; i < coordinatesNode.size(); i++) {
			coordinates.add(new ArrayList<double[]>());
			List<double[]> componentToFill = coordinates.get(i);

			JsonNode polygonComponentNode = coordinatesNode.get(i);
			for (int j = 0; j < polygonComponentNode.size(); j++) {
				JsonNode coordinateValues = polygonComponentNode.get(j);
				componentToFill.add(new double[coordinateValues.size()]);
				double[] pointToFill = componentToFill.get(j);

				for (int k = 0; k < coordinateValues.size(); k++) {
					pointToFill[k] = coordinateValues.get(k).asDouble();
				}
			}

			double[] firstPoint = componentToFill.get(0).clone();
			double[] lastPoint = componentToFill.get(componentToFill.size() - 1);
			for (int l = 0; l < firstPoint.length; l++) {
				if (firstPoint[l] != lastPoint[l]) {
					throw new Exception("Last point != first point for this polygon");
				}
			}
		}
		return coordinates;
	}

	private static Polygon parsePolygon(JsonNode geometryNode) throws Exception {
		Polygon polygon;

		if (geometryNode.get("type").textValue().equals(Polygon.class.getSimpleName())) {
			polygon = new Polygon();
			List<List<double[]>> coordinates = getCoordinatesForPolygon(geometryNode);
			polygon.setCoordinates(coordinates);
		} else {
			throw new Exception("Not Polygon");
		}

		return polygon;
	}

	@SuppressWarnings("deprecation")
	private static MultiPolygon parseMultiPolygon(JsonNode geometryNode) throws Exception {
		JsonNode coordinatesNode = geometryNode.get("coordinates");
		MultiPolygon multiPolygon;

		if (geometryNode.get("type").textValue().equals(MultiPolygon.class.getSimpleName())) {
			multiPolygon = new MultiPolygon();
			List<List<List<double[]>>> coordinates = new ArrayList<>();

			for (int i = 0; i < coordinatesNode.size(); i++) {
				ObjectNode polygonNode = new ObjectNode(JsonNodeFactory.instance);
				polygonNode.put("type", Polygon.class.getSimpleName());
				polygonNode.put("coordinates", coordinatesNode.get(i));
				Polygon polygon = parsePolygon(polygonNode);

				coordinates.add(polygon.getCoordinates());
			}

			multiPolygon.setCoordinates(coordinates);
		} else {
			throw new Exception("Not MultiPolygon");
		}

		return multiPolygon;
	}

	private static GeometryCollection parseGeometryCollection(JsonNode geometryNode) throws Exception {
		GeometryCollection geometryCollection;

		if (geometryNode.get("type").textValue().equals(GeometryCollection.class.getSimpleName())) {
			geometryCollection = new GeometryCollection();
			List<FeatureGeometry> geometries = new ArrayList<FeatureGeometry>();
			JsonNode geometriesNode = geometryNode.get("geometries");
			int geometryCount = geometriesNode.size();
			for (int i = 0; i < geometryCount; i++) {
				JsonNode geometryToAdd = geometriesNode.get(i);
				String type = geometryToAdd.get("type").asText();

				switch (type) {
				case "MultiPolygon":
					MultiPolygon multipolygon = parseMultiPolygon(geometryToAdd);
					geometries.add(multipolygon);
					break;

				case "Polygon":
					Polygon polygon = parsePolygon(geometryToAdd);
					geometries.add(polygon);
					break;

				case "Point":
					models.geo.Point point = parsePoint(geometryToAdd);
					geometries.add(point);
					break;

				default:
					throw new Exception(type + " not supported");
				}
			}

			geometryCollection.setGeometries(geometries);
		} else {
			throw new Exception(geometryNode.get("type").textValue() + "not supported");
		}

		return geometryCollection;
	}
}
