package club.dreamccc.ehtdownload.exception;

public class CanIgnoreException extends Exception {
    public CanIgnoreException(String message) {
        super(message);
    }

    public CanIgnoreException(Throwable cause) {
        super(cause);
    }

    public CanIgnoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
