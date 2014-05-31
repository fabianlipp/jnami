package nami.beitrag.db;

import java.util.ArrayList;

import lombok.Getter;

import org.apache.ibatis.annotations.Param;

/**
 * Stellt Datenbankabfragen zur Arbeit mit vorgemerkten Abmeldungen bereit.
 * 
 * @author Fabian Lipp
 * 
 */
public interface AbmeldungenMapper {
    /**
     * Fügt eine Abmeldung in die Datenbank ein. Die <tt>abmeldungId</tt> der
     * neu eingefügten Abmeldung ist anschließend im Objekt gespeichert, das als
     * Parameter übergeben wurde.
     * 
     * @param abmeldung
     *            einzufügende Abmeldung
     */
    void insertAbmeldung(BeitragAbmeldung abmeldung);

    /**
     * Holt den vollen Datensatz einer Abmeldung aus der Datenbank.
     * 
     * @param abmeldungId
     *            ID der Abmeldung
     * @return alle Daten der Abmeldung
     */
    BeitragAbmeldung getAbmeldung(int abmeldungId);

    /**
     * Ergebnis-Datentyp bei der Suche nach Abmeldungen. Enthält zusätzlich die
     * Daten des betreffenden Mitglieds.
     */
    @Getter
    public static class DataAbmeldungMitglied {
        private BeitragAbmeldung abmeldung;
        private BeitragMitglied mitglied;
    }

    /**
     * Aktualisiert eine Abmeldung in der Datenbank.
     * 
     * @param abmeldung
     *            Daten der Abmeldung
     */
    void updateAbmeldung(BeitragAbmeldung abmeldung);

    /**
     * Liefert alle Abmeldungen (inkl. der notwendigen Stammdaten des Mitglieds)
     * aus der Datenbank.
     * 
     * @param unbearbeitetOnly
     *            falls <tt>true</tt> werden nur Abmeldungen geliefert, die noch
     *            nicht in NaMi übertragen wurden
     * @return gefunden Abmeldungen
     */
    ArrayList<DataAbmeldungMitglied> findAbmeldungen(
            @Param("unbearbeitetOnly") boolean unbearbeitetOnly);
}
