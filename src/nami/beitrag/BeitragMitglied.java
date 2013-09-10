package nami.beitrag;

import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;
import nami.connector.Beitragsart;
import nami.connector.Mitgliedstyp;
import nami.connector.namitypes.NamiMitglied;

/**
 * Beschreibt ein Mitglied mit allen Daten, die für die Beitragsverwaltung
 * notwendig sind.
 * 
 * @author Fabian Lipp
 * 
 */
@Data
@NoArgsConstructor
public class BeitragMitglied {
    private int mitgliedId;
    private int mitgliedsnummer;
    private String nachname;
    private String vorname;
    private Mitgliedstyp status;
    private Beitragsart beitragsart;
    private String strasse;
    private String plz;
    private String ort;
    private int version;
    private Zahlungsart zahlungsart;
    private String mandatIBAN;
    private String mandatBIC;
    private Date mandatDatum;
    private String mandatKontoinhaber;
    private String mandatStrasse;
    private String mandatPlz;
    private String mandatOrt;
    private boolean deleted;

    /**
     * Aktualisiert die Felder mit einem übergebenen Mitglieds-Datensatz aus
     * NaMi. Die Mitglieds-ID wird dabei nicht aktualisiert.
     * 
     * @param namiMgl
     *            der Mitglieds-Datensatz aus NaMi.
     */
    public void updateFromNami(NamiMitglied namiMgl) {
        if (mitgliedId != namiMgl.getId()) {
            throw new IllegalArgumentException("MitgliedIds are not equal");
        }
        mitgliedsnummer = namiMgl.getMitgliedsnummer();
        nachname = namiMgl.getNachname();
        vorname = namiMgl.getVorname();
        status = namiMgl.getMitgliedstyp();
        beitragsart = namiMgl.getBeitragsart();
        strasse = namiMgl.getStrasse();
        plz = namiMgl.getPlz();
        ort = namiMgl.getOrt();
        version = namiMgl.getVersion();
    }

    /**
     * Erzeugt ein neues Mitglied aus einem übergebenen Mitglieds-Datensatz aus
     * NaMi. Die Felder, die in NaMi nicht vorhanden sind, werden mit ihren
     * Standardwerten initialisiert.
     * 
     * @param namiMgl
     *            der Mitglieds-Datensatz aus NaMi
     */
    public BeitragMitglied(NamiMitglied namiMgl) {
        mitgliedId = namiMgl.getId();
        updateFromNami(namiMgl);
        deleted = false;
    }
}
