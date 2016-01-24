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

public class TestFindBulkLocation {

	private String basePath = "api/find-bulk";
	private long timeout = 100_000;

	public static Runnable test() {
		return () -> newInstance().testFindBatchLocation();
	}

	private static TestFindBulkLocation newInstance() {
		return new TestFindBulkLocation();
	}

	public void testFindBatchLocation() {
		endToEndTest();
		unsafeRequestTest();
	}

	private void unsafeRequestTest() {
		String body = "[{\"name\":\" ; drop ;\"}]";
		String url = Server.makeTestUrl(basePath);
		WSResponse response = requestBatchLocation(url, body);
		assertStatus(response, BAD_REQUEST);

	}

	private void endToEndTest() {
		String body = "[{\"name\":\"pennsylvania\",\"locationTypeIds\":[16,104],\"start\":\"1780-12-12\","
				+ " \"end\":\"2012-11-11\"},{\"name\":\"sudan\",\"locationTypeIds\":[1,17],"
				+ "\"start\": null, \"end\":\"\"},{\"name\":\"sudan\"}]";
		String url = Server.makeTestUrl(basePath);
		WSResponse response = requestBatchLocation(url, body);
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
		response = requestBatchLocation(url, body);
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

	private WSResponse requestBatchLocation(String url, String body) {
		WSRequest req = WS.url(url).setContentType(
				"application/json; charset=utf-8");
		WSResponse response = req.post(body).get(timeout);
		return response;
	}

	private void assertStatus(WSResponse wsResponse, int expected) {
		assertAreEqual(wsResponse.getStatus(), expected);
	}
}
