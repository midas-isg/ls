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
import models.geo.MultiPolygon;
import models.geo.Polygon;

import com.fasterxml.jackson.databind.JsonNode;

public class GeoJSONParser {
	private GeoJSONParser() {
		return;
	}
	
	public static FeatureCollection parse(JsonNode inputJsonNode) throws Exception {
		FeatureCollection featureCollection = new FeatureCollection();
		//featureCollection.setId(inputJsonNode.get("id").textValue());
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
					
					case "Point":
					case "MultiLine":
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

	private static void toProperties(Map<String, Object> map,
			JsonNode currentNode) {
		JsonNode properties = currentNode.get("properties");
		if (properties == null)
			return;
		Iterator<Entry<String, JsonNode>> propertiesIterator = properties.fields();
		while(propertiesIterator.hasNext()) {
			Entry<String, JsonNode> mapping = propertiesIterator.next();
			map.put(mapping.getKey(), mapping.getValue().textValue());
		}
	}
	
	/*
	private static Point parsePoint(JsonNode coordinatesNode) {
		Point point = new Point();
			point.setLongitude(coordinatesNode.get(0).asDouble());
			point.setLatitude(coordinatesNode.get(1).asDouble());
		return point;
	}
	*/
	
	private static List<List<double []>> getCoordinatesForPolygon(JsonNode geometryNode) {
		JsonNode coordinatesNode = geometryNode.withArray("coordinates");
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
		JsonNode coordinatesNode = geometryNode.withArray("coordinates");
		MultiPolygon multiPolygon;
		
		if(geometryNode.get("type").textValue().equals(MultiPolygon.class.getSimpleName())) {
			multiPolygon = new MultiPolygon();
			List<List<List<double []>>> coordinates = new ArrayList<>();
			
			for(int i = 0; i < coordinatesNode.size(); i++) {
				coordinates.add(getCoordinatesForPolygon(coordinatesNode.get(i)));
				
				/*
				coordinates.add(new ArrayList<List<double[]>>());
				List<List<double []>> polygonToFill = coordinates.get(i);
				
				JsonNode polygon = coordinatesNode.get(i);
				for(int j = 0; j < polygon.size(); j++) {
					polygonToFill.add(new ArrayList<double[]>());
					List<double []> componentToFill = polygonToFill.get(j);
					
					JsonNode polygonComponent = polygon.get(j);
					for(int k = 0; k < polygonComponent.size(); k++){
						componentToFill.add(new double[polygonComponent.get(k).size()]);
						double[] pointToFill = componentToFill.get(k);
						
						JsonNode point = polygonComponent.get(k);
						for(int l = 0; l < point.size(); l++) {
							//point.add(points.get(k).get(l).asDouble());
							pointToFill[l] = point.get(l).asDouble();
						}
					}
				}
				*/
			}
			
			multiPolygon.setCoordinates(coordinates);
		}
		else {
			throw new Exception("Not MultiPolygon");
		}
		
		return multiPolygon;
	}
}
