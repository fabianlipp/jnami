package nami.beitrag.db;

import java.util.Date;

import nami.beitrag.letters.LetterType;
import lombok.Data;

/**
 * Beschreibt einen Datenbankeintrag für einen Brief, der als LaTeX-Quelltext im
 * entsprechenden Verzeichnis erstellt wurde.
 * 
 * @author Fabian Lipp
 * 
 */
@Data
public class BeitragBrief {
    private int briefId;
    private String dateiname;
    private Date datum;
    private LetterType typ;
    private Date kompiliert;
}
