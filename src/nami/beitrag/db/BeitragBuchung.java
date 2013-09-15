package nami.beitrag.db;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import nami.beitrag.Buchungstyp;
import nami.connector.namitypes.NamiBeitragszahlung;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Beschreibt eine Buchung aus der Datenbank.
 * 
 * @author Fabian Lipp
 * 
 */
@Data
@NoArgsConstructor
public class BeitragBuchung {
    private int buchungId;
    private int mitgliedId;
    private Integer namiBuchungId;
    private String rechnungsNummer;
    private Buchungstyp typ;
    private Date datum;
    private BigDecimal betrag;
    private int halbjahr;
    private int jahr;
    private boolean vorausberechnung;
    private String kommentar;

    /**
     * Erzeugt eine Buchung mit den aus NaMi geladenen Daten.
     * 
     * @param mitgliedId
     *            ID des Mitglieds
     * @param beitr
     *            Beitragszahlungs-Datensatz aus NaMi
     */
    public BeitragBuchung(int mitgliedId, NamiBeitragszahlung beitr) {
        this.mitgliedId = mitgliedId;

        namiBuchungId = beitr.getId();
        rechnungsNummer = beitr.getRechungsnummer();
        typ = Buchungstyp.RECHNUNG_BUNDESEBENE;

        // Datum
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        if (beitr.getHalbjahr() == 2) {
            cal.set(Calendar.MONTH, Calendar.JULY);
        } else {
            cal.set(Calendar.MONTH, Calendar.JANUARY);
        }
        cal.set(Calendar.YEAR, beitr.getJahr());
        datum = cal.getTime();

        betrag = beitr.getValue();
        halbjahr = beitr.getHalbjahr();
        jahr = beitr.getJahr();
        vorausberechnung = false;
        kommentar = beitr.getBuchungstext();
    }

    /**
     * Liefert den Zeitraum der Buchung.
     * 
     * @return Zeitraum, zu dem die Buchung gehört
     */
    public BeitragZeitraum getZeitraum() {
        return new BeitragZeitraum(halbjahr, jahr);
    }
}
