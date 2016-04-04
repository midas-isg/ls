package models.exceptions;

public class BadRequest extends RuntimeException {
	private static final long serialVersionUID = -8830188701796386829L;

	public BadRequest() {
		super();
	}

	public BadRequest(String message) {
		super(message);
	}

	public BadRequest(String message, Throwable cause) {
		super(message, cause);
	}

	public BadRequest(Throwable cause) {
		super(cause);
	}
}
