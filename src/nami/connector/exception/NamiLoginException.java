package nami.connector.exception;

import nami.connector.NamiApiResponse;

/**
 * Exception, die bei einem Misserfolg beim Login geworfen wird. Die
 * Fehlermeldung von NaMi wird in die Exception gespeichert.
 * 
 * @author Fabian Lipp
 * 
 */
public class NamiLoginException extends NamiApiException {
    private static final long serialVersionUID = -5171203317792006455L;

    /**
     * Erzeugt die Exception mit der Antwort von NaMi.
     * 
     * @param resp
     *            Antwort vom NaMi-Server
     */
    public NamiLoginException(NamiApiResponse<? extends Object> resp) {
        super(resp);
    }

}
