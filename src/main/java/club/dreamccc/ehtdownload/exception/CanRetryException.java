package club.dreamccc.ehtdownload.exception;

public class CanRetryException extends RuntimeException {
    public CanRetryException(String message) {
        super(message);
    }

    public CanRetryException(Throwable cause) {
        super(cause);
    }

    public CanRetryException(String message, Throwable cause) {
        super(message, cause);
    }
}
