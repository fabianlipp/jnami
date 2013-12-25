package nami.beitrag.db;

import java.util.Collection;

import nami.beitrag.reports.DataAbrechnungHalbjahr;
import nami.connector.Halbjahr;

/**
 * Beschreibt einen Mapper für Datenbankabfragen, auf denen die Reports
 * basieren.
 * 
 * @author Fabian Lipp
 * 
 */
public interface ReportsMapper {
    /**
     * Erstellt die Abrechnung für ein Halbjahr. Diese Abrechnung listet für
     * jedes Mitglied das gesamte Beitragskonto auf.
     * 
     * @param halbjahr
     *            Halbjahr, für das die Abrechnung abgefragt wird
     * @return Liste mit Mitgliederdaten und -buchungen
     */
    Collection<DataAbrechnungHalbjahr> abrechnungHalbjahr(Halbjahr halbjahr);
}
