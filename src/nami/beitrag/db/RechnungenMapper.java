package nami.beitrag.db;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import lombok.Data;
import lombok.Getter;
import nami.beitrag.Rechnungsstatus;
import nami.connector.Halbjahr;

import org.apache.ibatis.annotations.Param;

/**
 * Stellt Datenbankabfragen zur Arbeit mit Rechnungen bereit.
 * 
 * @author Fabian Lipp
 * 
 */
public interface RechnungenMapper {
    /**
     * Möglichkeiten Buchungen nach dem Vorausberechnungs-Feld zu filtern.
     */
    static enum VorausberechnungFilter {
        /**
         * Keine Vorausberechnungen ausgeben.
         */
        KEINE,

        /**
         * Alle Buchungen ausgeben.
         */
        AUCH,

        /**
         * Nur Vorausberechnungen ausgeben.
         */
        NUR
    }

    /**
     * Filtert Mitglieder danach, ob ihnen ein gültiges, aktives
     * Lastschrift-Mandat zugewiesen ist.
     */
    static enum ZahlungsartFilter {
        /**
         * Mitglied hat ein gültiges, aktives SEPA-Mandat.
         */
        LASTSCHRIFT,

        /**
         * Mitglied hat <i>kein</i> gültiges, aktives SEPA-Mandat.
         */
        KEINE_LASTSCHRIFT,

        /**
         * Filtere Mitglieder nicht nach diesem Kriterium.
         */
        ALLE
    }

    /**
     * Beschreibt Filterkriterien für die Suche nach Personen und Buchungen.
     */
    @Data
    static class FilterSettings {
        /**
         * Frühestes Halbjahr, dessen Buchungen gewählt werden. Falls der Wert
         * <tt>null</tt> ist, wird nicht nach diesem Kriterium gefiltert.
         */
        private Halbjahr halbjahrVon = null;

        /**
         * Spätestes Halbjahr, dessen Buchungen gewählt werden. Falls der Wert
         * <tt>null</tt> ist, wird nicht nach diesem Kriterium gefiltert.
         */
        private Halbjahr halbjahrBis = null;

        /**
         * Gibt an, ob Buchungen selektiert werden sollen, die
         * Vorausberechnungen sind.
         */
        private VorausberechnungFilter vorausberechnung;

        /**
         * Hiermit können nur Mitglieder selektiert werden, für die ein aktives
         * Lastschrift-Mandat vorhanden ist.
         */
        private ZahlungsartFilter zahlungsart = null;

        /**
         * Gibt an, ob auch Buchungen selektiert werden, die bereits in einer
         * Rechnung enthalten sind.
         */
        private boolean bereitsBerechnet;
    }

    /**
     * Liefert alle Mitglieder, bei denen das Beitragskonto nicht ausgeglichen
     * sind. Dabei werden nur Buchungen berücksichtigt, die den übergebenen
     * Filterkriterien entsprechen.
     * 
     * @param filterSettings
     *            Kriterien, nach denen die Buchungen gefiltert werden
     * @return Mitglieder, deren Beitragskonto (bezogen auf die Buchungen, die
     *         <tt>filterSettings</tt> erfüllen) nicht ausgeglichen ist
     */
    Collection<DataMitgliederForderungen> mitgliederOffeneForderungen(
            @Param("filterSettings") FilterSettings filterSettings);

    /**
     * Liefert für ein bestimmtes Mitglied alle Buchungen, die vorgegebene
     * Kriterien erfüllen.
     * 
     * @param mitgliedId
     *            ID des Mitglieds
     * @param filterSettings
     *            Kriterien, nach denen die Buchungen gefiltert werden
     * @return Buchungen, die die Kriterien erfüllen
     */
    Collection<BeitragBuchung> getBuchungenFiltered(
            @Param("mitgliedId") int mitgliedId,
            @Param("filterSettings") FilterSettings filterSettings);

    /**
     * Liefert die höchste Rechnungsnummer, die im übergebenen Jahr bisher
     * verwendet wurde.
     * 
     * @param jahr
     *            angefragtes Jahr
     * @return höchste bisher verwendete Rechnungsnummer
     */
    int maxRechnungsnummer(int jahr);

    /**
     * Fügt eine Rechnung in die Datenbank ein. Die <tt>rechnungId</tt> der neu
     * eingefügten Rechnung ist anschließend im Objekt gespeichert, das als
     * Parameter übergeben wurde.
     * 
     * @param rechnung
     *            einzufügende Rechnung
     */
    void insertRechnung(BeitragRechnung rechnung);

    /**
     * Holt den vollen Datensatz einer Rechnung aus der Datenbank.
     * 
     * @param rechnungId
     *            ID der Rechnung
     * @return alle Daten der Rechnung
     */
    BeitragRechnung getRechnung(int rechnungId);

    /**
     * Aktualisiert eine Rechnung in der Datenbank. Dabei wird nur das
     * Status-Feld geändert (die anderen Felder einer Rechnung sollten nach dem
     * Anlegen nicht mehr verändert werden).
     * 
     * @param rechnung
     *            Rechnungsdaten
     */
    void updateRechnung(BeitragRechnung rechnung);

    /**
     * Ergebnis-Datentyp, der verwendet wird, wenn eine Rechnung mit allen
     * Informationen aus der Datenbank geholt wird. Dabei wird der entsprechende
     * Mitgliedsdatensatz (Name, Anschrift, usw.) mit ausgelesen und außerdem
     * eine Liste der Posten, die in der Rechnung enthalten sind.
     */
    @Getter
    public static class DataRechnungMitBuchungen {
        private BeitragRechnung rechnung;
        private BeitragMitglied mitglied;
        private List<BeitragBuchung> buchungen;
    }

    /**
     * Liefert eine Rechnung mit allen Informationen aus der Datenbank. Dabei
     * wird neben den Daten der Rechnung noch der entsprechende
     * Mitgliedsdatensatz und die Liste der enthaltenen Posten aus der Datenbank
     * ausgelesen.
     * 
     * @param rechnungId
     *            ID der angefragten Rechnung
     * @return Rechnung mit verknüpften Informationen
     */
    DataRechnungMitBuchungen getRechnungMitBuchungen(int rechnungId);

    /**
     * Fügt einen Posten zu einer Rechnung hinzu.
     * 
     * @param rechnungId
     *            Rechnung, zu der der Posten gehört
     * @param buchungId
     *            Buchung, die damit beglichen werden soll
     * @param buchungstext
     *            Text, der auf der Rechnung angezeigt wird
     */
    void insertPosten(@Param("rechnungId") int rechnungId,
            @Param("buchungId") int buchungId,
            @Param("buchungstext") String buchungstext);

    /**
     * Beschreibt die zurückgegebenen Objekte bei der Rechnungsabfrage mit
     * Filterkriterien.
     */
    @Getter
    public class DataFindRechnungen {
        /**
         * Datensatz der Rechnung.
         */
        private BeitragRechnung rechnung;

        /**
         * Anzahl erstellter Mahnungen.
         */
        private int mahnungen;

        /**
         * Frist der letzten Mahnung. Wird auf <tt>null</tt> gesetzt, falls noch
         * keine Mahnungen erstellt wurden.
         */
        private Date letzteFrist;

        /**
         * Vorname des Mitglieds.
         */
        private String vorname;

        /**
         * Nachname des Mitglieds.
         */
        private String nachname;
    }

    /**
     * Fragt alle Rechnungen aus der Datenbank ab, die den übergebenen Kriterien
     * entsprechen.
     * 
     * @param erstellungsjahr
     *            Legt fest, aus welchem Jahr die Rechnungen stammen sollen.
     *            Falls der Wert <tt>-1</tt> ist, wird nicht nach dem
     *            Erstellungsjahr der Rechnung gefiltert.
     * @param status
     *            Legt fest, welchen Status die Rechnung haben muss. Falls der
     *            Wert <tt>null</tt> ist, wird nicht nach dem Status gefiltert.
     * @param ueberfaellig
     *            Falls dieser Parameter gesetzt ist, werden nur überfällige
     *            Rechnungen geliefert. Dabei bedeutet überfällig, dass die
     *            Frist der Rechnung selbst (und ggf. die der letzten erstellte
     *            Mahnung) bereits verstrichen ist. Dieser Parameter macht nur
     *            dann Sinn, wenn der Status auf <tt>OFFEN</tt> gesetzt wird
     * @param mitgliedId
     *            Es werden nur Rechnungen für dieses Mitglied gefunden. Falls
     *            der Wert <tt>-1</tt> ist, wird nicht nach dem Mitglied
     *            gefiltert.
     * @return Rechnungen, die den Kriterien entsprechen
     */
    ArrayList<DataFindRechnungen> findRechnungen(
            @Param("erstellungsjahr") int erstellungsjahr,
            @Param("status") Rechnungsstatus status,
            @Param("ueberfaellig") boolean ueberfaellig,
            @Param("mitgliedId") int mitgliedId);

    /**
     * Beschreibt die zurückgegebenen Objekte bei der Abfrage der Posten einer
     * Rechnung.
     */
    @Getter
    public class DataListPosten {
        /**
         * Buchung, auf die sich der Posten bezieht.
         */
        private BeitragBuchung buchung;

        /**
         * Buchungstext auf der Rechnung.
         */
        private String buchungstext;
    }

    /**
     * Liefert die Posten aus der Datenbank, die zu einer Rechnung gehören.
     * 
     * @param rechnungId
     *            ID der Rechnung, deren Posten gesucht werden
     * @return Posten der Rechnung
     */
    ArrayList<DataListPosten> getPosten(int rechnungId);

    /**
     * Liefert alle Mahnungen, die zu einer Rechnung erstellt wurden.
     * 
     * @param rechnungId
     *            ID der Rechnung
     * @return Mahnungen, die sich auf die Rechnung beziehen
     */
    ArrayList<BeitragMahnung> getMahnungen(int rechnungId);

    /**
     * Beschreibt die zurückgegebenen Objekte bei der Abfrage der Betragssummen
     * für die Halbjahre.
     */
    @Getter
    public class DataHalbjahrBetraege {
        /**
         * Halbjahr.
         */
        private Halbjahr halbjahr;

        /**
         * Summe der Beträge für das Halbjahr.
         */
        private BigDecimal betrag;
    }

    /**
     * Liefert die Beträge der Posten einer Rechnung gruppiert nach dem
     * Halbjahr, dem sie zugeordnet sind.
     * 
     * @param rechnungId
     *            ID der Rechnung
     * @return Paare von Halbjahren und deren Betragssummen
     */
    ArrayList<DataHalbjahrBetraege> getHalbjahrBetraege(int rechnungId);

    /**
     * Fügt eine Mahnung in die Datenbank ein. Die generierte <tt>mahnungId</tt>
     * der neu eingefügten Rechnung ist anschließend im Objekt gespeichert, das
     * als Parameter übergeben wurde.
     * 
     * @param mahnung
     *            einzufügende Mahnung
     */
    void insertMahnung(BeitragMahnung mahnung);

    /**
     * Ergebnis-Datentyp, der verwendet wird, wenn eine Mahnung mit allen
     * Informationen aus der Datenbank geholt wird. Dabei wird die entsprechende
     * Rechnung und der Mitgliedsdatensatz (Name, Anschrift, usw.) mit
     * ausgelesen und außerdem eine Liste der vorherigen Mahnungen.
     */
    @Getter
    public class DataMahnungKomplett {
        private BeitragMahnung mahnung;
        private BeitragRechnung rechnung;
        private BeitragMitglied mitglied;
        private BeitragMahnung vorherigeMahnung;
    }

    /**
     * Holt eine Mahnung mit allen Informationen aus der Datenbank. Diese
     * enthalten die entsprechende Rechnung und den Mitgliedsdatensatz. Außerdem
     * werden alle vorherigen Mahnungen geliefert, d.h. diejenigen, bei denen
     * die mahnungArt geringer ist.
     * 
     * @param mahnungId
     *            ID der Mahnung
     * @return aller Informationen aus der Datenbank, die zur Mahnung gehören
     */
    DataMahnungKomplett getMahnungKomplett(int mahnungId);
}
