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

public class TestFindLocation {

	private long timeout = 100_000;
	private String basePath = "api/locations";
	private String findBulkPath = basePath + "/find-bulk";
	private String findByTermPath = basePath + "/find-by-term";
	private long gidTest1;
	private long gidTest2;
	private String jsonContentType = "application/json; charset=utf-8";
	private final String findByTermTestFile1 = "test/test-find-by-term1.json";
	private final String findByTermTestFile2 = "test/test-find-by-term2.json";
	private final String findBulkTestFile1 = "test/test-find-bulk1.json";

	public static Runnable test() {
		return () -> newInstance().testFindLocation();
	}

	private static TestFindLocation newInstance() {
		return new TestFindLocation();
	}

	public void testFindLocation() {

		gidTest2 = createLocationFromFile("test/testLocation2.geojson");
		gidTest1 = createLocationFromFile("test/testLocation1.geojson");

		try {
			findByQueryTermTest();
			findByIdTest();
			findByNameTest();
			findBulkTest();
			unsafeFindBulkTest();
			unsafeFindByNameTest();
			findByTypeId();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			deleteLocation(gidTest2);
			deleteLocation(gidTest1);
		}

	}

	private void findByTypeId() {
		String url = Server.makeTestUrl(basePath + "/find-by-type/1");
		WSResponse response = get(url);
		JsonNode jsonResp = response.asJson();
		assertStatus(response, OK);
		assertAreEqual(jsonResp.size(), 4);
		Object[] fieldNames = toArray(jsonResp.fieldNames());
		assertContainsAll(fieldNames, new String[] { "type", "features", "properties", "bbox" });
		fieldNames = toArray(jsonResp.get("properties").fieldNames());
		assertContainsAll(fieldNames,
				new String[] { "locationTypeIds", "locationTypeNames", "resultSize" });
		assertThat(jsonResp.get("features")).isNotNull();
		fieldNames = toArray(jsonResp.get("features").get(0).get("properties").fieldNames());
		assertContainsAll(fieldNames, new String[] { "name", "startDate", "otherNames", "codes",
				"locationTypeName", "lineage", "gid" });
		assertAreEqual(jsonResp.get("properties").get("resultSize").asInt(), jsonResp.get("features").size());
	}

	private void findByQueryTermTest() {
		String body = KmlRule.getStringFromFile(findByTermTestFile1);
		String url = Server.makeTestUrl(findByTermPath);
		WSResponse response = post(url, body, jsonContentType);
		assertStatus(response, OK);
		JsonNode jsonResp = response.asJson();
		assertAreEqual(jsonResp.size(), 4);
		Object[] fieldNames = toArray(jsonResp.fieldNames());
		assertContainsAll(fieldNames, new String[] { "type", "features", "properties", "bbox" });
		fieldNames = toArray(jsonResp.get("properties").fieldNames());
		assertContainsAll(fieldNames,
				new String[] { "queryTerm", "startDate", "endDate", "locationTypeIds", "locationTypeNames",
						"ignoreAccent", "searchNames", "searchOtherNames", "searchCodes", "limit", "offset",
						"resultSize" });
		assertAreEqual(jsonResp.get("features").size(), 2);
		fieldNames = toArray(jsonResp.get("features").get(0).fieldNames());
		assertContainsAll(fieldNames, new String[] { "type", "properties", "repPoint", "bbox" });
		
		fieldNames = toArray(jsonResp.get("features").get(0).get("properties").fieldNames());
		assertContainsAll(fieldNames, new String[] { "name", "startDate", "endDate", "otherNames", "codes",
				"locationDescription", "locationTypeName", "lineage", "rank", "headline", "gid" });
		assertAreEqual(jsonResp.get("properties").get("resultSize").asInt(), jsonResp.get("features").size());

		testFeatureOrder(jsonResp);
		testHeadLineMatch(jsonResp);

		body = "{\"queryTerm\":\"name with accent\", \"verbose\":true}";
		response = post(url, body, jsonContentType);
		assertStatus(response, OK);
		jsonResp = response.asJson();
		assertAreEqual(jsonResp.get("features").size(), 2);

		body = "{}";
		response = post(url, body, jsonContentType);
		assertStatus(response, BAD_REQUEST);
		
		body = KmlRule.getStringFromFile(findByTermTestFile2);
		response = post(url, body, jsonContentType);
		assertStatus(response, OK);
		jsonResp = response.asJson();
		assertAreEqual(jsonResp.get("features").size(), 1);
		fieldNames = toArray(jsonResp.get("properties").fieldNames());
		assertContainsAll(fieldNames, new String[] { "rootALC" });
	}

	private void testHeadLineMatch(JsonNode jsonResp) {
		JsonNode PropFeature1 = jsonResp.get("features").get(0).get("properties");
		JsonNode PropFeature2 = jsonResp.get("features").get(1).get("properties");
		assertAreEqual(PropFeature1.get("headline").asText(), "<b>name</b> <b>with</b> <b>accent</b>");
		assertAreEqual(PropFeature2.get("headline").asText(), "<b>name</b> <b>with</b> <b>accent</b> 2");
	}

	private void testFeatureOrder(JsonNode jsonResp) {
		JsonNode PropFeature1 = jsonResp.get("features").get(0).get("properties");
		JsonNode PropFeature2 = jsonResp.get("features").get(1).get("properties");
		assertThat(PropFeature1.get("gid").asLong() > PropFeature2.get("gid").asLong()).isTrue();
		assertAreEqual(PropFeature1.get("name").asText(), "Test Location 1");
		assertAreEqual(PropFeature2.get("name").asText(), "Test Location 2");
	}

	private void findByNameTest() {
		boolean searchOtherNames = true;
		String url = Server.makeTestUrl(basePath + "?q=an%20otherName%20for%20test&limit=2&offset=0&searchOtherNames="
				+ searchOtherNames + "&verbose=true");
		WSResponse response = get(url);
		assertStatus(response, OK);
		JsonNode jsonResp = response.asJson();
		assertAreEqual(jsonResp.get("features").size(), 2);
		searchOtherNames = false;
		url = Server.makeTestUrl(basePath + "?q=an%20otherName%20for%20test&limit=2&offset=0&searchOtherNames="
				+ searchOtherNames + "&verbose=true");
		response = get(url);
		jsonResp = response.asJson();
		assertAreEqual(jsonResp.get("features").size(), 0);
	}

	private void findByIdTest() {
		String url = Server.makeTestUrl(basePath + "/" + gidTest1);
		WSResponse response = get(url);
		JsonNode jsonResp = response.asJson();
		Object[] features = toArray(jsonResp.get("features").get(0).fieldNames());
		assertContainsAll(features, new String[] { "type", "geometry", "properties", "id", "bbox", "repPoint" });
		Object[] properties = toArray(jsonResp.get("features").get(0).get("properties").fieldNames());
		assertContainsAll(properties, new String[] { "locationTypeName", "lineage", "codes", "otherNames", "gid",
				"otherNames", "related", "children", "name", "startDate", "endDate" });
		
		url = Server.makeTestUrl(basePath + "/" + gidTest1 + "?maxExteriorRings=0");
		response = get(url);
		jsonResp = response.asJson();
		features = toArray(jsonResp.get("features").get(0).fieldNames());
		assertContainsAll(features, new String[] { "type", "properties", "id", "bbox", "repPoint" });
		properties = toArray(jsonResp.get("features").get(0).get("properties").fieldNames());
		assertContainsAll(properties, new String[] { "locationTypeName", "lineage", "codes", "otherNames", "gid",
				"otherNames", "related", "children", "name", "startDate", "endDate" });
	}

	private void unsafeFindBulkTest() {
		String body = "[{\"queryTerm\":\" ; drop ;\"}]";
		String url = Server.makeTestUrl(findBulkPath);
		WSResponse response = post(url, body, jsonContentType);
		assertStatus(response, BAD_REQUEST);

	}

	private void unsafeFindByNameTest() {
		boolean searchAltNames = true;
		String url = Server.makeTestUrl(basePath + "?q=;drop%20a%20;" + searchAltNames);
		WSResponse response = get(url);
		assertStatus(response, BAD_REQUEST);
	}

	private void findBulkTest() {
		String body = KmlRule.getStringFromFile(findBulkTestFile1);
		String url = Server.makeTestUrl(findBulkPath);
		WSResponse response = post(url, body, jsonContentType);
		JsonNode jsonResp = response.asJson();
		assertStatus(response, OK);
		assertAreEqual(jsonResp.size(), 3);
		JsonNode firstFeature = jsonResp.get(0);
		Object[] fieldNames = toArray(firstFeature.fieldNames());
		assertContainsAll(fieldNames, new String[] { "features", "properties" });
		fieldNames = toArray(firstFeature.get("properties").fieldNames());
		assertContainsAll(fieldNames, new String[] { "queryTerm", "startDate", "endDate", "locationTypeIds" });

		assertAreEqual(jsonResp.size(), 3);
		fieldNames = toArray(firstFeature.get("features").get(0).get("properties").fieldNames());
		assertContainsAll(fieldNames, new String[] { "name", "startDate", "endDate", "otherNames", "codes",
				"locationDescription", "locationTypeName", "lineage", "rank", "headline", "gid" });
		assertAreEqual(firstFeature.get("properties").get("resultSize").asInt(), firstFeature.get("features").size());

		body = "[{\"queryTerm\":\"pennsylvania\",\"startDate\":\"2000-\", \"verbose\":true}]";
		response = post(url, body, jsonContentType);
		assertStatus(response, BAD_REQUEST);

		body = "[{}]";
		response = post(url, body, jsonContentType);
		assertStatus(response, BAD_REQUEST);
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

	private Object[] toArray(Iterator<String> iterator) {
		List<String> list = new ArrayList<>();
		while (iterator.hasNext()) {
			list.add(iterator.next());
		}
		return list.toArray();
	}

	private WSResponse get(String url) {
		WSRequest req = WS.url(url);
		WSResponse response = req.get().get(timeout);
		return response;
	}
}