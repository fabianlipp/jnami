package nami.beitrag.db;

import java.util.Collection;

import nami.connector.Halbjahr;

/**
 * Stellt Datenbankabfragen zur Arbeit mit Rechnungen bereit.
 * 
 * @author Fabian Lipp
 * 
 */
public interface RechnungenMapper {
    /**
     * Liefert alle Mitglieder, für die in einem bestimmten Halbjahr offene
     * Forderungen bestehen.
     * 
     * @param halbjahr
     *            Halbjahr, für das Buchungen abgefragt werden
     * @return Mitglieder, die die Bedingung erfüllen
     */
    Collection<DataMitgliederForderungen> mitgliederOffeneForderungen(
            Halbjahr halbjahr);
}
