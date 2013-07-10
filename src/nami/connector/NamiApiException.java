package nami.connector;

public class NamiApiException extends NamiException {

    private static final long serialVersionUID = -1855332145425346423L;

    public NamiApiException(String str) {
        super(str);
    }
    
    public NamiApiException(NamiApiResponse<? extends Object> resp) {
        super(resp.getStatusCode() + ": " + resp.getStatusMessage());
    }
}
