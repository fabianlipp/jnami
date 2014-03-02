package nami.beitrag.db;

import java.math.BigDecimal;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
 * Beschreibt eine einzelne Lastschrift in der Datenbank.
 * 
 * @author Fabian Lipp
 * 
 */
@Data
public class BeitragLastschrift {
    private int lastschriftId;
    private int sammelLastschriftId;
    private int mandatId;
    private String verwendungszweck;

    // Die folgenden Felder sind nicht direkt in der Datenbank gespeichert,
    // sondern werden bei der Abfrage berechnet.
    @Setter(AccessLevel.NONE)
    private BigDecimal betrag;
}
