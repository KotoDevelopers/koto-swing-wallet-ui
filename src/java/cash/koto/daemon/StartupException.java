package cash.koto.daemon;

/**
 * 2018-01-31
 *
 * @author KostaPC
 * c0f3.net
 */
public class StartupException extends RuntimeException {
    public StartupException(String message) {
        super(message);
    }

    public StartupException(String message, Throwable cause) {
        super(message, cause);
    }

    public StartupException(Throwable cause) {
        super(cause);
    }
}
