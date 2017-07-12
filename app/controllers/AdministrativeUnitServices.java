package controllers;

import interactors.GeoJSONParser;
import models.geo.FeatureCollection;
import play.Logger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBody;

import com.fasterxml.jackson.databind.JsonNode;

public class AdministrativeUnitServices extends Controller {
	static Status okJson(Object resultObject) {
		return ok(Json.toJson(resultObject));
	}

	static void setResponseLocation(Long id) {
		String uri = makeUri(id);
		response().setHeader(LOCATION, uri);
	}

	static String makeUri(Long id) {
		Request request = Context.current().request();
		Logger.debug("" + request.headers());
		String url = request.getHeader(ORIGIN) + request.path();
		String result = url;
		if (id != null)
			result += "/" + id;
		return result;
	}

	@BodyParser.Of(BodyParser.Json.class)
	static FeatureCollection parseRequestAsFeatureCollection() throws Exception {
		Request request = Context.current().request();
		JsonNode requestJSON = null;

		Logger.debug("\n");
		if (request != null) {
			RequestBody requestBody = request.body();

			// String requestBodyText = requestBody.toString();
			// Logger.debug("Request [" + request.getHeader("Content-Type") +
			// "], Length: " + requestBodyText.length() + "\n");
			// Logger.debug("Request Body:\n" + requestBodyText + "\n");
			// Logger.debug("Request.queryString():\n" + request.queryString() +
			// "\n");
			// Logger.debug("Request.headers().toString():\n" +
			// request.headers().toString() + "\n");

			requestJSON = requestBody.asJson();
			if (requestJSON == null) {
				throw new RuntimeException("Expecting JSON data");
			} else {
				String type = requestJSON.findPath("type").textValue();

				if (type == null) {
					throw new RuntimeException("Missing parameter [type]");
				}
			}

			return GeoJSONParser.parse(requestJSON);
		} else {
			String message = "Request is null";
			Logger.debug("\n" + message + "\n");
			throw new RuntimeException(message);
		}
	}
}
