package nami.beitrag.db;

import java.util.Date;

import lombok.Data;

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
}
