package suites;

import static com.fasterxml.jackson.databind.node.JsonNodeType.ARRAY;
import static com.fasterxml.jackson.databind.node.JsonNodeType.NULL;
import static com.fasterxml.jackson.databind.node.JsonNodeType.OBJECT;
import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;

import java.util.List;

import javax.persistence.EntityManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import _imperfactcoverage.Detour;
import gateways.configuration.ConfReader;
import interactors.ConfRule;
import interactors.TopoJsonRule;
import play.db.jpa.JPA;
import play.libs.F.Function0;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

public class Helper {
	public static WSResponse get(String url) {
		final long timeout = 1000000;
		final WSResponse response = WS.url(url).get().get(timeout);
		return response;
	}

	public static String readContext() {
		final ConfRule conf = new interactors.ConfRule(new ConfReader());
		return conf.readString("play.http.context");
	}

	public static <T> T wrapTransaction(Function0<T> block) {
		return wrapNoThrowingCheckedExecption(() -> JPA.withTransaction(block));
	}

	public static <T> T wrapNoThrowingCheckedExecption(Function0<T> block) {
		return Detour.wrapNoThrowingCheckedExecption(block);
	}

	public static void assertNodeType(JsonNode node, JsonNodeType... expected) {
		assertThat(node).isNotNull();
		assertThat(node.getNodeType()).isIn((Object[]) expected);
	}

	public static void assertAreEqual(Object actual, Object expected) {
		assertThat(actual).isEqualTo(expected);
	}
	
	public static void assertContainsAll(Object[] actual, Object[] expected) {
		for(Object obj: expected){
			assertThat(actual).contains(obj);
		}
	}
	
	public static void assertContainsOnly(Object[] actual, Object[] expected) {
			assertThat(actual).containsOnly(expected);
	}
	
	public static void assertExcludes(Object[] actual, Object[] expected) {
		assertThat(actual).excludes(expected);
	}

	public static JsonNode testJsonResponseMin(String url, int min) {
		return testJsonResponseClosedInterval(url, min, null);
	}

	public static JsonNode testJsonResponseLimit(String url, int limit) {
		return testJsonResponseClosedInterval(url, limit, limit);
	}

	private static JsonNode testJsonResponseClosedInterval(String url, int min,
			Integer max) {
		final WSResponse response = Helper.get(url);
		final JsonNode root = response.asJson();
		assertNodeType(root, OBJECT);
		final JsonNode results = root.get("results");
		assertNodeType(results, ARRAY);
		final int size = results.size();
		assertThat(size).isGreaterThanOrEqualTo(min);
		if (max != null)
			assertThat(size).isLessThanOrEqualTo(max);
		return root;
	}

	public static JsonNode testJsonObjectResponse(String url) {
		final JsonNode root = testJsonResponse(url);
		final JsonNode result = root.get("result");
		assertNodeType(result, OBJECT);
		return root;
	}

	private static JsonNode testJsonResponse(String url) {
		final WSResponse response = Helper.get(url);
		assertAreEqual(response.getStatus(), OK);
		final JsonNode root = response.asJson();
		assertNodeType(root, OBJECT);
		return root;
	}

	public static JsonNode testJsonArrayResponse(String url) {
		final JsonNode root = testJsonResponse(url);
		final JsonNode results = root.get("results");
		assertNodeType(results, ARRAY);
		return root;
	}

	public static void assertValueRange(JsonNode node, double max, double min) {
		final double val = node.asDouble();
		assertThat(val).isLessThanOrEqualTo(max);
		assertThat(val).isGreaterThanOrEqualTo(min);
	}

	public static <T> void assertArrayNode(JsonNode actuals, List<T> expected,
			Class<T> clazz) {
		assertNodeType(actuals, ARRAY);
		for (int i = 0; i < expected.size(); i++) {
			final Object actual = Json.fromJson(actuals.get(i), clazz);
			assertAreEqual(actual, expected.get(i));
		}
	}

	public static void assertTextNode(JsonNode actual, String expected) {
		if (expected == null)
			assertNodeType(actual, NULL);
		else
			assertAreEqual(actual.asText(), expected);
	}

	public static <T> void detachThenAssertWithDatabase(long id, T expected) {
		final EntityManager em = JPA.em();
		em.detach(expected);

		@SuppressWarnings("unchecked")
		final Class<T> clazz = (Class<T>) expected.getClass();
		final T found = em.find(clazz, id);
		assertAreEqual(found, expected);
	}
	
	public static boolean isTopoJsonInstalled(){
		String [] cmdArr = TopoJsonRule.toCommandArray("topojson --help");
		String result = TopoJsonRule.execute(cmdArr, null);
		return(result.contains("Usage:"));
	}
}
