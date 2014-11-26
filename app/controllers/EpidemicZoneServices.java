package controllers;

import interactors.AuRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Logger;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import dao.entities.AdministrativeUnit;

public class EpidemicZoneServices  extends Controller {
	
	@Transactional
	public static Result locations(String gid){
		AdministrativeUnit au = AuRule.findByGid(Long.parseLong(gid));
		Long auTypeId = au.getData().getAuTypeId();
		Logger.debug("AuTypeId=" + auTypeId);
		if (auTypeId.equals(7))
			return okJson(toEpidemicZones(au));
		else 
			return okJson(toAdministrativeLocations(au));
	}
	
	@Transactional
	public static Result epidemicZones(String gid) {
		AdministrativeUnit ez = AuRule.findByGid(Long.parseLong(gid));
		return okJson(toEpidemicZones(ez));
	}

	private static Status okJson(Object result) {
		return ok(Json.toJson(result));
	}

	private static Object toEpidemicZones(AdministrativeUnit ez) {
		return new Object[] {new MultiPolygon(ez)};
	}
	
	private static class MultiPolygon {
		public String textualDescription;
		public List<Object> polygons;
		
		public MultiPolygon(AdministrativeUnit au){
			textualDescription = au.getData().getName();
			polygons =new ArrayList<>();
			Geometry mpg = au.getData().getMultiPolygonGeom();
			int l = mpg.getNumGeometries();
			for (int i = 0; i < l; i++){
				Geometry p = mpg.getGeometryN(i);
				polygons.add(new Polygon(p));
			}
		}
	}
	
	private static class Polygon {
		public String linearRing ;

		public Polygon(Geometry polygon){
			linearRing = "";
			String space = "";
			Coordinate[] coordinates = polygon.getCoordinates();
			for (Coordinate c : coordinates){
				linearRing += space + c.x + "," + c.y + ",0";
				space = " ";
			}
		}
	}
	
	@Transactional
	public static Result administrativeLocations(String gid) {
		AdministrativeUnit au = AuRule.findByGid(Long.parseLong(gid));
		return okJson(toAdministrativeLocations(au));
	}

	private static Object toAdministrativeLocations(AdministrativeUnit au) {
		Map<String, Object> map = new HashMap<>();
		map.put("locationDefinition", new LocationDefinition(au));
		return new Object[] {map};
	}
	
	private static class LocationDefinition {
		public List<String> locationsIncluded = new ArrayList<>();
		public List<String> locationsExcluded = new ArrayList<>();
		public List<MultiPolygon> multiGeometries = new ArrayList<>();

		public LocationDefinition(AdministrativeUnit au){
			locationsIncluded.add(au.getData().getCode());
			multiGeometries.add(new MultiPolygon(au));
		}
	}

}

