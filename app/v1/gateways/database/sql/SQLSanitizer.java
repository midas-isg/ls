package v1.gateways.database.sql;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLSanitizer {
	private final static String[] KEYWORDS = { "ABORT", "ALTER", "ANALYZE",
			"CHECKPOINT", "COMMIT", "COPY", "CREATE", "DELETE", "DISCARD",
			"DROP", "END", "GRANT", "FETCH", "EXEC", "EXECUTE", "INSERT",
			"LOAD", "LOCK", "REINDEX", "RELEASE SAVEPOINT", "RESET", "REVOKE",
			"ROLLBACK", "SELECT", "SET", "SHOW", "START", "TRUNCATE",
			"UNLISTEN", "UPDATE" };

	private final static String[] ESPECIAL_CHARS = { "\"", "\'", "/*", "*/",
			"--", "=", "<", ">", "@" };

	public static boolean isUnsafe(String value) {
		if (value == null)
			return false;
		for (String keyword : KEYWORDS) {
			String regEx = "(;[^\\w]*" + keyword + "[^\\w])" + "|" + "([^\\w]*"
					+ keyword + "[^\\w].*;)";
			Pattern pattern = compile(regEx, CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(value);
			if (matcher.find())
				return true;
		}
		for (String espChar : ESPECIAL_CHARS) {
			if (value.contains(espChar))
				return true;
		}
		return false;

	}

	public static String tokenize(String value) {
		if (value == null)
			return null;
		String tokenized = value.replaceAll("['-]", " ");
		return tokenized;
	}
}