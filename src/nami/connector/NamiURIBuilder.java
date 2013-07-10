package nami.connector;

import org.apache.http.client.utils.URIBuilder;

public class NamiURIBuilder extends URIBuilder {
    public NamiURIBuilder(boolean useSsl, String namiServer, String namiDeploy) {
        super();
        if (useSsl) {
            setScheme("https");
        } else {
            setScheme("http");
        }
        setHost(namiServer);
        setPath("/" + namiDeploy);
    }

    public NamiURIBuilder(boolean useSsl, String namiServer, String namiDeploy, String path) {
        this(useSsl, namiServer, namiDeploy);
        appendPath(path);
    }    
    
    public void appendPath(String pathAppendix) {
        String path = getPath();
        if ((path.charAt(path.length() - 1) != '/') && (pathAppendix.charAt(0) != '/')) {
            setPath(path + "/" + pathAppendix);    
        } else {
            setPath(path + pathAppendix);            
        }        
    }

    
}
