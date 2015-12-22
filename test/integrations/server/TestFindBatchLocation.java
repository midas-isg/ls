package integrations.server;

import static suites.Helper.assertAreEqual;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.CONFLICT;
import static play.mvc.Http.Status.CREATED;

public class TestFindBatchLocation {

	private String basePath = "api/bulk-locations";
	private long timeout = 100_000;

	public static Runnable test() {
		return () -> newInstance().testFindBatchLocation();
	}

	private static TestFindBatchLocation newInstance() {
		return new TestFindBatchLocation();
	}

	public void testFindBatchLocation() {
		String body = "[{\"name\":\"pennsylvania\",\"locationTypeIds\":[16,10],"
				+ "\"start\":\"1780-12-12\", \"end\":\"2800-11-11\"},"
				+ "{\"name\":\"United States of America\",\"locationTypeIds\":[1,16],"
				+ "\"start\":\"2010-11-11\", \"end\":\"2012-11-11\"}]";
		String url = Server.makeTestUrl(basePath);
		WSResponse response = requestBatchLocation(url, body);
		assertStatus(response, CREATED);
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
