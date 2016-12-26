package interactors;

import static interactors.Util.containsOrIsEmpty;

import java.util.Arrays;
import java.util.List;

import models.FeatureKey;
import models.Request;

public class RequestRule {

	public static Request toRequest(String onlyFeatureFields, String excludedFeatureFields, Long typeId,
			Integer limit, Integer offset) {
		Request request = new Request();
		List<String> list = parse(onlyFeatureFields);
		request.setOnlyFeatureFields(list);
		list = parse(excludedFeatureFields);
		request.setExcludedFeatureFields(list);
		request.setLimit(limit);
		request.setOffset(offset);
		request.setLocationTypeIds(Arrays.asList(new Long[] { typeId }));
		return request;
	}

	public static Request toRequest(String onlyFeatureFields, String excludedFeatureFields) {
		return toRequest(onlyFeatureFields, excludedFeatureFields, null, null, null);
	}

	public static boolean isRequestedFeatureField(Request req, String key) {
		return isIncluded(req, key) && !isExcluded(req, key);
	}

	public static boolean isRequestedFeatureProperties(Request req, String propertiesField) {
		if (req == null || propertiesField == null)
			return false;
		boolean isRequested = isRequestedFeatureField(req, propertiesField);
		boolean isPropertiesIncluded = Util.contains(req.getOnlyFeatureFields(), FeatureKey.PROPERTIES.valueOf());
		boolean isPropertiesExcluded = Util.contains(req.getExcludedFeatureFields(), FeatureKey.PROPERTIES.valueOf());
		return (isRequested || isPropertiesIncluded) && !isPropertiesExcluded;
	}

	public static boolean isPropertiesRequested(Request req) {
		if (req == null)
			return false;
		String properties = FeatureKey.PROPERTIES.valueOf();
		if (isExcluded(req, properties))
			return false;
		if (isIncluded(req, properties))
			return true;
		for (String f : req.getOnlyFeatureFields()) {
			if (startsWith(f, properties))
				return true;
		}
		return false;
	}

	private static boolean isExcluded(Request req, String key) {
		if (req == null || key == null)
			return false;
		return Util.contains(req.getExcludedFeatureFields(), key);
	}

	private static boolean isIncluded(Request req, String key) {
		if (req == null || key == null)
			return false;
		return containsOrIsEmpty(req.getOnlyFeatureFields(), key);
	}

	private static boolean startsWith(String s, String sub) {
		if (s == null || sub == null)
			return false;
		String[] split = s.split("\\.");
		if (split.length > 0)
			return split[0].equals(sub);
		return false;
	}

	private static List<String> parse(String fields) {
		if (fields == null)
			return null;
		String[] strArr = fields.replaceAll("[\"\'\\s]", "").split("[\\[,\\]]");
		return Arrays.asList(strArr);
	}
}
