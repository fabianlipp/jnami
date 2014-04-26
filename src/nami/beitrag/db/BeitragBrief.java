package nami.beitrag.db;

import java.util.Date;

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
    private Date kompiliert;
}
