package nami.beitrag.db;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

/**
 * Beschreibt eine Sammellastschrift in der Datenbank.
 * 
 * @author Fabian Lipp
 * 
 */
@Data
public class BeitragSammelLastschrift {
    private int sammelLastschriftId;
    private Date faelligkeit;
    private boolean ausgefuehrt;
    private String bezeichnung;

    // Die folgenden Felder sind nicht direkt in der Datenbank gespeichert
    @Setter(AccessLevel.NONE)
    private int anzahlLastschriften;
    @Setter(AccessLevel.NONE)
    private BigDecimal betrag;
    @Setter(AccessLevel.NONE)
    private boolean alleGueltig;
}
