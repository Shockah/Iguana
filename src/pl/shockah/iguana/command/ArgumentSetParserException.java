package pl.shockah.iguana.command;

public class ArgumentSetParserException extends Exception {
	public ArgumentSetParserException() {
		super();
	}

	public ArgumentSetParserException(String message) {
		super(message);
	}

	public ArgumentSetParserException(Throwable cause) {
		super(cause);
	}

	public ArgumentSetParserException(String message, Throwable cause) {
		super(message, cause);
	}
}