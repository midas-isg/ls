package models.exceptions;

import org.postgresql.util.PSQLException;

public class PostgreSQLException extends RuntimeException {

	private static final long serialVersionUID = -2402228834044542692L;

	private String sqlState;

	public PostgreSQLException() {
		super();
	}

	public PostgreSQLException(String message) {
		super(message);
	}

	public PostgreSQLException(String message, Throwable cause) {
		super(message, cause);
	}

	public PostgreSQLException(Throwable cause) {
		super(cause);
	}

	public PostgreSQLException(Throwable cause, String sqlState) {
		super(cause);
		this.setSQLState(sqlState);
	}

	public String getSQLState() {
		return this.sqlState;
	}

	public void setSQLState(String sqlState) {
		this.sqlState = sqlState;
	}

	public static PSQLException toPSQLException(Exception e) {
		PSQLException pe = null;
		Throwable cause = e.getCause();
		while (cause != null) {
			if (cause instanceof PSQLException){
				pe = (PSQLException) cause;
				break;
			}
			cause = cause.getCause();
		}
		return pe;
	}
}
