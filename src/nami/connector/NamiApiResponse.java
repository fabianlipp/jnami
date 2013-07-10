package nami.connector;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class NamiApiResponse<ResponseT> {

    private String apiSessionName;
    private String apiSessionToken;
    private int minorNumber;
    private int majorNumber;
    private int statusCode;
    private String statusMessage;
    private String servicePrefix;
    private String methodCall;
    private ResponseT response;
    
    /**
     * @return the statusCode
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    public ResponseT getResponse() {
        return response;
    }
    
    public static Type getType(final Type responseT) {
        Type type = new ParameterizedType() {
            
            @Override
            public Type getRawType() {
                return NamiApiResponse.class;
            }
            
            @Override
            public Type getOwnerType() {
                return null;
            }
            
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] {responseT};
            }
        };
        return type;
    }
}
