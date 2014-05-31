package nami.beitrag.db;

import java.util.Date;

import lombok.Data;
import nami.beitrag.AbmeldungTyp;

/**
 * Beschreibt einen Datenbankeintrag für eine Abmeldung, die vorgemerkt wurde.
 * 
 * @author Fabian Lipp
 * 
 */
@Data
public class BeitragAbmeldung {
    private int abmeldungId;
    private int mitgliedId;
    private Date datum;
    private Date faelligkeit;
    private AbmeldungTyp typ;
    private Date namiEingetragen;
}
