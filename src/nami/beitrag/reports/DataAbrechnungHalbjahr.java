package nami.beitrag.reports;

import java.math.BigDecimal;
import java.util.Date;

import nami.beitrag.Buchungstyp;
import lombok.Data;

/**
 * Beschreibt die Daten, die für die Halbjahresabrechnung aus der Datenbank
 * abgefragt werden.
 * 
 * @author Fabian Lipp
 * 
 */
@Data
public class DataAbrechnungHalbjahr {
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
