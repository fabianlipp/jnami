package nami.beitrag.db;

import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;
import nami.connector.Halbjahr;

/**
 * Beschreibt einen Zeitraum für den Mitgliedsbeiträge berechnet werden.
 * 
 * @author Fabian Lipp
 * 
 */
@Data
@NoArgsConstructor
public class BeitragZeitraum {
    private Halbjahr halbjahr;
    private Date abgeschlossen = null;

    /**
     * Erzeugt einen neuen Zeitraum, gegeben durch Halbjahr und Jahr.
     * 
     * @param halbjahr
     *            Halbjahr
     * @param jahr
     *            Jahr
     */
    public BeitragZeitraum(int halbjahr, int jahr) {
        this.halbjahr = new Halbjahr(halbjahr, jahr);
    }

    /**
     * Erzeugt einen neuen Zeitraum, gegeben durch ein Halbjahr.
     * 
     * @param halbjahr
     *            Halbjahr (inkl. Jahr)
     */
    public BeitragZeitraum(Halbjahr halbjahr) {
        this.halbjahr = halbjahr;
    }

    /**
     * Erzeugt den Zeitraum, in dem das übergebene Datum liegt.
     * 
     * @param date
     *            Datum, dessen Zeitraum bestimmt werden soll
     */
    public BeitragZeitraum(Date date) {
        this.halbjahr = new Halbjahr(date);
    }
}
