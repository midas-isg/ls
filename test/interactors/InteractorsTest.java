package interactors;
import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import models.geo.Feature;
import models.geo.FeatureCollection;
import models.geo.FeatureGeometry;
import models.geo.GeometryCollection;
import models.geo.MultiPolygon;
import models.geo.Polygon;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import play.Logger;
import play.mvc.Http.RequestBody;

/**
* Simple (JUnit) tests that can test parts of the interactors package.
*/
public class InteractorsTest {
	static JsonNode polygon;
	static JsonNode holeyPolygon;
	static JsonNode multiPolygon;
	static JsonNode holeyMultiPolygon;
	static JsonNode geometryCollection;
	static JsonNode malformed;
	
	private static void geoJSONInitialize() {
		String polygonString = "{\"type\": \"FeatureCollection\",\"features\":[{\"type\": \"Feature\",\"geometry\":{\"type\": \"Polygon\",\"coordinates\":[[[35, 10], [45, 45], [15, 40], [10, 20], [35, 10]]]}}]}";
		String holeyPolygonString = "{\"type\": \"FeatureCollection\",\"features\":[{\"type\": \"Feature\",\"geometry\":{\"type\": \"Polygon\", \"coordinates\":[[[35, 10], [45, 45], [15, 40], [10, 20], [35, 10]], [[20, 30], [35, 35], [30, 20], [20, 30]]]}}]}";
		String multiPolygonString = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"MultiPolygon\",\"coordinates\":[[[[30,20],[45,40],[10,40],[30,20]]],[[[15,5],[40,10],[10,20],[5,10],[15,5]]]]}}]}";
		String holeyMultiPolygonString = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"MultiPolygon\",\"coordinates\":[[[[40,40],[20,45],[45,30],[40,40]]],[[[20,35],[10,30],[10,10],[30,5],[45,20],[20,35]],[[30,20],[20,15],[20,25],[30,20]]]]}}]}";
		String geometryCollectionString = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"MultiPolygon\",\"coordinates\":[[[[40,40],[20,45],[45,30],[40,40]]],[[[20,35],[10,30],[10,10],[30,5],[45,20],[20,35]],[[30,20],[20,15],[20,25],[30,20]]]]},{\"type\":\"Polygon\",\"coordinates\":[[[100,0],[101,0],[101,1],[100,1],[100,0]]]}]}}]}";
		String malformedString = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"MultiPolygon\",\"coordinates\":[[[100,0],[101,0],[101,1],[100,1],[100,0]]]}]}}]}";
		
		ObjectMapper mapper = new ObjectMapper();
		try {
			polygon = mapper.readTree(polygonString);
			holeyPolygon = mapper.readTree(holeyPolygonString);
			multiPolygon = mapper.readTree(multiPolygonString);
			holeyMultiPolygon = mapper.readTree(holeyMultiPolygonString);
			geometryCollection = mapper.readTree(geometryCollectionString);
			malformed = mapper.readTree(malformedString);
		}
		catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return;
	}
	
	@Test
	public void simpleCheck() {
		int a = 1 + 1;
		assertThat(a).isEqualTo(2);
	}
	
	@Test
	public void geoJSONParseTest()
	{
		geoJSONInitialize();
		
		FeatureCollection fcPolygon;
		FeatureCollection fcHoleyPolygon;
		FeatureCollection fcMultiPolygon;
		FeatureCollection fcHoleyMultiPolygon;
		FeatureCollection fcGeometryCollection;
		FeatureCollection fcMalformed;
		
		try {
			fcPolygon = GeoJSONParser.parse(polygon);
			Feature polygonFeature = fcPolygon.getFeatures().get(0);
			Polygon polygonGeometry = (Polygon) polygonFeature.getGeometry();
			assertThat(polygonGeometry.getType()).isEqualTo("Polygon");
			assertThat(polygonGeometry.getCoordinates().get(0).size()).isEqualTo(5);
			
			fcHoleyPolygon = GeoJSONParser.parse(holeyPolygon);
			Feature holeyPolygonFeature = fcHoleyPolygon.getFeatures().get(0);
			Polygon holeyPolygonGeometry = (Polygon) holeyPolygonFeature.getGeometry();
			assertThat(holeyPolygonGeometry.getType()).isEqualTo("Polygon");
			assertThat(holeyPolygonGeometry.getCoordinates().size()).isEqualTo(2);
			assertThat(holeyPolygonGeometry.getCoordinates().get(0).size()).isEqualTo(5);
			assertThat(holeyPolygonGeometry.getCoordinates().get(1).size()).isEqualTo(4);
			
			fcMultiPolygon = GeoJSONParser.parse(multiPolygon);
			Feature multiPolygonFeature = fcMultiPolygon.getFeatures().get(0);
			MultiPolygon multiPolygonGeometry = (MultiPolygon) multiPolygonFeature.getGeometry();
			assertThat(multiPolygonGeometry.getType()).isEqualTo("MultiPolygon");
			assertThat(multiPolygonGeometry.getCoordinates().size()).isEqualTo(2);
			assertThat(multiPolygonGeometry.getCoordinates().get(0).size()).isEqualTo(1);
			assertThat(multiPolygonGeometry.getCoordinates().get(0).get(0).size()).isEqualTo(4);
			assertThat(multiPolygonGeometry.getCoordinates().get(1).size()).isEqualTo(1);
			assertThat(multiPolygonGeometry.getCoordinates().get(1).get(0).size()).isEqualTo(5);
			
			fcHoleyMultiPolygon = GeoJSONParser.parse(holeyMultiPolygon);
			Feature holeyMultiPolygonFeature = fcHoleyMultiPolygon.getFeatures().get(0);
			MultiPolygon holeyMultiPolygonGeometry = (MultiPolygon) holeyMultiPolygonFeature.getGeometry();
			assertThat(holeyMultiPolygonGeometry.getType()).isEqualTo("MultiPolygon");
			assertThat(holeyMultiPolygonGeometry.getCoordinates().size()).isEqualTo(2);
			assertThat(holeyMultiPolygonGeometry.getCoordinates().get(0).size()).isEqualTo(1);
			assertThat(holeyMultiPolygonGeometry.getCoordinates().get(0).get(0).size()).isEqualTo(4);
			assertThat(holeyMultiPolygonGeometry.getCoordinates().get(1).size()).isEqualTo(2);
			assertThat(holeyMultiPolygonGeometry.getCoordinates().get(1).get(0).size()).isEqualTo(6);
			assertThat(holeyMultiPolygonGeometry.getCoordinates().get(1).get(1).size()).isEqualTo(4);
			
			fcGeometryCollection = GeoJSONParser.parse(geometryCollection);
			Feature geometryCollectionFeature = fcGeometryCollection.getFeatures().get(0);
			GeometryCollection geometryCollectionGeometry = (GeometryCollection) geometryCollectionFeature.getGeometry();
			List<FeatureGeometry> geometries = geometryCollectionGeometry.getGeometries();
			assertThat(geometryCollectionGeometry.getType()).isEqualTo("GeometryCollection");
			assertThat(geometries.size()).isEqualTo(2);
			assertThat(geometries.get(0).getType()).isEqualTo("MultiPolygon");
			assertThat(((MultiPolygon)(geometries.get(0))).getCoordinates().size()).isEqualTo(2);
			assertThat(((MultiPolygon)(geometries.get(0))).getCoordinates().get(0).size()).isEqualTo(1);
			assertThat(((MultiPolygon)(geometries.get(0))).getCoordinates().get(0).get(0).size()).isEqualTo(4);
			assertThat(((MultiPolygon)(geometries.get(0))).getCoordinates().get(1).size()).isEqualTo(2);
			assertThat(((MultiPolygon)(geometries.get(0))).getCoordinates().get(1).get(0).size()).isEqualTo(6);
			assertThat(((MultiPolygon)(geometries.get(0))).getCoordinates().get(1).get(1).size()).isEqualTo(4);
			assertThat(geometries.get(1).getType()).isEqualTo("Polygon");
			assertThat(((Polygon)(geometries.get(1))).getCoordinates().size()).isEqualTo(1);
			assertThat(((Polygon)(geometries.get(1))).getCoordinates().get(0).size()).isEqualTo(5);
			
			fcMalformed = GeoJSONParser.parse(malformed);
			//TODO: malformed contains a polygon that is mislabeled as a multipolygon
			//but the current code can still process it; it should throw an error instead
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return;
	}

}
