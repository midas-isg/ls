package interactors;

import java.math.BigInteger;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.joda.time.LocalDate;

import com.fasterxml.jackson.databind.JsonNode;

import models.exceptions.BadRequest;

public class Util {

	static boolean containsKey(JsonNode node, String key) {
		Iterator<String> names = node.fieldNames();
		while (names.hasNext()) {
			if (names.next().equals(key))
				return true;
		}
		return false;
	}

	public static Date getDate(Map<String, Object> m, String key) {
		return (Date) m.get(key);
	}

	public static String getString(Map<String, Object> m, String key) {
		Object obj = m.get(key);
		if (obj == null) {
			return null;
		}

		return String.valueOf(obj);
	}

	public static Long getLong(Map<String, Object> m, String key) {
		Object object = m.get(key);
		if (object == null) {
			return null;
		}

		if (object instanceof BigInteger) {
			return ((BigInteger) object).longValue();
		}
		return Long.parseLong(object.toString());
	}

	static Date toDate(Map<String, Object> param, String key) {
		if (param.get(key) == null) {
			return null;
		}
		String dateString = (String) param.get(key);
		if (dateString.equals("") || dateString.equals("null"))
			return null;
		Date date = null;
		try {
			LocalDate localDt = new LocalDate(dateString);
			date = Date.valueOf(localDt.toString());
		} catch (Exception e) {
			String msg = (e.getMessage() != null) ? e.getMessage() : "Invalid date: " + dateString;
			throw new BadRequest(msg);
		}
		return date;
	}

	static Date toDate(String dateString) {
		if (dateString == null)
			return null;
		if (dateString.equals("") || dateString.equals("null"))
			return null;
		Date date = null;
		try {
			LocalDate localDt = new LocalDate(dateString);
			date = Date.valueOf(localDt.toString());
		} catch (Exception e) {
			String msg = (e.getMessage() != null) ? e.getMessage() : "Invalid date: " + dateString;
			throw new BadRequest(msg);
		}
		return date;
	}

	static String toStringValue(Date date) {
		if (date == null)
			return null;
		return date.toString();
	}

	static <T> String listToString(List<T> list) {
		if (list == null || list.isEmpty())
			return null;
		StringJoiner joiner = new StringJoiner(",", "[", "]");
		for (Object o : list) {
			if (o == null)
				joiner.add("null");
			else
				joiner.add(o.toString());
		}
		return joiner.toString();
	}

	static List<Long> toListOfLong(JsonNode jsonNode) {
		if (jsonNode == null)
			return null;
		List<Long> list = new ArrayList<>();
		Iterator<JsonNode> elements = jsonNode.elements();
		while (elements.hasNext()) {
			list.add(elements.next().asLong());
		}
		return list;
	}

	static List<String> toListOfString(JsonNode jsonNode) {
		if (jsonNode == null)
			return null;
		List<String> list = new ArrayList<>();
		Iterator<JsonNode> elements = jsonNode.elements();
		while (elements.hasNext()) {
			list.add(elements.next().asText());
		}
		return list;
	}

	static boolean includeField(List<String> fields, String... keys) {
		if (fields == null || fields.isEmpty())
			return true;

		return fields.containsAll(Arrays.asList(keys));
	}

	private static String toString(Object object) {
		if (object == null)
			return null;
		return String.valueOf(object);
	}

	public static void putAsStringIfNotNull(Map<String, Object> properties, String key, Object value) {
		if (value == null)
			return;
		properties.put(key, toString(value));
	}

	static Date getNowDate() {
		java.util.Date now = new java.util.Date();
		return new Date(now.getTime());
	}

	static Date newDate(String date) {
		if (date == null)
			return null;
		return java.sql.Date.valueOf(date);
	}

	public static boolean containsOrIsEmpty(List<String> list, Object item) {
		if (list == null || list.isEmpty() || list.contains(item))
			return true;
		return false;
	}

	public static boolean contains(List<String> list, String item) {
		if (list == null)
			return false;
		return list.contains(item);
	}

	public static Boolean isTrue(Boolean param) {
		return (param == null) ? false : param;
	}
}
