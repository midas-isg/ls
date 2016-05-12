package models.exceptions;

public class NotFound extends RuntimeException {

	private static final long serialVersionUID = 6471550874941802944L;

	public NotFound() {
		super();
	}

	public NotFound(String message) {
		super(message);
	}

	public NotFound(String message, Throwable cause) {
		super(message, cause);
	}

	public NotFound(Throwable cause) {
		super(cause);
	}
}
