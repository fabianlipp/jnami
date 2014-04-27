package nami.beitrag.letters;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Verwaltet das Verzeichnis, in dem erzeugte Briefe abgelegt werden.
 * 
 * @author Fabian Lipp
 * 
 */
public class LetterDirectory {
    private File workdir;

    // Wird verwendet, um Daten zu formatieren
    // Sollte nur synchronized verwendet werden
    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    // Trenner zwischen den einzelnen Bestandteilen im Dateinamen
    private static final char SEPARATOR = '_';
    // Verwendete Dateiendungen
    private static final String FILE_ENDING_SRC = ".tex";
    private static final String FILE_ENDING_RES = ".pdf";

    /**
     * Erzeugt eine neue Instanz der Klasse die Briefe in einem vorgegebenen
     * Verzeichnis verwaltet.
     * 
     * @param workdir
     *            Verzeichnis in dem die Briefe abgelegt werden
     */
    public LetterDirectory(File workdir) {
        if (workdir == null || !workdir.isDirectory()) {
            throw new IllegalArgumentException(
                    "Given Workdir is not a directory: " + workdir);
        }
        this.workdir = workdir;
    }

    /**
     * Liefert das Arbeitsverzeichnis, in dem Dateien abgelegt werden.
     * 
     * @return Arbeitsverzeichnis
     */
    public File getWorkdir() {
        return workdir;
    }

    /**
     * Liefert die Datei (inkl. absoluten Pfad) zum übergebenen Dateinamen (es
     * wird angenommen, dass die Datei im Arbeitsverzeichnis liegt).
     * 
     * @param filename
     *            Dateiname (ohne Pfad). Die Datei wird im Arbeitsverzeichnis
     *            gesucht.
     * @return Datei
     */
    public File getFile(String filename) {
        return new File(workdir, filename);
    }

    /**
     * Liefert die aus dem angegebenen Dateinamen erzeugte Datei.
     * 
     * @param filename
     *            Dateiname (ohne Pfad). Die Datei wird im Arbeitsverzeichnis
     *            gesucht.
     * @return Datei
     */
    public File getGeneratedFile(String filename) {
        filename = filename.replace(FILE_ENDING_SRC, FILE_ENDING_RES);
        return new File(workdir, filename);
    }

    /**
     * Überprüft, ob die vorgegebene Datei bereits existiert und liefert ggf.
     * eine alternative (daraus abgeleiteten) Datei, die noch nicht existiert.
     * 
     * @param filename
     *            gewünschter Dateiname
     * @return Datei, die noch nicht existiert
     */
    private synchronized File findNewFilename(String filename) {
        File f = new File(workdir, filename + FILE_ENDING_SRC);
        int suffix = 0;
        while (f.exists()) {
            suffix++;
            f = new File(workdir, filename + SEPARATOR + suffix
                    + FILE_ENDING_SRC);
        }

        return f;
    }

    /**
     * Liefert einen Dateinamen für einen Brief, der an einzelne Person
     * adressiert ist.
     * 
     * @param type
     *            Typ des Briefs
     * @param date
     *            Datum, an dem der Brief versandt wird
     * @param nachname
     *            Nachname des Empfängers
     * @param vorname
     *            Vorname des Empfängers
     * @return Dateiname, der bisher noch nicht existiert
     */
    public synchronized File createFilenameSingle(LetterType type, Date date,
            String nachname, String vorname) {
        StringBuilder filename = new StringBuilder();
        filename.append(formatter.format(date));
        filename.append(SEPARATOR);
        filename.append(nachname).append(SEPARATOR);
        filename.append(vorname).append(SEPARATOR);
        filename.append(type.getFilenamePart());

        return findNewFilename(filename.toString());
    }

    /**
     * Liefert einen Dateinamen für eine Sammlung von Briefen, die an
     * verschiedene Personen adressiert sind.
     * 
     * @param type
     *            Typ der Briefe
     * @param date
     *            Datum, an dem die Briefe versandt werden
     * @return Dateiname, der bisher noch nicht existiert
     */
    public synchronized File createFilenameMultiple(LetterType type, Date date) {
        StringBuilder filename = new StringBuilder();
        filename.append(formatter.format(date));
        filename.append(SEPARATOR);
        filename.append(type.getFilenamePartPlural());

        return findNewFilename(filename.toString());
    }
}
