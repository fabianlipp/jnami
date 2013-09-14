package nami.beitrag;

import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Beschreibt einen Zeitraum für den Mitgliedsbeiträge berechnet werden.
 * 
 * @author Fabian Lipp
 * 
 */
@Data
@NoArgsConstructor
public class BeitragZeitraum {
    private int halbjahr;
    private int jahr;
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
        this.halbjahr = halbjahr;
        this.jahr = jahr;
    }
}
