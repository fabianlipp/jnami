package nami.beitrag.db;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

import lombok.Data;
import nami.beitrag.Buchungstyp;
import nami.connector.Halbjahr;
import org.apache.ibatis.annotations.Param;

/**
 * Beschreibt einen Mapper für Datenbankabfragen, auf denen die Reports
 * basieren.
 * 
 * @author Fabian Lipp
 * 
 */
public interface ReportsMapper {
    /**
     * Beschreibt die Daten, die für die Halbjahresabrechnung aus der Datenbank
     * abgefragt werden.
     *
     * @author Fabian Lipp
     *
     */
    @Data
    class DataAbrechnungHalbjahr {
        private int buchungId;
        private int mitgliedId;
        private int namiBuchungId;
        private String rechnungsnummer;
        private Date datum;
        private BigDecimal betrag;
        private boolean vorausberechnung;
        private String kommentar;
        private String mitgliedsnummer;
        private String nachname;
        private String vorname;
        private Buchungstyp typ;
    }

    /**
     * Erstellt die Abrechnung für ein Halbjahr. Diese Abrechnung listet für
     * jedes Mitglied das gesamte Beitragskonto auf.
     * 
     * @param halbjahr
     *            Halbjahr, für das die Abrechnung abgefragt wird
     * @param ausgeglichen
     *            Zeige auch Mitglieder mit ausgeglichenen Beitragskonten
     * @return Liste mit Mitgliederdaten und -buchungen
     */
    Collection<DataAbrechnungHalbjahr> abrechnungHalbjahr(@Param("halbjahr") Halbjahr halbjahr,
                                                          @Param("ausgeglichen") boolean ausgeglichen);

    @Data
    class DataAbrechnungNachTypen {
        private Buchungstyp typ;
        private boolean vorausberechnung;
        private BigDecimal betrag;
    }

    Collection<DataAbrechnungNachTypen> abrechnungNachTypenHalbjahr(@Param("halbjahr") Halbjahr halbjahr);

    /**
     * Listet alle aktiven (beitragspflichtigen) Mitglieder auf, denen kein
     * gültiges SEPA-Mandat zugeordnet ist.
     * 
     * @return Liste der Mitglieder ohne SEPA-Mandat
     */
    Collection<BeitragMitglied> mitgliederOhneSepaMandat();

    @Data
    class DataAnzahlBuchungenProHalbjahr {
        private Halbjahr halbjahr;
        private int vorausberechnungen;
        private int endgueltig;
        private int total;
    }

    /**
     * Zeigt die Anzahl der Buchungen (aufgeteilt in Vorausbuchungen
     * und endgültige Buchungen) pro Halbjahr.
     *
     * @return Liste der Halbjahre mit Anzahl der Buchungen
     */
    Collection<DataAnzahlBuchungenProHalbjahr> anzahlBuchungenProHalbjahr();

    @Data
    class DataUnausgeglicheneBeitragskonten {
        private Halbjahr halbjahr;
        private String mitgliedsnummer;
        private String nachname;
        private String vorname;
        private BigDecimal betrag;
    }

    /**
     * Zeigt die unausgeglichenen Beitragskonten gruppiert nach
     * Halbjahr inkl. Saldo an.
     *
     * @return Liste der unausgeglichenen Beitragskonten
     */
    Collection<DataUnausgeglicheneBeitragskonten> unausgeglicheneBeitragskontenProHalbjahr();
}
