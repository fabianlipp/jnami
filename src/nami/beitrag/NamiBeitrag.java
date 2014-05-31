package nami.beitrag;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import nami.beitrag.db.BeitragBuchung;
import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.BeitragMitglied;
import nami.beitrag.db.BeitragZeitraum;
import nami.connector.Beitragsart;
import nami.connector.Halbjahr;
import nami.connector.Mitgliedstyp;
import nami.connector.NamiConnector;
import nami.connector.exception.NamiApiException;
import nami.connector.namitypes.NamiBeitragszahlung;
import nami.connector.namitypes.NamiMitgliedListElement;
import nami.connector.namitypes.NamiSearchedValues;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * Liest Beitragsdaten aus NaMi aus, schreibt sie in eine lokale Datenbank und
 * verarbeitet diese Daten.
 * 
 * @author Fabian Lipp
 * 
 */
public final class NamiBeitrag {
    private SqlSessionFactory sqlSessionFactory;
    private String gruppierungsNummer;
    private Map<Beitragsart, BigDecimal> beitragssaetze;
    private NamiConnector con;
    private static Logger log = Logger.getLogger(NamiBeitrag.class.getName());

    /**
     * Erzeugt ein neues Objekt.
     * 
     * @param sqlSessionFactory
     *            Verbindung zur Datenbank
     * @param conf
     *            Konfiguration des Nami-Beitrags-Tools
     * @param con
     *            Verbindung zum NaMi-Server
     */
    public NamiBeitrag(SqlSessionFactory sqlSessionFactory,
            NamiBeitragConfiguration conf, NamiConnector con) {
        this.sqlSessionFactory = sqlSessionFactory;

        gruppierungsNummer = conf.getGruppierungsnummer();
        beitragssaetze = conf.getBeitragssaetze();

        this.con = con;
    }

    /**
     * Bringt die lokale Mitgliederdatenbank auf den aktuellen Stand. Dazu
     * werden folgende Schritte ausgeführt:
     * <ul>
     * <li>in NaMi neu angelegte Mitglieder in die lokale Datenbank einfügen</li>
     * <li>in NaMi geänderte Mitglieder (erkennbar an der Versionsnummer) in der
     * lokalen Datenbank aktualisieren</li>
     * <li>in NaMi gelöschte Mitglieder in der lokalen Datenbank als gelöscht
     * markieren</li>
     * </ul>
     * Es werden keine Daten aus der lokalen Datenbank nach NaMi übertragen.
     * 
     * @throws IOException
     *             IOException
     * @throws NamiApiException
     *             Fehler bei einer Anfrage an NaMi
     */
    public void syncMitglieder() throws NamiApiException, IOException {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            BeitragMapper mapper = session.getMapper(BeitragMapper.class);

            Set<Integer> localMglIds = mapper.getMitgliedIds();

            con.namiLogin();
            Collection<NamiMitgliedListElement> mitgliederFromNami = NamiSearchedValues
                    .withStammgruppierung(gruppierungsNummer)
                    .getAllResults(con);

            // DEBUG: nehme nur 10 Mitglieder aus Nami
            Collection<NamiMitgliedListElement> tmp = new LinkedList<>();
            Iterator<NamiMitgliedListElement> iter = mitgliederFromNami
                    .iterator();
            for (int i = 0; i <= 9; i++) {
                if (iter.hasNext()) {
                    tmp.add(iter.next());
                }
            }
            mitgliederFromNami = tmp;

            for (NamiMitgliedListElement namiMgl : mitgliederFromNami) {
                BeitragMitglied beitMgl = mapper.getMitglied(namiMgl.getId());
                if (beitMgl != null) {
                    // Mitglied existiert bereits lokal
                    if (namiMgl.getVersion() > beitMgl.getVersion()) {
                        // in Nami geupdated
                        beitMgl.updateFromNami(namiMgl.getFullData(con));
                        log.log(Level.INFO,
                                "Updating MitgliedID {0,number,#} in local database",
                                beitMgl.getMitgliedId());
                        mapper.updateMitglied(beitMgl);
                    }
                } else {
                    // Mitglied existiert noch nicht lokal
                    beitMgl = new BeitragMitglied(namiMgl.getFullData(con));
                    log.log(Level.INFO,
                            "Inserting MitgliedID {0,number,#} into local database",
                            beitMgl.getMitgliedId());
                    mapper.insertMitglied(beitMgl);
                }
                localMglIds.remove(namiMgl.getId());
            }

            // IDs, die jetzt noch in localMglIds sind, existieren nur lokal
            // aber nicht in Nami
            for (int mglId : localMglIds) {
                if (!mapper.isDeleted(mglId)) {
                    log.log(Level.INFO,
                            "MitgliedID {0,number,#} does not exist in NaMi; "
                                    + "marking as deleted in local database",
                            mglId);
                    mapper.setDeleted(mglId);
                }
            }

            session.commit();
        } finally {
            session.close();
        }
    }

    /**
     * Holt die Beitragszahlungen aller Mitglieder ab und fügt sie in die lokale
     * Datenbank ein, falls sie noch nicht vorhanden sind.
     * 
     * @throws IOException
     *             IOException
     * @throws NamiApiException
     *             Fehler bei einer Anfrage an NaMi
     */
    public void fetchBeitragszahlungen() throws NamiApiException, IOException {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            BeitragMapper mapper = session.getMapper(BeitragMapper.class);
            con.namiLogin();

            Set<Integer> localMglIds = mapper.getMitgliedIds();
            for (int mglId : localMglIds) {
                fetchBeitragszahlungen(mglId, mapper);
            }

            session.commit();
        } finally {
            session.close();
        }
    }

    /**
     * Holt die Beitragszahlungen eines Mitglieds ab und fügt die in die lokale
     * Datenbank ein, falls sie noch nicht vorhanden sind.
     * 
     * @param mitgliedId
     *            ID des Mitglieds
     * @throws IOException
     *             IOException
     * @throws NamiApiException
     *             Fehler bei einer Anfrage an NaMi
     */
    public void fetchBeitragszahlungen(int mitgliedId) throws NamiApiException,
            IOException {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            BeitragMapper mapper = session.getMapper(BeitragMapper.class);
            con.namiLogin();
            fetchBeitragszahlungen(mitgliedId, mapper);
            session.commit();
        } finally {
            session.close();
        }
    }

    private void fetchBeitragszahlungen(int mitgliedId, BeitragMapper mapper)
            throws NamiApiException, IOException {
        Collection<NamiBeitragszahlung> zahlungen = NamiBeitragszahlung
                .getBeitragszahlungen(con, mitgliedId);
        for (NamiBeitragszahlung zahlung : zahlungen) {
            BeitragBuchung buchung = mapper.getBuchungByNamiId(zahlung.getId());
            if (buchung == null) {
                // Buchung existiert noch nicht lokal -> einfügen
                buchung = new BeitragBuchung(mitgliedId, zahlung);
                log.log(Level.INFO,
                        "Inserting BuchungID {0,number,#} for MitgliedID "
                                + "{1,number,#} into local database",
                        new Object[] { zahlung.getId(), mitgliedId });

                // Zeitraum einfügen, falls noch nicht vorhanden
                if (mapper.getZeitraum(buchung.getHalbjahr()) == null) {
                    BeitragZeitraum zeitr = new BeitragZeitraum(
                            buchung.getHalbjahr());
                    mapper.insertZeitraum(zeitr);
                }

                // Lösche vorhandene Vorausberechnungen
                mapper.deleteVorausberechnung(zahlung.getZeitraum(), mitgliedId);

                mapper.insertBuchung(buchung);
            }
        }
    }

    /**
     * Fügt die vorausberechneten Beitragszahlungen für alle Mitglieder ein,
     * falls für das gegebene Halbjahr noch keine "echte" Buchung von
     * Bundesebene existiert.
     * 
     * @param halbjahr
     *            Halbjahr, für das der Beitrag vorausberechnet werden soll
     */
    public void vorausberechnung(Halbjahr halbjahr) {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            BeitragMapper mapper = session.getMapper(BeitragMapper.class);
            Set<Integer> localMglIds = mapper.getMitgliedIds();
            for (int mglId : localMglIds) {
                vorausberechnung(halbjahr, mglId, mapper);
            }
            session.commit();
        } finally {
            session.close();
        }
    }

    /**
     * Fügt die vorausberechnete Beitragszahlung für ein Mitglied ein, falls für
     * das gegebene Halbjahr noch keine "echte" Buchung von Bundesebene
     * existiert.
     * 
     * @param halbjahr
     *            Halbjahr, für das der Beitrag vorausberechnet werden soll
     * @param mitgliedId
     *            ID des Mitglieds
     */
    public void vorausberechnung(Halbjahr halbjahr, int mitgliedId) {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            BeitragMapper mapper = session.getMapper(BeitragMapper.class);
            vorausberechnung(halbjahr, mitgliedId, mapper);
            session.commit();
        } finally {
            session.close();
        }
    }

    private void vorausberechnung(Halbjahr halbjahr, int mitgliedId,
            BeitragMapper mapper) {
        BeitragMitglied mgl = mapper.getMitglied(mitgliedId);

        // Lösche vorhandene Vorausberechnungen
        mapper.deleteVorausberechnung(halbjahr, mitgliedId);

        if (mapper.checkForRechnungBundesebene(halbjahr, mitgliedId) > 0) {
            // es existiert bereits eine Rechnungs-Buchung für dieses Halbjahr
            // => es erfolgt keine Vorausberechnung
            return;
        }

        if (mgl.getStatus() != Mitgliedstyp.MITGLIED) {
            // kein reguläres Mitglied
            // => es ist kein Beitrag zu bezahlen
            return;
        }

        Date eintrittsdatum = mgl.getEintrittsdatum();
        if (eintrittsdatum == null) {
            // keine Vorausberechnung, wenn kein Eintrittsdatum angegeben ist
            return;
        }

        Halbjahr eintrittsHj = new Halbjahr(eintrittsdatum);
        if (eintrittsHj.compareTo(halbjahr) > 0) {
            // Eintritt nach diesem Halbjahr
            return;
        } else if (eintrittsHj.equals(halbjahr)) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(eintrittsdatum);
            int month = cal.get(Calendar.MONTH);
            if (month == Calendar.MAY || month == Calendar.JUNE
                    || month == Calendar.NOVEMBER || month == Calendar.DECEMBER) {
                // Eintritt in letzten beiden Monaten dieses Halbjahr
                return;
            }
        }
        // Aufgrund des Eintrittsdatums muss ein Beitrag bezahlt werden

        // Stelle Buchung zusammen
        BeitragBuchung buchung = new BeitragBuchung();
        buchung.setMitgliedId(mitgliedId);
        buchung.setNamiBuchungId(null);
        buchung.setRechnungsNummer(null);
        buchung.setTyp(Buchungstyp.RECHNUNG_BUNDESEBENE);
        buchung.setDatum(new Date());
        // Betrag negativ, da Belastung
        buchung.setBetrag(beitragssaetze.get(mgl.getBeitragsart()).negate());
        buchung.setHalbjahr(halbjahr);
        buchung.setVorausberechnung(true);
        buchung.setKommentar(null);

        // Zeitraum einfügen, falls noch nicht vorhanden
        if (mapper.getZeitraum(halbjahr) == null) {
            BeitragZeitraum zeitr = new BeitragZeitraum(halbjahr);
            mapper.insertZeitraum(zeitr);
        }

        mapper.insertBuchung(buchung);
    }

    /**
     * Liefert die verwendete Verbindung zur SQL-Datenbank.
     * 
     * @return Factory zum Erzeugen von Datenbank-Verbindungen
     */
    public SqlSessionFactory getSessionFactory() {
        return sqlSessionFactory;
    }

}
