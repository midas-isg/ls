package controllers;

import java.util.List;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import dao.entities.CodeType;
import interactors.CodeTypeRule;
import models.geo.FeatureCollection;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

@Api(value = "/api/LocationCode", description = "Endpoint for location codes")
public class LocationCodeService extends Controller {
	
	@Transactional
	@ApiOperation(
			httpMethod = "GET", 
			nickname = "readLocationCodeTypes", 
			value = "Returns location code-types", 
			notes = "This endpoint returns all available location code-types.",
			response = CodeType.class
	)
	@ApiResponses(value = {
			@ApiResponse(code = OK, message = "Successful retrieval of location", 
					response = FeatureCollection.class),
			@ApiResponse(code = INTERNAL_SERVER_ERROR, message = "Internal server error"),
	})
	public Result findCodeTypes(){
		List<CodeType> result = CodeTypeRule.findCodeTypes();
		return ok(Json.toJson(result));
	}	
}
