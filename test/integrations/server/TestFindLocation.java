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
		findBulkTest();
		unsafeFindBulkTest();
	}

	private void findByIdTest() {
		String url = Server.makeTestUrl(basePath + "/" + gid );
		WSResponse response = get(url);
		JsonNode jsonResp = response.asJson();
		Object[] featuresKeys = getKeyList(jsonResp.get("features").get(0).get("properties")).toArray();
		assertContainsAll(featuresKeys, new String[]{"locationTypeName", "lineage", "codes",
				"gid", "otherNames", "related", "children", "name", "start", "parentGid"});
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

	private void findBulkTest() {
		String body = "[{\"name\":\"pennsylvania\",\"locationTypeIds\":[16,104],\"start\":\"1780-12-12\","
				+ " \"end\":\"2016-11-11\"},{\"name\":\"sudan\",\"locationTypeIds\":[1,17],"
				+ "\"start\":\"2010-11-11\", \"end\":\"2012-11-11\"}]";
		String url = Server.makeTestUrl(findBulkPath);
		WSResponse response = post(url, body, jsonContentType);
		JsonNode jsonResp = response.asJson();
		List<String> keys = getKeyList(jsonResp.get(0));
		Object[] propKeys = getKeyList(jsonResp.get(0).get("properties")).toArray();
		
		assertStatus(response, OK);
		assertAreEqual(jsonResp.size(), 2);
		assertContainsAll(keys.toArray(), new String[]{"features","properties"});
		assertContainsAll(propKeys, new String[]{"name","start","end","locationTypeIds"});
	}

	private List<String> getKeyList(JsonNode jsonResp) {
		List<String> keys = new ArrayList<>();
		Iterator<String> l = jsonResp.fieldNames();
		while(l.hasNext()){
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