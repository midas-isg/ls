package integrations.server;

import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.OK;
import static suites.Helper.assertAreEqual;
import static suites.Helper.assertContainsAll;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import com.fasterxml.jackson.databind.JsonNode;

public class TestFindLocation {

	private String findBulkPath = "api/find-bulk";
	private long timeout = 100_000;
	private String basePath = "api/locations";
	private String gid = "1";
	private String jsonContentType = "application/json; charset=utf-8";

	public static Runnable test() {
		return () -> newInstance().testFindLocation();
	}

	private static TestFindLocation newInstance() {
		return new TestFindLocation();
	}

	public void testFindLocation() {
		findByIdTest();
		findByNameTest();
		findBulkTest();
		unsafeFindBulkTest();
		unsafeFindByNameTest();
	}

	private void findByNameTest() {
		boolean searchAltNames = true;
		String url = Server.makeTestUrl(basePath + "?q=pennsylvania&limit=2&offset=0&searchAltNames="+ searchAltNames  );
		WSResponse response = get(url);
		JsonNode jsonResp = response.asJson();
		assertAreEqual(jsonResp.get("properties").get("searchAltNames").asText(), "true");
		searchAltNames = false;
		url = Server.makeTestUrl(basePath + "?q=pennsylvania&limit=2&offset=0&searchAltNames="+ searchAltNames  );
		response = get(url);
		jsonResp = response.asJson();
		assertAreEqual(jsonResp.get("properties").get("searchAltNames").asText(), "false");
	}

	private void findByIdTest() {
		String url = Server.makeTestUrl(basePath + "/" + gid);
		WSResponse response = get(url);
		JsonNode jsonResp = response.asJson();
		Object[] features = getKeyList(jsonResp.get("features").get(0))
				.toArray();
		assertContainsAll(features, new String[] { "type", "geometry",
				"properties", "id", "bbox", "repPoint" });
		Object[] properties = getKeyList(
				jsonResp.get("features").get(0).get("properties")).toArray();
		assertContainsAll(properties, new String[] { "locationTypeName",
				"lineage", "codes", "gid", "otherNames", "related", "children",
				"name", "startDate", "parentGid" });
	}

	private WSResponse get(String url) {
		WSRequest req = WS.url(url);
		WSResponse response = req.get().get(timeout);
		return response;
	}

	private void unsafeFindBulkTest() {
		String body = "[{\"name\":\" ; drop ;\"}]";
		String url = Server.makeTestUrl(findBulkPath);
		WSResponse response = post(url, body, jsonContentType);
		assertStatus(response, BAD_REQUEST);

	}
	
	private void unsafeFindByNameTest() {
		boolean searchAltNames = true;
		String url = Server.makeTestUrl(basePath + "?q=;drop%20a%20;"+ searchAltNames  );
		WSResponse response = get(url);
		assertStatus(response, BAD_REQUEST);
	}

	private void findBulkTest() {
		String body = "[{\"name\":\"pennsylvania\",\"locationTypeIds\":[16,104],\"start\":\"1780-12-12\","
				+ " \"end\":\"2012-11-11\"},{\"name\":\"sudan\",\"locationTypeIds\":[1,17],"
				+ "\"start\": null, \"end\":\"\"},{\"name\":\"sudan\"}]";
		String url = Server.makeTestUrl(findBulkPath);
		WSResponse response = post(url, body, jsonContentType);
		JsonNode jsonResp = response.asJson();
		List<String> keys = getKeyList(jsonResp.get(0));
		Object[] propKeys = getKeyList(jsonResp.get(0).get("properties"))
				.toArray();

		assertStatus(response, OK);
		assertAreEqual(jsonResp.size(), 3);
		assertContainsAll(keys.toArray(), new String[] { "features",
				"properties" });
		assertContainsAll(propKeys, new String[] { "name", "start", "end",
				"locationTypeIds" });
		body = "[{\"name\":\"pennsylvania\",\"start\":\"2000-\"}]";
		response = post(url, body, jsonContentType);
		assertStatus(response, BAD_REQUEST);
		
		body = "[{}]";
		response = post(url, body, jsonContentType);
		assertStatus(response, BAD_REQUEST);
	}

	private List<String> getKeyList(JsonNode jsonResp) {
		List<String> keys = new ArrayList<>();
		Iterator<String> l = jsonResp.fieldNames();
		while (l.hasNext()) {
			keys.add(l.next());
		}
		return keys;
	}

	private WSResponse post(String url, String body, String contentType) {
		WSRequest req = WS.url(url).setContentType(contentType);
		WSResponse response = req.post(body).get(timeout);
		return response;
	}

	private void assertStatus(WSResponse wsResponse, int expected) {
		assertAreEqual(wsResponse.getStatus(), expected);
	}
}
