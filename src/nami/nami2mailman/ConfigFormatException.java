package nami.nami2mailman;

public class ConfigFormatException extends Exception {
    
    private static final long serialVersionUID = -7182026720340191111L;

    public ConfigFormatException() {
        super();
    }

    public ConfigFormatException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ConfigFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigFormatException(Throwable cause) {
        super(cause);
    }

    public ConfigFormatException(String string) {
        super(string);
    }

}
