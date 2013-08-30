package nami.configuration;

import java.io.File;

/**
 * Wird geworfen, wenn es Probleme mit dem Zugriff aufs Programmverzeichnis
 * gibt.
 * 
 * @author Fabian Lipp
 * 
 */
public class ApplicationDirectoryException extends Exception {
    private static final long serialVersionUID = 6429592387040497174L;

    /**
     * Erzeugt die Exception mit einer übergebenen Nachricht.
     * 
     * @param message
     *            Nachricht
     */
    public ApplicationDirectoryException(String message) {
        super(message);
    }

    /**
     * Erzeugt die Exception mit einer übergebenen Nachricht und einer Datei.
     * Der Dateiname (inkl. Pfad) wird mit der Fehlernachricht ausgegeben.
     * 
     * @param message
     *            Nachricht
     * @param f
     *            Dateiname
     */
    public ApplicationDirectoryException(String message, File f) {
        super(message + System.lineSeparator() + f.getAbsolutePath());
    }
}
