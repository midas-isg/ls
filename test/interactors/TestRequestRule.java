package interactors;

import static models.FeatureKey.asFullPath;
import static org.fest.assertions.Assertions.assertThat;
import static suites.Helper.assertAreEqual;
import static suites.Helper.assertContainsAll;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import models.FeatureKey;
import models.Request;

public class TestRequestRule {

	private final static String PROPERTIES = FeatureKey.PROPERTIES;
	private final static String GEOMETRY = FeatureKey.GEOMETRY;
	private final static String GID = FeatureKey.GID;
	private final static String NAME = FeatureKey.NAME;
	private final static String CHILDREN = FeatureKey.CHILDREN;

	@Test
	public void toRequestTest() {
		String onlyFeatureFields = " properties.gid , properties.name ";
		String excludedFeatureFields = GEOMETRY;
		Long typeId = 1L;
		Integer limit = 10;
		Integer offset = 0;
		String[] includedFields = new String[] { asFullPath(GID), asFullPath(NAME) };
		String[] excludedFields = new String[] { GEOMETRY };

		Request req = RequestRule.toRequest(onlyFeatureFields, excludedFeatureFields);

		assertContainsAll(req.getOnlyFeatureFields().toArray(), includedFields);
		assertContainsAll(req.getExcludedFeatureFields().toArray(), excludedFields);
		
		req = RequestRule.toFindByTypeRequest(onlyFeatureFields, excludedFeatureFields, typeId, limit, offset);
		assertAreEqual(req.getLimit(), limit);
		assertAreEqual(req.getOffset(), offset);
		assertThat(req.getLocationTypeIds()).contains(typeId);		
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
