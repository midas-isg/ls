package controllers;

import interactors.LocationTypeRule;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.QueryParam;

import models.geo.FeatureCollection;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import dao.entities.LocationType;
import dao.entities.SuperType;

@Api(value = "/api/LocationTypes", description = "Endpoint for location Types")
public class ListServices extends Controller {
	private static final String superTypeAPI = "/api/super-types";
	
	@Transactional
	@ApiOperation(
			httpMethod = "GET", 
			nickname = "readLocationSuperTypes", 
			value = "Returns location superTypes", 
			notes = "This endpoint returns all available location super-types as location-type categories.",
			response = SuperType.class
	)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successful retrieval of location", 
					response = FeatureCollection.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
	})
	public Result findSuperTypes(){
		List<SuperType> result = LocationTypeRule.findSuperTypes();
		return okAsJson(result);
	}

	@Transactional
	@ApiOperation(
			httpMethod = "GET", 
			nickname = "readLocationTypes", 
			value = "Returns location types by superTypeId", 
			notes = "This endpoint returns all location types available for the requested superTypeId as a location-type category."
					+ "If superTypeId is not specified, returns all availabe location types with any superTypeId (location-type category)."
					+ "For more information about superTypes refer to " + superTypeAPI + " endpoint.",
			response = LocationType.class
	)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successful retrieval of location", 
					response = FeatureCollection.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
	})
	public Result findLocationTypes(
			@ApiParam(
				value = "superTypeId (Location-type category)",
				defaultValue = "",
				required = false
			)
			@QueryParam("superTypeId")
			Long superTypeId){
		List<LocationType> types = LocationTypeRule.findAllBySuperTypeId(superTypeId);
		return okAsJson(types);
	}

	private Result okAsJson(Object result) {
		return ok(Json.toJson(result));
	}
	
	public static class Wire {
		public static List<String> findLocationTypeNamesBySuperTypeId(Long superTypeId){
			List<LocationType> types = LocationTypeRule.findAllBySuperTypeId(superTypeId);
			List<String> names = toNames(types);
			return names;
		}

		private static List<String> toNames(List<LocationType> types) {
			List<String> names = new ArrayList<>();
			for (LocationType t : types){
				names.add(t.getName());
			}
			return names;
		}
	}
	
}
