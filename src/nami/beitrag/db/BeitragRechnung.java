package nami.beitrag.db;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import nami.beitrag.Rechnungsstatus;

/**
 * Beschreibt die Grunddaten einer Rechnung, die erstellt wurde.
 * 
 * @author Fabian Lipp
 * 
 */
@Data
public class BeitragRechnung {
    private int rechnungId;
    private int mitgliedId;
    private int rechnungsNummer;
    private Date datum;
    private Date frist;
    private Rechnungsstatus status;

    /**
     * Rechnungsbetrag (d. h. Summe der enthaltenen Posten).
     * 
     * Dieses Feld steht nicht in der Datenbank, sondern wird in der SQL-Abfrage
     * aus den verknüpften Posten berechnet.
     */
    @Setter(AccessLevel.NONE)
    private BigDecimal betrag;

    /**
     * Liefert die vollständige Rechnungsnummer.
     * 
     * @return Rechnungsnummer inklusive Jahreszahl
     */
    public String getCompleteRechnungsNummer() {
        String jahreszahl = new SimpleDateFormat("y").format(datum);
        return jahreszahl + "/" + rechnungsNummer;
    }
}
