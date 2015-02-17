package nami.beitrag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import nami.beitrag.db.BeitragBuchung;
import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.BeitragMitglied;
import nami.beitrag.db.BeitragZeitraum;
import nami.connector.Halbjahr;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * Importiert eine Rechnung von Bundesebene, die vorher von einem Skript ins
 * CSV-Format umgewandelt wurde. Die CSV ist durch ';' getrennt und enthält
 * folgende Spalten:
 * <ol>
 * <li>Mitgliedsnummer</li>
 * <li>Buchungstext</li>
 * <li>nicht benutzt</li>
 * <li>Beginn des Zeitraums, für den der Beitrag gilt (im Format DD.MM.YY)</li>
 * <li>Betrag (mit Komma als Dezimaltrennzeichen)</li>
 * <li>Buchungs-ID aus NaMi</li>
 * </ol>
 * 
 * @author Fabian Lipp
 * 
 */
public class RechnungCsvImport {
    private SqlSessionFactory sqlSessionFactory;
    private DecimalFormat germanDecimalFormat;
    private static Logger log = Logger.getLogger(RechnungCsvImport.class
            .getName());

    /**
     * Initialisiert einen neuen CSV-Importer.
     * 
     * @param sqlSessionFactory
     *            Verbindung zur Datenbank
     */
    public RechnungCsvImport(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.GERMAN);
        nf.setGroupingUsed(false);
        germanDecimalFormat = (DecimalFormat) nf;
        germanDecimalFormat.setParseBigDecimal(true);
    }

    /**
     * Importiert eine CSV-Rechnungs-Tabelle in die Datenbank.
     * 
     * @param csvFile
     *            zu importierende Datei
     * @param rechnungsNummer
     *            Rechnungsnummer, die für jeden der Einträge in die Datenbank
     *            geschrieben wird
     * @param rechnungsdatum
     *            Datum der Rechnung
     */
    public void importCsv(File csvFile, String rechnungsNummer,
            Date rechnungsdatum) {
        SqlSession session = sqlSessionFactory.openSession();
        BufferedReader br = null;
        try {
            try {
                BeitragMapper mapper = session.getMapper(BeitragMapper.class);
                br = new BufferedReader(new FileReader(csvFile));
                String line;
                int lineNumber = 0;
                while ((line = br.readLine()) != null) {
                    lineNumber++;
                    String[] split = line.split(";");
                    if (split.length != 6) {
                        throw new IllegalArgumentException(
                                "Ungültiges Format für CSV-Datei in Zeile "
                                        + lineNumber);
                    }
                    int mitgliedsNummer = Integer.parseInt(split[0]);
                    String kommentar = split[1];
                    String datumBeginn = split[3];
                    int jahr = Integer.parseInt("20"
                            + datumBeginn.substring(6, 8));
                    int haelfte;
                    if (datumBeginn.substring(3, 5).equals("01")) {
                        haelfte = 1;
                    } else if (datumBeginn.substring(3, 5).equals("07")) {
                        haelfte = 2;
                    } else {
                        throw new IllegalArgumentException(
                                "Kann Datum keinem Halbjahr zuordnen: "
                                        + datumBeginn);
                    }
                    Halbjahr halbjahr = new Halbjahr(haelfte, jahr);
                    BigDecimal betrag = (BigDecimal) germanDecimalFormat
                            .parse(split[4]);
                    int namiBuchungId = Integer.parseInt(split[5]);

                    BeitragBuchung buchung = mapper
                            .getBuchungByNamiId(namiBuchungId);
                    if (buchung == null) {
                        // Buchung existiert noch nicht lokal -> einfügen

                        BeitragMitglied mitglied = mapper
                                .getMitgliedByNummer(mitgliedsNummer);
                        if (mitglied == null) {
                            // Mitglied nicht gefunden
                            throw new IllegalArgumentException(
                                    "Unbekannte Mitgliedsnummer in CSV: "
                                            + mitgliedsNummer);
                        }

                        buchung = new BeitragBuchung();
                        buchung.setMitgliedId(mitglied.getMitgliedId());
                        buchung.setNamiBuchungId(namiBuchungId);
                        buchung.setRechnungsNummer(rechnungsNummer);
                        buchung.setTyp(Buchungstyp.RECHNUNG_BUNDESEBENE);
                        buchung.setDatum(rechnungsdatum);
                        buchung.setBetrag(betrag.negate());
                        buchung.setHalbjahr(halbjahr);
                        buchung.setVorausberechnung(false);
                        buchung.setKommentar(kommentar);
                        buchung.setStorniert(false);

                        log.log(Level.INFO,
                                "Inserting BuchungID {0,number,#} for MitgliedID "
                                        + "{1,number,#} into local database",
                                new Object[] { namiBuchungId,
                                        mitglied.getMitgliedId() });

                        // Zeitraum einfügen, falls noch nicht vorhanden
                        if (mapper.getZeitraum(buchung.getHalbjahr()) == null) {
                            BeitragZeitraum zeitr = new BeitragZeitraum(
                                    buchung.getHalbjahr());
                            mapper.insertZeitraum(zeitr);
                        }

                        // Lösche vorhandene Vorausberechnungen
                        mapper.storniereVorausberechnung(halbjahr,
                                mitglied.getMitgliedId());

                        mapper.insertBuchung(buchung);
                    }
                }
                session.commit();
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                session.close();
                if (br != null) {
                    br.close();
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
