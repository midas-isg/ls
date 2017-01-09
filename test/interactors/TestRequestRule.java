package interactors;

import static org.fest.assertions.Assertions.assertThat;
import static suites.Helper.assertAreEqual;
import static suites.Helper.assertContainsAll;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import models.FeatureKey;
import models.Request;

public class TestRequestRule {

	private String properties = FeatureKey.PROPERTIES.valueOf();
	private String geometry = FeatureKey.GEOMETRY.valueOf();
	private String gid = FeatureKey.GID.valueOf();
	private String name = FeatureKey.NAME.valueOf();
	private String children = FeatureKey.CHILDREN.valueOf();

	@Test
	public void toRequestTest() {
		String onlyFeatureFields = " properties.gid , properties.name ";
		String excludedFeatureFields = geometry;
		Long typeId = 1L;
		Integer limit = 10;
		Integer offset = 0;
		String[] includedFields = new String[] { properties + "." + gid, properties + "." + name };
		String[] excludedFields = new String[] { geometry };

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
		List<String> onlyFeatureFields = Arrays.asList(new String[] { properties });
		req.setOnlyFeatureFields(onlyFeatureFields);
		boolean isRequested = RequestRule.isRequestedFeatureField(req, properties);
		assertThat(isRequested).isTrue();
		isRequested = RequestRule.isRequestedFeatureField(req, geometry);
		assertThat(isRequested).isFalse();

		req = new Request();
		List<String> excludedFeatureFields = Arrays.asList(new String[] { geometry });
		req.setExcludedFeatureFields(excludedFeatureFields);
		isRequested = RequestRule.isRequestedFeatureField(req, geometry);
		assertThat(isRequested).isFalse();

		req = new Request();
		excludedFeatureFields = Arrays.asList(new String[] { properties + "." + children });
		req.setExcludedFeatureFields(excludedFeatureFields);
		isRequested = RequestRule.isRequestedFeatureField(req, properties + "." + children);
		assertThat(isRequested).isFalse();
		isRequested = RequestRule.isRequestedFeatureField(req, properties + "." + gid);
		assertThat(isRequested).isTrue();
	}

	@Test
	public void isRequestedFeaturePropertiesTest() {
		Request req = new Request();
		List<String> onlyFeatureFields = Arrays
				.asList(new String[] { properties + "." + name, properties + "." + gid });
		req.setOnlyFeatureFields(onlyFeatureFields);
		boolean isRequested = RequestRule.isRequestedFeatureProperties(req, properties + "." + gid);
		assertThat(isRequested).isTrue();
		isRequested = RequestRule.isRequestedFeatureProperties(req, properties + "." + children);
		assertThat(isRequested).isFalse();

		req = new Request();
		List<String> excludedFeatureFields = Arrays.asList(new String[] { properties + "." + children, geometry });
		req.setExcludedFeatureFields(excludedFeatureFields);
		isRequested = RequestRule.isRequestedFeatureProperties(req, properties + "." + children);
		assertThat(isRequested).isFalse();
		isRequested = RequestRule.isRequestedFeatureProperties(req, properties + "." + name);
		assertThat(isRequested).isTrue();

		req = new Request();
		onlyFeatureFields = Arrays.asList(new String[] { properties });
		req.setOnlyFeatureFields(onlyFeatureFields);
		isRequested = RequestRule.isRequestedFeatureProperties(req, properties + "." + children);
		assertThat(isRequested).isTrue();

		req = new Request();
		excludedFeatureFields = Arrays.asList(new String[] { properties });
		req.setExcludedFeatureFields(excludedFeatureFields);
		isRequested = RequestRule.isRequestedFeatureProperties(req, properties + "." + children);
		assertThat(isRequested).isFalse();

	}

}
