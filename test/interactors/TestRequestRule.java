package interactors;

import static models.FeatureKey.asFullPath;
import static org.fest.assertions.Assertions.assertThat;
import static suites.Helper.assertAreEqual;
import static suites.Helper.assertContainsAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import models.FeatureKey;
import models.Request;
import play.libs.Json;

public class TestRequestRule {

	private final static String PROPERTIES = FeatureKey.PROPERTIES;
	private final static String GEOMETRY = FeatureKey.GEOMETRY;
	private final static String GID = FeatureKey.GID;
	private final static String NAME = FeatureKey.NAME;
	private final static String CHILDREN = FeatureKey.CHILDREN;
	private final String findByTermRequestExample = "test/resources/test/find-by-term-v2-example.json";

	@Test
	public void toRequestTest() {
		String onlyFeatureFields = " properties.gid , properties.name ";
		String excludedFeatureFields = GEOMETRY;
		Long typeId = 1L;
		Integer limit = 10;
		Integer offset = 0;
		String[] includedFields = new String[] { asFullPath(GID), asFullPath(NAME) };
		String[] excludedFields = new String[] { GEOMETRY };

		Request req = RequestRule.toRequestForCustomizedFeatureFields(onlyFeatureFields, excludedFeatureFields);

		assertContainsAll(req.getOnlyFeatureFields().toArray(), includedFields);
		assertContainsAll(req.getExcludedFeatureFields().toArray(), excludedFields);
		
		req = RequestRule.toFindByTypeRequest(onlyFeatureFields, excludedFeatureFields, typeId, limit, offset);
		assertAreEqual(req.getLimit(), limit);
		assertAreEqual(req.getOffset(), offset);
		assertThat(req.getLocationTypeIds()).contains(typeId);		
	}
	
	@Test
	public void toFindByTermRequestTest() throws IOException{
		JsonNode jsonNode = asJsonNode(findByTermRequestExample);
		Request req = RequestRule.toFindByTermRequest(jsonNode);
		assertThat(req.getCodeTypeIds()).contains(1L);
		assertAreEqual(req.getLimit(), 10);
		assertAreEqual(req.getOffset(), 0);
		assertThat(req.getLocationTypeIds()).contains(1L);
	}

	private JsonNode asJsonNode(String filePath) throws IOException {
		String string = asString(filePath);
		JsonNode jsonNode = Json.parse(string);
		return jsonNode;
	}

	private String asString(String filePath) throws IOException {
		String string = String.join("", Files.readAllLines(Paths.get(filePath)));
		return string;
	}

	@Test
	public void isRequestedFeatureFieldTest() {
		Request req = new Request();
		List<String> onlyFeatureFields = Arrays.asList(new String[] { PROPERTIES });
		req.setOnlyFeatureFields(onlyFeatureFields);
		boolean isRequested = RequestRule.isRequestedFeatureField(req, PROPERTIES);
		assertThat(isRequested).isTrue();
		isRequested = RequestRule.isRequestedFeatureField(req, GEOMETRY);
		assertThat(isRequested).isFalse();

		req = new Request();
		List<String> excludedFeatureFields = Arrays.asList(new String[] { GEOMETRY });
		req.setExcludedFeatureFields(excludedFeatureFields);
		isRequested = RequestRule.isRequestedFeatureField(req, GEOMETRY);
		assertThat(isRequested).isFalse();

		req = new Request();
		excludedFeatureFields = Arrays.asList(new String[] { asFullPath(CHILDREN) });
		req.setExcludedFeatureFields(excludedFeatureFields);
		isRequested = RequestRule.isRequestedFeatureField(req, asFullPath(CHILDREN));
		assertThat(isRequested).isFalse();
		isRequested = RequestRule.isRequestedFeatureField(req, asFullPath(GID));
		assertThat(isRequested).isTrue();
	}

	@Test
	public void isRequestedFeaturePropertiesTest() {
		Request req = new Request();
		List<String> onlyFeatureFields = Arrays
				.asList(new String[] { asFullPath(NAME), asFullPath(GID) });
		req.setOnlyFeatureFields(onlyFeatureFields);
		boolean isRequested = RequestRule.isRequestedFeatureProperties(req, asFullPath(GID));
		assertThat(isRequested).isTrue();
		isRequested = RequestRule.isRequestedFeatureProperties(req, asFullPath(CHILDREN));
		assertThat(isRequested).isFalse();

		req = new Request();
		List<String> excludedFeatureFields = Arrays.asList(new String[] { asFullPath(CHILDREN), GEOMETRY });
		req.setExcludedFeatureFields(excludedFeatureFields);
		isRequested = RequestRule.isRequestedFeatureProperties(req, asFullPath(CHILDREN));
		assertThat(isRequested).isFalse();
		isRequested = RequestRule.isRequestedFeatureProperties(req, asFullPath(NAME));
		assertThat(isRequested).isTrue();

		req = new Request();
		onlyFeatureFields = Arrays.asList(new String[] { PROPERTIES });
		req.setOnlyFeatureFields(onlyFeatureFields);
		isRequested = RequestRule.isRequestedFeatureProperties(req, asFullPath(CHILDREN));
		assertThat(isRequested).isTrue();

		req = new Request();
		excludedFeatureFields = Arrays.asList(new String[] { PROPERTIES });
		req.setExcludedFeatureFields(excludedFeatureFields);
		isRequested = RequestRule.isRequestedFeatureProperties(req, asFullPath(CHILDREN));
		assertThat(isRequested).isFalse();

	}

}
