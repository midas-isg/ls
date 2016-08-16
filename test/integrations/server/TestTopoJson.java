package integrations.server;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.HeaderNames.LOCATION;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.route;
import static suites.Helper.assertAreEqual;
import static suites.Helper.assertContainsAll;
import interactors.KmlRule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import play.Logger;
import play.api.mvc.Call;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.routes;

public class TestTopoJson {

	private long timeout = 100_000;
	private String topoJsonRoute = "api/topojson";
	private long gid;
	private String jsonContentType = "application/json; charset=utf-8";
	
	public static Runnable test() {
		return () -> newInstance().testTopoJson();
	}

	private static TestTopoJson newInstance() {
		return new TestTopoJson();
	}

	public void testTopoJson() {

		gid = createLocationFromFile("test/testLocation1.geojson");

		try {
			createTopoJsonTest();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			deleteLocation(gid);
		}

	}

	private void createTopoJsonTest() {
		String body = "{\"gids\":[" + gid + ", " + gid + "]}";
		String url = Server.makeTestUrl(topoJsonRoute);
		WSResponse response = post(url, body, jsonContentType);
		assertStatus(response, OK);
		
		body = "{\"gids\":[]}";
		response = post(url, body, jsonContentType);
		assertStatus(response, OK);
	}

	private WSResponse post(String url, String body, String contentType) {
		WSRequest req = WS.url(url).setContentType(contentType);
		WSResponse response = req.post(body).get(timeout);
		return response;
	}

	private void assertStatus(WSResponse wsResponse, int expected) {
		assertAreEqual(wsResponse.getStatus(), expected);
	}

	private long createLocationFromFile(String fileName) {

		String json = KmlRule.getStringFromFile(fileName);
		JsonNode body = Json.parse(json);
		Call call = routes.LocationServices.create();
		final RequestBuilder requestBuilder = Helpers.fakeRequest(call);
		if (body != null)
			requestBuilder.bodyJson(body);
		Result result = route(requestBuilder);
		String location = result.header(LOCATION);
		return toGid(location);
	}

	private long toGid(String url) {
		String[] tokens = url.split("/");
		String gid = tokens[tokens.length - 1];
		return Long.parseLong(gid);
	}

	private Result deleteLocation(long gid) {
		Call call = routes.LocationServices.delete(gid);
		final RequestBuilder requestBuilder = Helpers.fakeRequest(call);
		return route(requestBuilder);
	}
}