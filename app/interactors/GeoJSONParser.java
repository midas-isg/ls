package interactors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import play.Logger;
import models.geo.Feature;
import models.geo.FeatureCollection;
import models.geo.FeatureGeometry;
import models.geo.GeometryCollection;
import models.geo.MultiPolygon;
import models.geo.Point;
import models.geo.Polygon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GeoJSONParser {
	private GeoJSONParser() {
		return;
	}
	
	public static FeatureCollection parse(JsonNode inputJsonNode) throws Exception {
		FeatureCollection featureCollection = new FeatureCollection();
		featureCollection.setType(inputJsonNode.get("type").textValue());
		Map<String, Object> fcProperties = new HashMap<>();
		toProperties(fcProperties, inputJsonNode);
		featureCollection.setProperties(fcProperties);
		
		JsonNode featuresArrayNode = inputJsonNode.withArray("features");
		for(int i = 0; i < featuresArrayNode.size(); i++) {
			Map<String, Object> properties = new HashMap<>();
			FeatureGeometry geometry;
			
			JsonNode currentNode = featuresArrayNode.get(i);
			Feature feature = new Feature();
			feature.setType(currentNode.get("type").textValue());
			toProperties(properties, currentNode);
			
			try {
				String type = currentNode.get("geometry").get("type").textValue();
				switch(type) {
					case "MultiPolygon":
						geometry = parseMultiPolygon(currentNode.get("geometry"));
					break;
					
					case "Polygon":
						geometry = parsePolygon(currentNode.get("geometry"));
					break;
					
					case "GeometryCollection":
						geometry = parseGeometryCollection(currentNode.get("geometry"));
					break;
					
					case "Point":
						geometry = parsePoint(currentNode.get("geometry"));
					break;
					
					case "LineString":
					case "MultiLineString":
					case "MultiPoint":
					default:
						throw (new RuntimeException("Unsupported Geometry: " + type + "\n"));
				}
			}
			catch(Exception e) {
				throw(e);
			}
			
			feature.setProperties(properties);
			feature.setGeometry(geometry);
			featureCollection.getFeatures().add(feature);
		}
		
		return featureCollection;
	}

	private static void toProperties(Map<String, Object> map, JsonNode currentNode) {
		JsonNode properties = currentNode.get("properties");
		if (properties == null) {
			return;
		}
		
		Iterator<Entry<String, JsonNode>> propertiesIterator = properties.fields();
		while(propertiesIterator.hasNext()) {
			Entry<String, JsonNode> mapping = propertiesIterator.next();
			map.put(mapping.getKey(), mapping.getValue().textValue());
		}
		
		return;
	}
	
	private static Point parsePoint(JsonNode geometryNode) {
		JsonNode coordinatesNode = geometryNode.get("coordinates");
		Point point = new Point();
		double [] coordinates = new double[2];
		coordinates[0] = coordinatesNode.get(0).doubleValue();
		coordinates[1] = coordinatesNode.get(1).doubleValue();
		point.setCoordinates(coordinates);
		
		return point;
	}
	
	private static List<List<double []>> getCoordinatesForPolygon(JsonNode geometryNode) throws Exception {
		JsonNode coordinatesNode = geometryNode.get("coordinates");
		
		if(coordinatesNode == null) {
			throw new Exception("Malformed Polygon");
		}
		
		List<List<double []>> coordinates = new ArrayList<List<double[]>>();
		
		for(int i = 0; i < coordinatesNode.size(); i++) {
			coordinates.add(new ArrayList<double[]>());
			List<double []> componentToFill = coordinates.get(i);
			
			JsonNode polygonComponentNode = coordinatesNode.get(i);
			for(int j = 0; j < polygonComponentNode.size(); j++) {
				JsonNode coordinateValues = polygonComponentNode.get(j);
				componentToFill.add(new double[coordinateValues.size()]);
				double[] pointToFill = componentToFill.get(j);
				
				for(int k = 0; k < coordinateValues.size(); k++){
					pointToFill[k] = coordinateValues.get(k).asDouble();
				}
				
//if((j < 2) || (j == polygonComponentNode.size() - 1)) {
//	Logger.debug("[" + String.valueOf(pointToFill[0]) + ", " + String.valueOf(pointToFill[1]) + "]");
//}
			}
			
			double [] firstPoint = componentToFill.get(0).clone();
			double [] lastPoint = componentToFill.get(componentToFill.size() - 1);
			for(int l = 0; l < firstPoint.length; l++) {
				if(firstPoint[l] != lastPoint[l]) {
					//componentToFill.add(firstPoint);
//Logger.debug("~Appended first point~");
//					break;
					throw new Exception("Last point != first point for this polygon");
				}
			}
		}
		
//Logger.debug("coordinates size: " + coordinates.size());
//Logger.debug("coordinates[0] size: " + coordinates.get(0).size());
//Logger.debug("coordinates[0][0] length: " + coordinates.get(0).get(0).length);
		
		return coordinates;
	}
	
	private static Polygon parsePolygon(JsonNode geometryNode) throws Exception {
		Polygon polygon;
		
		if(geometryNode.get("type").textValue().equals(Polygon.class.getSimpleName())) {
			polygon = new Polygon();
			List<List<double[]>> coordinates = getCoordinatesForPolygon(geometryNode);
			polygon.setCoordinates(coordinates);
		}
		else {
			throw new Exception("Not Polygon");
		}
		
		return polygon;
	}
	
	private static MultiPolygon parseMultiPolygon(JsonNode geometryNode) throws Exception {
		JsonNode coordinatesNode = geometryNode.get("coordinates");
		MultiPolygon multiPolygon;
		
		if(geometryNode.get("type").textValue().equals(MultiPolygon.class.getSimpleName())) {
			multiPolygon = new MultiPolygon();
			List<List<List<double []>>> coordinates = new ArrayList<>();
			
			for(int i = 0; i < coordinatesNode.size(); i++) {
				ObjectNode polygonNode = new ObjectNode(JsonNodeFactory.instance);
				polygonNode.put("type", Polygon.class.getSimpleName());
				polygonNode.put("coordinates", coordinatesNode.get(i));
				Polygon polygon = parsePolygon(polygonNode);
				
				coordinates.add(polygon.getCoordinates());
			}
			
			multiPolygon.setCoordinates(coordinates);
		}
		else {
			throw new Exception("Not MultiPolygon");
		}
		
		return multiPolygon;
	}
	
	private static GeometryCollection parseGeometryCollection(JsonNode geometryNode) throws Exception {
		GeometryCollection geometryCollection;
		
		if(geometryNode.get("type").textValue().equals(GeometryCollection.class.getSimpleName())) {
			geometryCollection = new GeometryCollection();
			List<FeatureGeometry> geometries = new ArrayList<FeatureGeometry>();
			JsonNode geometriesNode = geometryNode.get("geometries");
/*
Logger.debug("GeometryCollection fields: ");
Iterator<String> fields = geometryNode.fieldNames();
while(fields.hasNext()) {
	Logger.debug("\t" + fields.next());
}
*/
			int geometryCount = geometriesNode.size();
			for(int i = 0; i < geometryCount; i++) {
				JsonNode geometryToAdd = geometriesNode.get(i);
				String type = geometryToAdd.get("type").asText();
				
				switch(type) {
					case "MultiPolygon":
						MultiPolygon multipolygon = parseMultiPolygon(geometryToAdd);
						geometries.add(multipolygon);
					break;
					
					case "Polygon":
						Polygon polygon = parsePolygon(geometryToAdd);
						geometries.add(polygon);
					break;
					
					case "Point":
						Point point = parsePoint(geometryToAdd);
						geometries.add(point);
					break;
					
					default:
						throw new Exception(type + " not supported");
				}
			}
			
			geometryCollection.setGeometries(geometries);
		}
		else {
			throw new Exception(geometryNode.get("type").textValue() + "not supported");
		}
		
		return geometryCollection;
	}
}
