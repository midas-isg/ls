package integrations.server;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.HeaderNames.LOCATION;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.route;
import static suites.Helper.assertAreEqual;
import static suites.Helper.assertContainsAll;
import static suites.Helper.assertContainsOnly;
import static suites.Helper.assertExcludes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.routes;
import interactors.KmlRule;
import play.api.mvc.Call;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;

public class TestFindLocation {

	private long timeout = 100_000;
	private String basePath = "api/locations";
	private String findBulkPath = basePath + "/find-bulk";
	private String findByTermPath = basePath + "/find-by-term";
	private long gidTest1;
	private long gidTest2;
	private String jsonContentType = "application/json; charset=utf-8";
	private final String findByTermRequestFile1 = "test/test-find-by-term-request-1.json";
	private final String findByTermRequestFile2 = "test/test-find-by-term-request-2.json";
	private final String findByTermRequestFile3 = "test/test-find-by-term-request-3.json";
	private final String fuzzyMatchRequest1 = "test/fuzzy-match-request-1.json";
	private final String findBulkRequestFile1 = "test/test-find-bulk-request-1.json";
	private final String CURRENT_VERSION = "2";

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
			findByQueryTermTest2();
			findByQueryTermTest();
			findByIdTest();
			findByNameTest();
			findBulkTest();
			unsafeFindBulkTest();
			unsafeFindByNameTest();
			findByTypeId();
			fuzzyMatchTest();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			deleteLocation(gidTest2);
			deleteLocation(gidTest1);
		}

	}

	private void findByQueryTermTest2() {
		
		String body = "{\"queryTerm\":\"testUnit 123\"," 
						+ "\"limit\":10,"
						+ "\"logic\":\"OR\","
						+ "\"onlyFeatureFields\":"
							+ "[\"properties.name\", "
							+ "\"properties.matchedTerm\", "
							+ "\"properties.rank\", "
							+ "\"properties.headline\"]"
						+ "}";
		String url = Server.makeTestUrl(findByTermPath + "?_v=" + CURRENT_VERSION);
		WSResponse response = post(url, body, jsonContentType);
		assertStatus(response, OK);
		JsonNode jsonResp = response.asJson();
		assertAreEqual(jsonResp.size(), 3);
		assertAreEqual(jsonResp.get("features").size(), 6);
		
		JsonNode features = jsonResp.get("features");
		String name = features.get(0).get("properties").get("name").asText();
		double rank = features.get(0).get("properties").get("rank").asDouble();
        assertThat(name).isEqualTo("def abc testUnit 123");
        assertThat(rank).isEqualTo(2.0);
        
        name = features.get(features.size()-1).get("properties").get("name").asText();
		rank = features.get(features.size()-1).get("properties").get("rank").asDouble();
		assertThat(name).isEqualTo("testUnit abc testUnit testUnit");
        assertThat(rank).isEqualTo(1.0);
        
        body = "{\"queryTerm\":\"testUnit\"," 
				+ "\"limit\":10,"
				+ "\"logic\":\"OR\","
				+ "\"onlyFeatureFields\":"
					+ "[\"properties.name\", "
					+ "\"properties.matchedTerm\", "
					+ "\"properties.rank\", "
					+ "\"properties.headline\"]"
				+ "}";
        url = Server.makeTestUrl(findByTermPath + "?_v=" + CURRENT_VERSION);
        response = post(url, body, jsonContentType);
        assertStatus(response, OK);
        jsonResp = response.asJson();
        assertAreEqual(jsonResp.get("features").size(), 6);
        
        features = jsonResp.get("features");
		String matchedTerm = features.get(0).get("properties").get("matchedTerm").asText();
		rank = features.get(0).get("properties").get("rank").asDouble();
		assertThat(matchedTerm).isEqualTo("testUnit");
		assertThat(rank).isEqualTo(1.0);
        
        body = "{\"queryTerm\":\"testUnit 123\"," 
				+ "\"limit\":10,"
				+ "\"logic\":\"AND\","
				+ "\"onlyFeatureFields\":"
					+ "[\"properties.name\", "
					+ "\"properties.matchedTerm\", "
					+ "\"properties.rank\", "
					+ "\"properties.headline\"]"
				+ "}";
        url = Server.makeTestUrl(findByTermPath + "?_v=" + CURRENT_VERSION);
        response = post(url, body, jsonContentType);
        assertStatus(response, OK);
        jsonResp = response.asJson();
        assertAreEqual(jsonResp.get("features").size(), 4);
        
        features = jsonResp.get("features");
		name = features.get(0).get("properties").get("name").asText();
		rank = features.get(0).get("properties").get("rank").asDouble();
		assertThat(name).isEqualTo("def abc testUnit 123");
		assertThat(rank).isEqualTo(1.0);
		
		
		name = features.get(features.size()-1).get("properties").get("name").asText();
		rank = features.get(features.size()-1).get("properties").get("rank").asDouble();
		assertThat(name).isEqualTo("testUnit abc def ghi 123");
		assertThat(rank).isEqualTo(1.0);
	}

	private void findByQueryTermTest() {
		String body = KmlRule.getStringFromFile(findByTermRequestFile1);
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
		assertContainsOnly(fieldNames,
				new String[] { "name", "startDate", "endDate", "otherNames", "codes", "locationDescription",
						"locationTypeName", "lineage", "rank", "headline", "gid", "matchedTerm", "related" });
		assertAreEqual(jsonResp.get("properties").get("resultSize").asInt(), jsonResp.get("features").size());

		testFeatureOrder(jsonResp);
		testHeadLineMatch(jsonResp);

		body = "{\"queryTerm\":\"name with accent\", \"verbose\":true}";
		response = post(url, body, jsonContentType);
		assertStatus(response, OK);
		jsonResp = response.asJson();
		assertAreEqual(jsonResp.get("features").size(), 2);

		body = "{\"queryTerm\":\"name with accent\", \"verbose\":false}";
		response = post(url, body, jsonContentType);
		assertStatus(response, OK);
		jsonResp = response.asJson();
		fieldNames = toArray(jsonResp.fieldNames());
		assertContainsOnly(fieldNames, new String[] { "gids", "properties" });
		assertAreEqual(jsonResp.get("gids").size(), 2);

		body = "{}";
		response = post(url, body, jsonContentType);
		assertStatus(response, BAD_REQUEST);

		body = KmlRule.getStringFromFile(findByTermRequestFile2);
		response = post(url, body, jsonContentType);
		assertStatus(response, OK);
		jsonResp = response.asJson();
		assertAreEqual(jsonResp.get("features").size(), 1);
		fieldNames = toArray(jsonResp.get("properties").fieldNames());
		assertContainsAll(fieldNames, new String[] { "rootALC" });

		body = KmlRule.getStringFromFile(findByTermRequestFile3);
		response = post(url, body, jsonContentType);
		assertStatus(response, OK);
		jsonResp = response.asJson();
		assertAreEqual(jsonResp.get("features").size(), 2);
		fieldNames = toArray(jsonResp.fieldNames());
		assertContainsOnly(fieldNames, new String[] { "type", "properties", "bbox", "features" });
		fieldNames = toArray(jsonResp.get("features").get(0).fieldNames());
		assertContainsOnly(fieldNames, new String[] { "type", "properties", "bbox", "repPoint" });
		fieldNames = toArray(jsonResp.get("features").get(0).get("properties").fieldNames());
		assertContainsOnly(fieldNames, new String[] { "name", "gid", "locationTypeName", "lineage", "codes",
				"otherNames", "related", "rank", "headline", "startDate", "endDate", "matchedTerm" });

		body = "{\"queryTerm\":\"name with accent\", \"includeOnly\":[\"properties.name\", \"properties.gid\"]}";
		response = post(url, body, jsonContentType);
		assertStatus(response, OK);
		jsonResp = response.asJson();
		fieldNames = toArray(jsonResp.get("features").get(0).get("properties").fieldNames());
		assertContainsOnly(fieldNames, new String[] { "gid", "name" });
		
		body = "{\"queryTerm\":\"name with accent\", \"onlyFeatureFields\":[\"properties.name\", \"properties.gid\"]}";
		url = Server.makeTestUrl(findByTermPath + "?_v=" + CURRENT_VERSION);
		response = post(url, body, jsonContentType);
		assertStatus(response, OK);
		jsonResp = response.asJson();
		fieldNames = toArray(jsonResp.get("features").get(0).get("properties").fieldNames());
		assertContainsOnly(fieldNames, new String[] { "gid", "name" });
	}

	private void fuzzyMatchTest() {
		String body = KmlRule.getStringFromFile(fuzzyMatchRequest1);
		String url = Server.makeTestUrl(findByTermPath);
		WSResponse response = post(url, body, jsonContentType);
		assertStatus(response, OK);
		JsonNode jsonResp = response.asJson();
		assertAreEqual(jsonResp.size(), 4);
		assertAreEqual(jsonResp.get("features").size(), 1);
		assertAreEqual(jsonResp.get("features").get(0).get("properties").get("rank").asDouble(), 0.33333334);
		assertAreEqual(jsonResp.get("features").get(0).get("properties").get("matchedTerm").asText(),
				"ñámé wíth áccéñt");
		assertAreEqual(jsonResp.get("properties").get("fuzzyMatchThreshold").asDouble(), 0.32);
	}

	private void findByTypeId() {
		String url = Server.makeTestUrl(basePath + "/find-by-type/999999");
		WSResponse response = get(url);
		JsonNode jsonResp = response.asJson();
		assertStatus(response, OK);
		assertAreEqual(jsonResp.size(), 3);
		Object[] fieldNames = toArray(jsonResp.fieldNames());
		assertContainsAll(fieldNames, new String[] { "type", "features", "properties" });
		fieldNames = toArray(jsonResp.get("properties").fieldNames());
		assertContainsAll(fieldNames, new String[] { "locationTypeIds", "resultSize" });
		assertAreEqual(jsonResp.get("properties").get("resultSize").asInt(), jsonResp.get("features").size());
		
		url = Server.makeTestUrl(basePath + "/find-by-type/1?_onlyFeatureFields=properties.name&limit=5&offset=1&_v=" + CURRENT_VERSION);
		response = get(url);
		jsonResp = response.asJson();
		assertStatus(response, OK);
		assertAreEqual(jsonResp.size(), 3);
		fieldNames = toArray(jsonResp.fieldNames());
		assertContainsAll(fieldNames, new String[] { "type", "features", "properties" });
		assertAreEqual(jsonResp.get("features").size(), 5);
		fieldNames = toArray(jsonResp.get("features").get(0).fieldNames());
		assertContainsOnly(fieldNames, new String[] { "type", "properties" });
		fieldNames = toArray(jsonResp.get("features").get(0).get("properties").fieldNames());
		assertContainsOnly(fieldNames, new String[] { "name" });
		
		url = Server.makeTestUrl(basePath + "/find-by-type/1?_excludedFeatureFields=properties.children,geometry&limit=2&offset=0&_v=" + CURRENT_VERSION);
		response = get(url);
		jsonResp = response.asJson();
		assertStatus(response, OK);
		fieldNames = toArray(jsonResp.get("features").get(0).fieldNames());
		assertExcludes(fieldNames, new String[] { "geometry" });
		fieldNames = toArray(jsonResp.get("features").get(0).get("properties").fieldNames());
		assertExcludes(fieldNames, new String[] { "children" });
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
		Object[] fieldNames = toArray(jsonResp.get("features").get(0).get("properties").fieldNames());
		assertContainsOnly(fieldNames,
				new String[] { "name", "startDate", "endDate", "otherNames", "codes", "locationDescription",
						"locationTypeName", "lineage", "rank", "headline", "gid", "matchedTerm", "related",
						"children" });

		boolean verbose = false;
		url = Server.makeTestUrl(basePath + "?q=an%20otherName%20for%20test&limit=2&offset=0&searchOtherNames="
				+ searchOtherNames + "&verbose=" + verbose);
		response = get(url);
		assertStatus(response, OK);
		jsonResp = response.asJson();
		fieldNames = toArray(jsonResp.fieldNames());
		assertContainsAll(fieldNames, new String[] { "gids", "properties" });
		assertAreEqual(jsonResp.get("gids").size(), 2);

		searchOtherNames = true;
		url = Server.makeTestUrl(basePath + "?q=an%20otherName%20for%20test&limit=2&offset=0&searchOtherNames="
				+ searchOtherNames + "&_onlyFeatureFields=properties.gid,properties.name&_v=" + CURRENT_VERSION);
		response = get(url);
		jsonResp = response.asJson();
		assertAreEqual(jsonResp.get("features").size(), 2);
		fieldNames = toArray(jsonResp.get("features").get(0).get("properties").fieldNames());
		assertContainsOnly(fieldNames, new String[] { "name", "gid" });
		
		searchOtherNames = true;
		url = Server.makeTestUrl(basePath + "?q=an%20otherName%20for%20test&limit=2&offset=0&searchOtherNames="
				+ searchOtherNames + "&_excludedFeatureFields=geometry,properties.children&_v=" + CURRENT_VERSION);
		response = get(url);
		jsonResp = response.asJson();
		assertAreEqual(jsonResp.get("features").size(), 2);
		fieldNames = toArray(jsonResp.get("features").get(0).fieldNames());
		assertContainsOnly(fieldNames, new String[] { "properties", "type", "bbox", "repPoint" });
		fieldNames = toArray(jsonResp.get("features").get(0).get("properties").fieldNames());
		assertContainsOnly(fieldNames, new String[] { "name", "startDate", "endDate", "otherNames", "codes", "locationDescription",
				"locationTypeName", "lineage", "rank", "headline", "gid", "matchedTerm", "related" });
	}

	private void findByIdTest() {
		String url = Server.makeTestUrl(basePath + "/" + gidTest1);
		WSResponse response = get(url);
		JsonNode jsonResp = response.asJson();
		Object[] jsonRespNodes = toArray(jsonResp.fieldNames());
		assertContainsAll(jsonRespNodes, new String[] { "type", "features", "bbox" });
		Object[] features = toArray(jsonResp.get("features").get(0).fieldNames());
		assertContainsAll(features, new String[] { "type", "geometry", "properties", "id", "bbox", "repPoint" });
		Object[] properties = toArray(jsonResp.get("features").get(0).get("properties").fieldNames());
		assertContainsAll(properties, new String[] { "locationTypeName", "lineage", "codes", "otherNames", "gid",
				"otherNames", "related", "children", "name", "startDate", "endDate" });

		url = Server.makeTestUrl(basePath + "/" + gidTest1 + "?maxExteriorRings=0");
		response = get(url);
		jsonResp = response.asJson();
		jsonRespNodes = toArray(jsonResp.fieldNames());
		assertContainsAll(jsonRespNodes, new String[] { "type", "features", "bbox" });
		features = toArray(jsonResp.get("features").get(0).fieldNames());
		assertContainsAll(features, new String[] { "type", "properties", "id", "bbox", "repPoint" });
		properties = toArray(jsonResp.get("features").get(0).get("properties").fieldNames());
		assertContainsAll(properties, new String[] { "locationTypeName", "lineage", "codes", "otherNames", "gid",
				"otherNames", "related", "children", "name", "startDate", "endDate" });
		
		url = Server.makeTestUrl(basePath + "/" + gidTest1 + "?_onlyFeatureFields=geometry");
		response = get(url);
		jsonResp = response.asJson();
		jsonRespNodes = toArray(jsonResp.fieldNames());
		assertContainsAll(jsonRespNodes, new String[] { "type", "features" });
		features = toArray(jsonResp.get("features").get(0).fieldNames());
		assertContainsOnly(features, new String[] { "type", "geometry", "id"});
		
		url = Server.makeTestUrl(basePath + "/" + gidTest1 + "?_excludedFeatureFields=geometry,properties.children");
		response = get(url);
		jsonResp = response.asJson();
		jsonRespNodes = toArray(jsonResp.fieldNames());
		assertContainsAll(jsonRespNodes, new String[] { "type", "features", "bbox" });
		features = toArray(jsonResp.get("features").get(0).fieldNames());
		assertContainsOnly(features, new String[] { "type", "properties", "bbox", "repPoint" });
		properties = toArray(jsonResp.get("features").get(0).get("properties").fieldNames());
		assertThat("children").isNotIn(properties);		
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
		String body = KmlRule.getStringFromFile(findBulkRequestFile1);
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

		body = "[{\"queryTerm\":\"Test Location 1\",\"includeOnly\":[\"properties.name\",\"properties.gid\"]},"
				+ "{\"queryTerm\":\"Test Location 2\",\"includeOnly\":[\"properties.name\"]}" + "]";
		response = post(url, body, jsonContentType);
		assertStatus(response, OK);
		jsonResp = response.asJson();
		firstFeature = jsonResp.get(0);
		fieldNames = toArray(firstFeature.get("features").get(0).get("properties").fieldNames());
		assertContainsOnly(fieldNames, new String[] { "name", "gid" });
		JsonNode secondFeature = jsonResp.get(1);
		fieldNames = toArray(secondFeature.get("features").get(0).get("properties").fieldNames());
		assertContainsOnly(fieldNames, new String[] { "name" });
		
		body = "[{\"queryTerm\":\"Test Location 1\",\"onlyFeatureFields\":[\"properties.name\",\"properties.gid\"]},"
				+ "{\"queryTerm\":\"Test Location 2\",\"onlyFeatureFields\":[\"properties.name\"]}" + "]";
		url = Server.makeTestUrl(findBulkPath + "?_v=" + CURRENT_VERSION);
		response = post(url, body, jsonContentType);
		assertStatus(response, OK);
		jsonResp = response.asJson();
		firstFeature = jsonResp.get(0);
		fieldNames = toArray(firstFeature.get("features").get(0).get("properties").fieldNames());
		assertContainsOnly(fieldNames, new String[] { "name", "gid" });
		secondFeature = jsonResp.get(1);
		fieldNames = toArray(secondFeature.get("features").get(0).get("properties").fieldNames());
		assertContainsOnly(fieldNames, new String[] { "name" });
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