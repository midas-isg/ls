package interactors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import play.Logger;
import models.geo.*;

import com.fasterxml.jackson.databind.JsonNode;

public class GeoJSONParser {
	private GeoJSONParser() {
		return;
	}
	
	public static FeatureCollection parse(JsonNode inputJsonNode) {
		FeatureCollection featureCollection = new FeatureCollection();
		featureCollection.id = inputJsonNode.get("id").textValue();
		featureCollection.type = inputJsonNode.get("type").textValue();
		
		JsonNode featuresArrayNode = inputJsonNode.withArray("features");
		
		for(int i = 0; i < featuresArrayNode.size(); i++) {
			JsonNode currentNode = featuresArrayNode.get(i);
			Feature feature = new Feature();
			feature.type = currentNode.get("type").textValue();
			
			Iterator<Entry<String, JsonNode>> propertiesIterator = currentNode.get("properties").fields();
			while(propertiesIterator.hasNext()) {
				Entry<String, JsonNode> mapping = propertiesIterator.next();
				feature.properties.put(mapping.getKey(), mapping.getValue().textValue());
			}
			
			String type = currentNode.get("geometry").get("type").textValue();
			switch(type){
			case "MultiPolygon":
				feature.geometry = parseMultiPolygon(currentNode.get("geometry"));
				break;
				
			case "Point":
			case "MultiLine":
			case "Polygon":
			default:
				throw (new RuntimeException("Unsupported Geometry: " + type + "\n"));
			}
			
			featureCollection.features.add(feature);
		}
		
		return featureCollection;
	}
	
	/*
	private static Point parsePoint(JsonNode coordinatesNode) {
		Point point = new Point();
			point.setLongitude(coordinatesNode.get(0).asDouble());
			point.setLatitude(coordinatesNode.get(1).asDouble());
		return point;
	}
	*/
	
	private static MultiPolygon parseMultiPolygon(JsonNode geometryNode) {
		JsonNode coordinatesNode = geometryNode.withArray("coordinates");
		
		MultiPolygon multiPolygon = new MultiPolygon();
		multiPolygon.type = geometryNode.get("type").textValue();
		
		for(int i = 0; i < coordinatesNode.size(); i++) {
			multiPolygon.coordinates.add(new ArrayList<>());
			List<List<double []>> polygonToFill = multiPolygon.coordinates.get(i);
			
			JsonNode polygon = coordinatesNode.get(i);
			for(int j = 0; j < polygon.size(); j++) {
				polygonToFill.add(new ArrayList<>());
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
		}
		
		return multiPolygon;
	}
}
