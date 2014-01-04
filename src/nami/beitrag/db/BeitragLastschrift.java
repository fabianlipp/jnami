package nami.beitrag.db;

import lombok.Data;

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
}
