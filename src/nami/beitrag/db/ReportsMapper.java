package nami.beitrag.db;

import java.math.BigDecimal;
import java.util.Collection;

import lombok.Getter;
import nami.beitrag.Buchungstyp;
import nami.beitrag.reports.DataAbrechnungHalbjahr;
import nami.connector.Halbjahr;
import org.apache.ibatis.annotations.Param;

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
     * @param ausgeglichen
     *            Zeige auch Mitglieder mit ausgeglichenen Beitragskonten
     * @return Liste mit Mitgliederdaten und -buchungen
     */
    Collection<DataAbrechnungHalbjahr> abrechnungHalbjahr(@Param("halbjahr") Halbjahr halbjahr,
                                                          @Param("ausgeglichen") boolean ausgeglichen);

    @Getter
    class DataAbrechnungNachTypen {
        private Buchungstyp typ;
        private boolean vorausberechnung;
        private BigDecimal betrag;
    }

    Collection<DataAbrechnungNachTypen> abrechnungNachTypenHalbjahr(@Param("halbjahr") Halbjahr halbjahr);

    /**
     * Listet alle aktiven (beitragspflichtigen) Mitglieder auf, denen kein
     * gültiges SEPA-Mandat zugeordnet ist.
     * 
     * @return Liste der Mitglieder ohne SEPA-Mandat
     */
    Collection<BeitragMitglied> mitgliederOhneSepaMandat();
}
