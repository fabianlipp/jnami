package nami.beitrag.db;

import java.util.Date;

import lombok.Data;

/**
 * Beschreibt ein SEPA-Lastschrift-Mandat aus der Datenbank.
 * 
 * @author Fabian Lipp
 * 
 */
@Data
public class BeitragSepaMandat {
    private int mandatId;
    private String iban;
    private String bic;
    private Date datum;
    private String kontoinhaber;
    private String strasse;
    private String plz;
    private String ort;
    private String email;
    private boolean gueltig;
}
