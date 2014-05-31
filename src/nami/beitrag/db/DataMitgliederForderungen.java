package nami.beitrag.db;

import java.math.BigDecimal;

import lombok.Data;

/**
 * Ein Element, das auf die Anfrage nach Mitgliedern mit offenen Forderungen
 * geliefert wird.
 * 
 * @author Fabian Lipp
 * 
 */
@Data
public class DataMitgliederForderungen {
    /**
     * ID des Mitglieds.
     */
    private int mitgliedId;

    /**
     * Migliedsnummer.
     */
    private String mitgliedsnummer;

    /**
     * Vorname.
     */
    private String vorname;

    /**
     * Nachname.
     */
    private String nachname;

    /**
     * Summe aller Buchungen des Mitglieds (ggf. werden nur Buchungen in einem
     * bestimmten Halbjahr berücksichtigt).
     */
    private BigDecimal saldo;
}
