package nami.connector;

public class NamiServer {

    public static final NamiServer TESTSERVER = new NamiServer(
            "namitest.dpsg.de", false, "ica");
    public static final NamiServer LIVESERVER = new NamiServer("nami.dpsg.de",
            true, "ica");

    private String namiServer = "namitest.dpsg.de";
    private boolean useSsl = false;
    private String namiDeploy = "ica";

    public NamiServer(String namiServer, boolean useSsl, String namiDeploy) {
        this.namiServer = namiServer;
        this.useSsl = useSsl;
        this.namiDeploy = namiDeploy;
    }

    public String getNamiServer() {
        return namiServer;
    }

    public boolean getUseSsl() {
        return useSsl;
    }

    public String getNamiDeploy() {
        return namiDeploy;
    }
}
