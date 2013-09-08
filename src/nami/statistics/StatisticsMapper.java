package nami.statistics;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import nami.statistics.StatisticsDatabase.Run;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;

/**
 * Beschreibt einen Mapper für die Datenbankabfragen, die von NamiStatistics
 * verwendet werden.
 * 
 * @author Fabian Lipp
 * 
 */
public interface StatisticsMapper {
    /**
     * Erzeuge Tabelle Gruppierungen.
     * 
     * @return Anzahl geänderte Zeilen
     */
    int createTableGruppierung();

    /**
     * Erzeuge Tabelle statisticsGruppen.
     * 
     * @return Anzahl geänderte Zeilen
     */
    int createTableStatisticsGruppe();

    /**
     * Erzeuge Tabelle statisticsRun.
     * 
     * @return Anzahl geänderte Zeilen
     */
    int createTableStatisticsRun();

    /**
     * Erzeuge Tabelle statisticsData.
     * 
     * @return Anzahl geänderte Zeilen
     */
    int createTableStatisticsData();

    /**
     * Gibt die Anzahl der Gruppierungen in der Datenbank mit einer übergebenen
     * ID zurück.
     * 
     * @param id
     *            gesuchte ID
     * @return 0 oder 1, je nachdem ob die Gruppierung bereits in der Datenbank
     *         existiert
     */
    int checkForGruppierung(String id);

    /**
     * Fügt eine Gruppierung in die Datenbank ein.
     * 
     * @param gruppierungId
     *            ID der Gruppierung
     * @param descriptor
     *            Bezeichnung der Gruppierung (Stammesname)
     * @param dioezeseId
     *            ID der zugehörigen Diözese
     * @param bezirkId
     *            ID des zugehörigen Bezirks
     * @return Anzahl geänderte Zeilen
     */
    int insertGruppierung(@Param("gruppierungId") String gruppierungId,
            @Param("descriptor") String descriptor,
            @Param("dioezeseId") String dioezeseId,
            @Param("bezirkId") String bezirkId);

    /**
     * Gibt die Anzahl der Gruppen in der Datenbank mit einer übergebenen ID
     * zurück.
     * 
     * @param id
     *            gesuchte ID
     * @return 0 oder 1, je nachdem ob die Gruppe bereits in der Datenbank
     *         existiert
     */
    int checkForGruppe(int id);

    /**
     * Fügt eine Gruppe in die Datenbank ein.
     * 
     * @param id
     *            ID der Gruppe
     * @param bezeichnung
     *            Bezeichnung der Gruppe
     * @return Anzahl geänderte Zeilen
     */
    int insertGruppe(@Param("id") int id,
            @Param("bezeichnung") String bezeichnung);

    /**
     * Fügt einen neuen Run in die Datenbank ein.
     * 
     * @param param
     *            wenn eine leere Map übergeben wird, ist nach der Ausführung im
     *            Feld <tt>runId</tt> die neu eingefügte ID gespeichert
     * @return Anzahl geänderte Zeilen
     */
    int newRun(Map<String, Object> param);

    /**
     * Fügt eine Personenzahl in die Datenbank ein.
     * 
     * @param gruppierungId
     *            ID der Gruppierung
     * @param gruppeId
     *            ID der Gruppe
     * @param runId
     *            ID des Runs
     * @param anzahl
     *            Anzahl Personen in der Gruppe
     * @return Anzahl geänderte Zeilen
     */
    int insertAnzahl(@Param("gruppierungId") String gruppierungId,
            @Param("gruppeId") int gruppeId, @Param("runId") long runId,
            @Param("anzahl") int anzahl);

    /**
     * Liefert die in der Datenbank vorhandenen Runs.
     * 
     * @return Liste der Runs
     */
    List<Run> getRuns();

    /**
     * Liefert die ID des zuletzt durchgeführten Runs.
     * 
     * @return ID des neuesten Runs; <tt>null</tt>, falls noch keiner in
     *         Datenbank vorhanden ist
     */
    Integer getLatestRunId();

    /**
     * Fragt die Statistiken für alle Gruppierungen in einem bestimmten Run ab
     * und übergibt die Ergebnisse an einen <tt>ResultHandler</tt>.
     * 
     * @param runId
     *            ID des Runs
     * @param cumulate
     *            <tt>true</tt>, falls bei Gruppierungen die Anzahlen für alle
     *            untergeordneten Gruppierungen kummuliert werden sollen
     * @param gruppen
     *            vorhandene Gruppen
     * @param handler
     *            Handler, an den die Ergebnis-Zeilen übergeben werden sollen
     */
    void getStatsAllGruppierungen(@Param("runId") int runId,
            @Param("cumulate") boolean cumulate,
            @Param("gruppen") Collection<Gruppe> gruppen, ResultHandler handler);

    /**
     * Fragt die Statistiken für eine Gruppierung im zeitlichen Verlauf ab und
     * übergibt die Ergebnisse an einen <tt>ResultHandler</tt>.
     * 
     * @param gruppierungId
     *            Gruppierung, für die der Verlauf ausgegeben werden soll
     * @param cumulate
     *            <tt>true</tt>, falls bei Gruppierungen die Anzahlen für alle
     *            untergeordneten Gruppierungen kummuliert werden sollen
     * @param gruppen
     *            vorhandene Gruppen
     * @param handler
     *            Handler, an den die Ergebnis-Zeilen übergeben werden sollen
     */
    void getHistory(@Param("id") String gruppierungId,
            @Param("cumulate") boolean cumulate,
            @Param("gruppen") Collection<Gruppe> gruppen, ResultHandler handler);
}
