package v1.models.exceptions;


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
}
