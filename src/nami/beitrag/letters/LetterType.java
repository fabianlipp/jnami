package nami.beitrag.letters;

/**
 * Beschreibt die verschiedenen Typen von Briefen.
 * 
 * @author Fabian Lipp
 * 
 */
public enum LetterType {
    /**
     * Rechnung.
     */
    RECHNUNG("rechnung", "rechnungen"),

    /**
     * Mahnung.
     */
    MAHNUNG("mahnung", "mahnungen"),

    /**
     * SEPA-Prenotification.
     */
    PRENOTIFICATION("prenotification", "prenotifications");

    private String filenamePart;
    private String filenamePartPlural;

    private LetterType(String filenamePart, String filenamePartPlural) {
        this.filenamePart = filenamePart;
        this.filenamePartPlural = filenamePartPlural;
    }

    /**
     * Liefert den String, der für diesen Brieftyp im Dateinamen verwendet wird.
     * 
     * @return Bestandteil für Dateinamen im Singular
     */
    public String getFilenamePart() {
        return filenamePart;
    }

    /**
     * Liefert den String, der für diesen Brieftyp im Dateinamen verwendet wird,
     * wenn mehrere Briefe in der Datei enthalten sind.
     * 
     * @return Bestandteil für Dateinamen im Plural
     */
    public String getFilenamePartPlural() {
        return filenamePartPlural;
    }
}
