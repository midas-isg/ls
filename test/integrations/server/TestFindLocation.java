package integrations.server;

import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.OK;
import static suites.Helper.assertAreEqual;
import static suites.Helper.assertContainsAll;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import play.Logger;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import com.fasterxml.jackson.databind.JsonNode;

public class TestFindLocation {

	private long timeout = 100_000;
	private String findByNamePath = "api/locations-by-name";
	private String findBulkPath = "api/locations/find-bulk";
	private String findPath = "api/locations-by-term";
	private String gid = "1";
	private String jsonContentType = "application/json; charset=utf-8";
	private final String exampleFilePath = "public\\examples\\api\\find.json";
	private String basePath = "api/locations";

	public static Runnable test() {
		return () -> newInstance().testFindLocation();
	}

	private static TestFindLocation newInstance() {
		return new TestFindLocation();
	}

	public void testFindLocation() {
		findLocationsTest();
		findByIdTest();
		findByNameTest();
		findBulkTest();
		unsafeFindBulkTest();
		unsafeFindByNameTest();
	}

	private void findLocationsTest() {
		String body = readFile(exampleFilePath);
		Logger.debug(body);
		String url = Server.makeTestUrl(findPath);
		WSResponse response = post(url, body, jsonContentType);
		JsonNode jsonResp = response.asJson();
		List<String> keys = getKeyList(jsonResp);
		Object[] propKeys = getKeyList(jsonResp.get("properties"))
				.toArray();

		assertStatus(response, OK);
		assertAreEqual(jsonResp.size(), 3);
		assertContainsAll(keys.toArray(), new String[] { "type", "features",
				"properties" });
		assertContainsAll(propKeys, new String[] { "queryTerm", "start",
				"end", "locationTypeIds", "ignoreAccent", "searchNames",
				"searchOtherNames", "searchCodes", "limit", "offset", "resultSize" });
		body = "[{}]";
		response = post(url, body, jsonContentType);
		assertStatus(response, BAD_REQUEST);
	}

	private void findByNameTest() {
		boolean searchOtherNames = true;
		String url = Server.makeTestUrl(findByNamePath
				+ "?queryTerm=pennsylvania&limit=2&offset=0&searchOtherNames="
				+ searchOtherNames);
		WSResponse response = get(url);
		JsonNode jsonResp = response.asJson();
		assertAreEqual(jsonResp.get("properties").get("searchOtherNames")
				.asText(), "true");
		searchOtherNames = false;
		url = Server.makeTestUrl(findByNamePath
				+ "?queryTerm=pennsylvania&limit=2&offset=0&searchOtherNames="
				+ searchOtherNames);
		response = get(url);
		jsonResp = response.asJson();
		assertAreEqual(jsonResp.get("properties").get("searchOtherNames")
				.asText(), "false");
	}

	private void findByIdTest() {
		String url = Server.makeTestUrl(basePath  + "/" + gid);
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
		String body = "[{\"queryTerm\":\" ; drop ;\"}]";
		String url = Server.makeTestUrl(findBulkPath);
		WSResponse response = post(url, body, jsonContentType);
		assertStatus(response, BAD_REQUEST);

	}

	private void unsafeFindByNameTest() {
		boolean searchAltNames = true;
		String url = Server.makeTestUrl(findByNamePath + "?queryTerm=;drop%20a%20;"
				+ searchAltNames);
		WSResponse response = get(url);
		assertStatus(response, BAD_REQUEST);
	}

	private void findBulkTest() {
		String body = "[{\"queryTerm\":\"pennsylvania\",\"locationTypeIds\":[16,104],\"start\":\"1780-12-12\","
				+ " \"end\":\"2012-11-11\"},{\"queryTerm\":\"sudan\",\"locationTypeIds\":[1,17],"
				+ "\"start\": null, \"end\":\"\"},{\"queryTerm\":\"sudan\"}]";
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
		assertContainsAll(propKeys, new String[] { "queryTerm", "start", "end",
				"locationTypeIds" });
		body = "[{\"queryTerm\":\"pennsylvania\",\"start\":\"2000-\"}]";
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

	private String readFile(String path) {
		File file = new File(path);
		String content = "";
		BufferedReader bf = null;
		try {
			bf = new BufferedReader(new FileReader(file));

			String line;
			line = bf.readLine();
			while (line != null) {
				content += line;
				line = bf.readLine();
			}
			bf.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return content;
	}
}
