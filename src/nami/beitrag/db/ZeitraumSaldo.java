package nami.beitrag.db;

import java.math.BigDecimal;

import lombok.Data;
import nami.connector.Halbjahr;

/**
 * Stellt die Salden der Buchungen für ein bestimmtes Halbjahr dar.
 * 
 * @author Fabian Lipp
 * 
 */
@Data
public class ZeitraumSaldo {
    private Halbjahr halbjahr;

    /**
     * Anzahl der Buchungen im Halbjahr.
     */
    private int anzahlBuchungen;

    /**
     * Summe der Buchungsbeträge im Halbjahr.
     */
    private BigDecimal saldo;
}
