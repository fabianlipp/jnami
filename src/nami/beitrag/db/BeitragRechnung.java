package nami.beitrag.db;

import java.util.Date;

import com.ibm.icu.text.SimpleDateFormat;

import lombok.Data;

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
    private boolean beglichen;

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
