package interactors;

import static interactors.Util.containsKey;
import static interactors.Util.containsOrIsEmpty;
import static interactors.Util.getNowDate;
import static interactors.Util.toDate;
import static interactors.Util.toListOfString;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import models.FeatureKey;
import models.Request;
import models.exceptions.BadRequest;

public class RequestRule {

	public static Request toFindByTermRequest(JsonNode node) {
		Request req = new Request();
		Boolean value;
		if (containsKey(node, "queryTerm"))
			req.setQueryTerm(node.get("queryTerm").asText());
		else
			throw new BadRequest("\"" + "queryTerm" + "\" key is requierd!");
		setStartDate(node, req, "startDate");
		setEndDate(node, req, "endDate");
		if (containsKey(node, "locationTypeIds"))
			req.setLocationTypeIds(Util.toListOfLong((JsonNode) node.get("locationTypeIds")));
		if (containsKey(node, "limit"))
			req.setLimit(node.get("limit").asInt());
		if (containsKey(node, "offset"))
			req.setOffset(node.get("offset").asInt());
		value = returnDefaultIfKeyNotExists(node, "ignoreAccent", true);
		req.setIgnoreAccent(value);
		value = returnDefaultIfKeyNotExists(node, "searchNames", true);
		req.setSearchNames(value);
		value = returnDefaultIfKeyNotExists(node, "searchOtherNames", true);
		req.setSearchOtherNames(value);
		value = returnDefaultIfKeyNotExists(node, "searchCodes", true);
		req.setSearchCodes(value);
		value = returnDefaultIfKeyNotExists(node, "verbose", true);
		req.setVerbose(value);
		JsonNode rootALC = node.get("rootALC");
		if (rootALC != null)
			req.setRootALC(rootALC.asLong());

		if (containsKey(node, "onlyFeatureFields")) {
			JsonNode jsonNode = node.get("onlyFeatureFields");
			List<String> list = toListOfString(jsonNode);
			req.setOnlyFeatureFields(list);
		}

		if (containsKey(node, "excludedFeatureFields")) {			
			JsonNode jsonNode = node.get("excludedFeatureFields");
			List<String> list = toListOfString(jsonNode);
			req.setExcludedFeatureFields(list);
		}

		value = returnDefaultIfKeyNotExists(node, "fuzzyMatch", false);
		req.setFuzzyMatch(value);
		if (containsKey(node, "fuzzyMatchThreshold"))
			req.setFuzzyMatchThreshold((float) node.get("fuzzyMatchThreshold").asDouble());
		return req;
	}

	public static Request toRequest(String onlyFeatureFields, String excludedFeatureFields, Long typeId, Integer limit,
			Integer offset) {
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
		if (fields == null || fields.isEmpty())
			return null;
		String[] strArr = fields.replaceAll("[\"\'\\s]", "").split("[\\[,\\]]");
		return Arrays.asList(strArr);
	}

	private static Boolean returnDefaultIfKeyNotExists(JsonNode node, String key, Boolean defaultValue) {
		if (containsKey(node, key))
			return node.get(key).asBoolean();
		else
			return defaultValue;
	}

	private static void setEndDate(JsonNode node, Request req, String endDate) {
		if (containsKey(node, endDate))
			req.setEndDate(toDate(node.get(endDate).asText()));
		if (req.getEndDate() == null)
			req.setEndDate(getNowDate());
	}

	private static void setStartDate(JsonNode node, Request req, String startDate) {
		if (containsKey(node, startDate))
			req.setStartDate(toDate(node.get(startDate).asText()));
		if (req.getStartDate() == null)
			req.setStartDate(toDate("0001-01-01"));
	}
}
