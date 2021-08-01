package nami.beitrag.db;

import java.util.ArrayList;

import lombok.Getter;
import org.apache.ibatis.annotations.Param;

/**
 * Stellt Datenbankabfragen zur Arbeit mit SEPA-Lastschrift-Mandaten bereit.
 * 
 * @author Fabian Lipp
 * 
 */
public interface MandateMapper {
    /**
     * Liefert den Datensatz eines Mandats.
     * 
     * @param mandatId
     *            ID des Mandats
     * @return kompletter Datensatz des Mandats
     */
    BeitragSepaMandat getMandat(int mandatId);

    /**
     * Fügt ein Mandat in die Datenbank ein.
     * 
     * @param mandat
     *            Mandats-Datensatz, der in die Datenbank geschrieben werden
     *            soll. Die <tt>mandatId</tt> des neu eingefügten Mandats ist
     *            anschließend im Objekt gespeichert, das als Parameter
     *            übergeben wurde.
     */
    void insertMandat(BeitragSepaMandat mandat);

    /**
     * Aktualisiert ein Mandat in der Datenbank. Der Datensatz wird anhand der
     * <tt>mandatID</tt> identifiziert.
     * 
     * @param mandat
     *            Mandats-Datensatz, der in die Datenbank geschrieben werden
     *            soll
     */
    void updateMandat(BeitragSepaMandat mandat);

    /**
     * Fügt ein Mitglied zu einem Mandat hinzu (d. h. das Mandat kann verwendet
     * werden, um den Mitgliedsbeitrag des Mitglieds einzuziehen). Das aktive
     * Mandat wird dabei <i>nicht</i> gesetzt, das heißt das Mandat wird noch
     * nicht verwendet.
     * 
     * @param mandatId
     *            ID des Mandats
     * @param mitgliedId
     *            ID des Mitglieds
     */
    void addMitgliedForMandat(@Param("mandatId") int mandatId,
            @Param("mitgliedId") int mitgliedId);

    /**
     * Setzt das aktive Mandat für ein Mitglieds (d. h. das Mandat das verwendet
     * wird, um den Mitgliedsbeitrag des Mitglieds einzuziehen
     * 
     * @param mandatId
     *            ID des Mandats. Wenn hier <tt>null</tt> übergeben wird, dann
     *            hat das Mitglied anschließend kein aktives Mandat mehr, d. h.
     *            die Beiträge können nicht mehr durch eine Lastschrift
     *            eingezogen werden, sondern müssen per Rechnung angefordert
     *            werden.
     * @param mitgliedId
     *            ID des Mitglieds
     */
    void setAktivesMandat(@Param("mandatId") Integer mandatId,
            @Param("mitgliedId") int mitgliedId);

    /**
     * Liefert alle Mandate, die einem Mitglied zugeordnet sind.
     * 
     * @param mitgliedId
     *            ID des Mitglieds
     * @return alle Mandate (gültig und ungültig), von denen Beiträge für das
     *         Mitglied eingezogen werden können
     */
    ArrayList<BeitragSepaMandat> findMandateByMitglied(int mitgliedId);

    /**
     * Liefert alle Mitglieder, die einem Mandat zugeordnet sind.
     * 
     * @param mandatId
     *            ID des Mandats
     * @return alle Mitglieder, deren Beiträge mit diesem Mandat eingezogen
     *         werden können
     */
    ArrayList<BeitragMitglied> findMitgliederByMandat(int mandatId);

    /**
     * Ergebnis-Datentyp, der Kombinationen aus einer Lastschrift und der
     * zugehörigen Sammellastschrift aufnimmt.
     */
    @Getter
    class DataLastschriftSammellastschrift {
        private BeitragLastschrift lastschrift;
        private BeitragSammelLastschrift sammelLastschrift;
    }

    /**
     * Liefert alle Lastschriften, die für ein Mandat angelegt wurden.
     *
     * @param mandatId
     *            ID des Mandats
     * @return alle Lastschriften, die mit diesem Mandat angelegt wurden.
     */
    ArrayList<MandateMapper.DataLastschriftSammellastschrift> findLastschriftenByMandat(int mandatId);

    /**
     * Gibt an, ob ein bestimmtes Mandat schon einmal benutzt wurde. Benutzt
     * bedeutet hierbei, dass es in einer Sammellastschrift verwendet wurde, die
     * auch als ausgeführt markiert ist. Die Rückgabe kann also verwendet werden
     * um den SEPA-Sequenztyp (FRST oder RCUR) zu bestimmen.
     * 
     * @param mandatId
     *            ID des Mandats
     * @return <tt>true</tt>, falls bereits eine Erstlastschrift von diesem
     *         Mandat gezogen wurde
     */
    boolean isMandatUsed(int mandatId);
}
