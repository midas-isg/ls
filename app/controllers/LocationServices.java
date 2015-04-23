package controllers;

import com.wordnik.swagger.annotations.*;
import interactors.GeoJsonRule;
import interactors.GeometryRule;
import interactors.KmlRule;
import interactors.LocationProxyRule;
import interactors.LocationRule;

import java.math.BigInteger;
import java.util.List;

import models.geo.FeatureCollection;
import models.geo.FeatureGeometry;
import play.Logger;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import dao.entities.Location;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

@Api(value = "/api/locations", description = "Endpoint for locations")
public class LocationServices extends Controller {
	public static final String FORMAT_GEOJSON = "geojson";
	public static final String FORMAT_APOLLOJSON = "json";
	public static final String FORMAT_APOLLOXML = "xml";
	public static final String FORMAT_KML = "kml";
	public static final String FORMAT_DEFAULT = "geojson";
	
	@Transactional
    @ApiOperation(
      nickname = "getLocation",
      value = "Returns a location",
      notes = "",
      httpMethod = "GET",
      response = FeatureCollection.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Successful retrieval of location", response = FeatureCollection.class),
        @ApiResponse(code = 404, message = "Location not found"),
        @ApiResponse(code = 500, message = "Internal server error"),
        @ApiResponse(code = 400, message = "Format is not supported")}
    )
    public static Result locations(
            @ApiParam(value = "ID of the location", required = true) @PathParam("gid") Long gid,
            @ApiParam(value = "Requested serialization format (geojson, json, xml, or kml)", allowableValues = "[geojson, json, xml, kml]") @QueryParam("format") String format,
            @ApiParam(value = "Number of exterior rings (integer)", required = false) @QueryParam("maxExteriorRings") Integer maxExteriorRings) {

		if (gid == null)
			return notFound("gid is required but got " + gid);
		
		if (IsFalsified(format))
			format = FORMAT_DEFAULT;
		
		Location location = Wire.simplifyToMaxExteriorRings(gid, maxExteriorRings);
		switch (format.toLowerCase()){
		case FORMAT_GEOJSON:
			return asGeoJson(location);
		case FORMAT_APOLLOJSON:
			return ApolloLocationServices.asJson(location);
		case FORMAT_APOLLOXML:
			return ApolloLocationServices.asXml(location); 
		case FORMAT_KML:
			return asKml(location);
		default:
			return badRequest(format + " is not supported.");
		}
	}

	@Transactional
	public static Result getGeometryMetadata(long gid, Double tolerance){
		Object object = LocationRule.getSimplifiedGeometryMetadata(gid, tolerance);
		return ok(Json.toJson(object));
	}
	
	private static boolean IsFalsified(String text) {
		return text == null || text.isEmpty();
	}
	
	@Transactional
    @ApiOperation(
      nickname = "locationsByName",
      value = "Returns a location by searching their name",
      notes = "",
      httpMethod = "GET",
      response = FeatureCollection.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Successful retrieval of location", response = FeatureCollection.class),
        @ApiResponse(code = 404, message = "Location not found"),
        @ApiResponse(code = 500, message = "Internal server error"),
        @ApiResponse(code = 400, message = "Format is not supported")}
    )
	public static Result findLocations(
            @ApiParam(value = "Search terms", required = true) @QueryParam("q") String q,
            @ApiParam(value = "Number of locations to return", required = false) @QueryParam("limit") Integer limit,
            @ApiParam(value = "Page offset if number of locations exceeds limit", required = false) @QueryParam("offset") Integer offset){
		Object result = GeoJsonRule.findByName(q, limit, offset);
		return ok(Json.toJson(result));
	}
	
	@Transactional
	public static Result findLocationNames(String q, Integer limit){
		Object result = LocationProxyRule.findLocationNames(q, limit);
		return ok(Json.toJson(result));
	}
	
	@Transactional
    @ApiOperation(
          nickname = "locationsByPoint",
          value = "Returns a location by submitting a coordinate",
          notes = "",
          httpMethod = "GET",
          response = FeatureCollection.class)
        @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful retrieval of location", response = FeatureCollection.class),
            @ApiResponse(code = 404, message = "Location not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 400, message = "Format is not supported")}
        )
	public static Result findLocationsByPoint(
            @ApiParam(value = "Latitude", required = true) @QueryParam("lat") double lat,
            @ApiParam(value = "Longitude", required = true) @QueryParam("lon") double lon){
		Object result = GeoJsonRule.findByNameByPoint(lat, lon);
		return ok(Json.toJson(result));
	}
	
	@Transactional
	public static Result create() {
		try {
			FeatureCollection parsed = parseRequestAsFeatureCollection();
			Long id = Wire.create(parsed);
			setResponseLocation(id);
			
			return created();
		}
		catch (RuntimeException e) {
			String message = e.getMessage();
			Logger.error(message, e);
			
			return badRequest(message);
		}
		catch (Exception e) {
			String message = e.getMessage();
			Logger.error(message, e);
			
			return forbidden(message);
		}
	}

	private static void setResponseLocation(Long id) {
		AdministrativeUnitServices.setResponseLocation(id);
	}

	private static FeatureCollection parseRequestAsFeatureCollection()
			throws Exception {
		return AdministrativeUnitServices.parseRequestAsFeatureCollection();
	}
	
	@Transactional
	static Result asGeoJson(Location location) {
		response().setContentType("application/vnd.geo+json");
		return ok(Json.toJson(Wire.asFeatureCollection(location)));
	}
	
	@Transactional
	public static Result update(long gid) {
		try {
			FeatureCollection parsed = parseRequestAsFeatureCollection();
			Wire.update(gid, parsed);
			setResponseLocation(null);
			return noContent();
		} catch (RuntimeException e){
			String message = e.getMessage();
			Logger.error(message, e);
			return badRequest(message);
		} catch (Exception e) {
			String message = e.getMessage();
			Logger.error(message, e);
			return forbidden(message);
		}
	}
	
	@Transactional
	public static Result delete(long gid) {

        String remote = request().remoteAddress();
        if (! remote.equalsIgnoreCase("127.0.0.1")) {
            return unauthorized();
        }

		Long id = Wire.delete(gid);
		setResponseLocation(null);
		if (id == null){
			return notFound();
		}
		return noContent();
	}

	@Transactional
	public static Result asKml(Location location) {
		String result = KmlRule.asKml(location);
		response().setContentType("application/vnd.google-earth.kml+xml");
		return ok(result);
	}

	@Transactional
	public static Result findByFeatureCollection(Long superTypeId, Long typeId) throws Exception {
		FeatureCollection fc = parseRequestAsFeatureCollection();
		response().setContentType("application/vnd.geo+json");
		return Wire.findByFeatureCollection(fc, superTypeId, typeId);
	}
	
	public static class Wire {
		public static Long create(FeatureCollection fc) {
			Location location = GeoJsonRule.asLocation(fc);
			Long id = LocationRule.create(location);
			return id;
		}

		public static Location read(Long gid){
			return simplifyToMaxExteriorRings(gid, null);
		}
		
		public static Location simplifyToMaxExteriorRings(Long gid,
				Integer maxExteriorRings) {
			Location location = LocationRule.simplifyToMaxExteriorRings(gid, maxExteriorRings);
			return location;
		}
		
		public static FeatureCollection asFeatureCollection(Location location) {
			FeatureCollection fc = GeoJsonRule.asFeatureCollection(location);
			return fc;
		}
		
		public static Long update(long gid, FeatureCollection fc) {
			Location location = GeoJsonRule.asLocation(fc);
			return LocationRule.update(gid, location);
		}
		
		public static Long delete(long gid) {
			Long id = LocationRule.deleteTogetherWithAllGeometries(gid);
			return id;
		}

		public static Result findByFeatureCollection(FeatureCollection fc, Long superTypeId, Long typeId) {
			FeatureGeometry geometry = GeoJsonRule.asFetureGeometry(fc);
			String geo = Json.toJson(geometry).toString();
			List<BigInteger> gids = GeometryRule.findGidsByGeometry(geo, superTypeId, typeId);
			List<Location> locations = LocationProxyRule.getLocations(gids);
			return ok(Json.toJson(GeoJsonRule.toFeatureCollection(locations, GeoJsonRule.MINIMUM_KEYS)));
		}
	}
}
