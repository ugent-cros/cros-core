package exceptions;

/**
 * Created by yasser on 16/03/15.
 */
public class IncompatibleSystemException extends Exception {

    public IncompatibleSystemException() {
        super();
    }

    public IncompatibleSystemException(Throwable cause) {
        super(cause);
    }

    public IncompatibleSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
