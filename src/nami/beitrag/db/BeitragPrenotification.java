package nami.beitrag.db;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

/**
 * Beschreibt die Daten einer Prenotification, die in die Datenbank eingetragen
 * wurde.
 * 
 * @author Fabian Lipp
 * 
 */
@Data
public class BeitragPrenotification {
    private int prenotificationId;
    private int mandatId;
    private Date datum;
    private BigDecimal betrag;
    private Date faelligkeit;
    private boolean regelmaessig;
}
