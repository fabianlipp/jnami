package nami.beitrag.db;

import java.util.Date;

import lombok.Data;

/**
 * Beschreibt eine Mahnung aus der Datenbank.
 * 
 * @author Fabian Lipp
 * 
 */
@Data
public class BeitragMahnung {
    private int mahnungId;
    private int rechnungId;
    private Date datum;
    private Date frist;
    private int mahnungArt;
}
