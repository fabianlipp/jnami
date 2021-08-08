package nami.beitrag.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import nami.beitrag.NamiBeitrag;
import nami.beitrag.NamiBeitragConfiguration;
import nami.beitrag.RechnungCsvImport;
import nami.beitrag.letters.LetterDirectory;
import nami.beitrag.letters.LetterGenerator;
import nami.beitrag.reports.ReportViewer;
import nami.connector.Halbjahr;
import nami.connector.exception.NamiApiException;
import net.miginfocom.swing.MigLayout;

/**
 * Stellt das Hauptfenster der GUI dar.
 * 
 * @author Fabian Lipp
 * 
 */
// TODO: Lange Strings externalisieren
public class MainWindow extends JFrame {
    private static final long serialVersionUID = 7477838944466651902L;

    private static final String MIG_BUTTON_CONSTRAINTS = "wrap,alignx left,aligny top";
    private LetterGenerator letterGenerator;
    private ReportViewer reportViewer;

    /**
     * Erzeugt das Hauptfenster.
     * 
     * @param namiBeitrag
     *            Objekt für die Beitragslogik (enthält Zugriff auf Datenbank
     *            und NaMi)
     * @param letterDirectory
     *            Verzeichnis, in dem die erzeugten Briefe abgelegt werden
     * @param conf
     *            Beitrags-Konfiguration
     */
    public MainWindow(final NamiBeitrag namiBeitrag,
            final LetterDirectory letterDirectory,
            final NamiBeitragConfiguration conf) {

        letterGenerator = new LetterGenerator(namiBeitrag.getSessionFactory(),
                letterDirectory, conf);
        reportViewer = new ReportViewer(namiBeitrag.getSessionFactory());

        setTitle("NamiBeitrag");
        getContentPane().setLayout(
                new MigLayout("", "[grow][grow]", "[grow][grow][grow][][][]"));

        /**** NaMi ****/
        JPanel panelNami = new JPanel();
        getContentPane().add(panelNami, "cell 0 0,grow");
        panelNami.setBorder(new TitledBorder("NaMi-Zugriff"));
        panelNami.setLayout(new MigLayout("", "[]", "[][][]"));

        JButton btnSync = new JButton("Mitglieder mit NaMi synchronisieren");
        panelNami.add(btnSync, MIG_BUTTON_CONSTRAINTS);
        btnSync.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    namiBeitrag.syncMitglieder();
                } catch (NamiApiException | IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });

        JButton btnFetch = new JButton("<html>Beitragszahlungen aus NaMi holen"
                + "<br>(von NaMi momentan nicht unterstützt)");
        panelNami.add(btnFetch, MIG_BUTTON_CONSTRAINTS);
        btnFetch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    namiBeitrag.fetchBeitragszahlungen();
                } catch (NamiApiException | IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });

        JButton btnCsvImport = new JButton("Rechnung im CSV-Format einlesen");
        panelNami.add(btnCsvImport, MIG_BUTTON_CONSTRAINTS);
        btnCsvImport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RechnungCsvImport imp = new RechnungCsvImport(namiBeitrag
                        .getSessionFactory());
                RechnungImportDialog diag = new RechnungImportDialog(
                        MainWindow.this);
                diag.setVisible(true);
                if (diag.isAccepted()) {
                    imp.importCsv(diag.getChosenFile(),
                            diag.getRechnungsNummer(), diag.getRechnungsDatum());
                }
            }
        });

        /**** Vorausberechnungen ****/
        JPanel panelVorausberechnungen = new JPanel();
        getContentPane().add(panelVorausberechnungen, "cell 1 0,grow");
        panelVorausberechnungen
                .setBorder(new TitledBorder("Vorausberechnungen"));
        panelVorausberechnungen.setLayout(new MigLayout("", "[]", "[][][]"));

        JButton btnVorausberechnung = new JButton(
                "Vorausberechnung für alle Mitglieder ...");
        panelVorausberechnungen
                .add(btnVorausberechnung, MIG_BUTTON_CONSTRAINTS);
        btnVorausberechnung.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HalbjahrMitgliedSelectDialog halbjahrSel;
                halbjahrSel = new HalbjahrMitgliedSelectDialog(MainWindow.this);
                halbjahrSel.setVisible(true);
                if (halbjahrSel.getChosenHalbjahr() != null) {
                    namiBeitrag.vorausberechnung(halbjahrSel
                            .getChosenHalbjahr());
                }
            }
        });

        JButton btnVorausberechnungUpdate = new JButton(
                "Vorausberechnung aktualisieren ...");
        panelVorausberechnungen.add(btnVorausberechnungUpdate,
                MIG_BUTTON_CONSTRAINTS);
        btnVorausberechnungUpdate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HalbjahrMitgliedSelectDialog halbjahrMglSel;
                halbjahrMglSel = new HalbjahrMitgliedSelectDialog(
                        MainWindow.this, namiBeitrag.getSessionFactory());
                halbjahrMglSel.setVisible(true);
                if (halbjahrMglSel.getChosenHalbjahr() != null
                        && halbjahrMglSel.getChosenMitgliedId() != -1) {
                    namiBeitrag.aktualisiereVorausberechnung(
                            halbjahrMglSel.getChosenHalbjahr(),
                            halbjahrMglSel.getChosenMitgliedId());
                }
            }
        });

        JButton btnVorausberechnungDelete = new JButton(
                "Vorausberechnungen für Halbjahr löschen ...");
        panelVorausberechnungen.add(btnVorausberechnungDelete,
                MIG_BUTTON_CONSTRAINTS);
        btnVorausberechnungDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HalbjahrMitgliedSelectDialog halbjahrSel;
                halbjahrSel = new HalbjahrMitgliedSelectDialog(MainWindow.this);
                halbjahrSel.setVisible(true);
                if (halbjahrSel.getChosenHalbjahr() != null) {
                    namiBeitrag.loescheVorausberechnungen(halbjahrSel
                            .getChosenHalbjahr());
                }
            }
        });

        /**** Buchungen ****/
        JPanel panelBuchungen = new JPanel();
        getContentPane().add(panelBuchungen, "cell 0 1,grow");
        panelBuchungen.setBorder(new TitledBorder(""));
        panelBuchungen.setLayout(new MigLayout("", "[]", "[][][]"));

        JButton btnBeitragskonto = new JButton("Beitragskonto");
        panelBuchungen.add(btnBeitragskonto, MIG_BUTTON_CONSTRAINTS);
        btnBeitragskonto.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MitgliedAnzeigenWindow beitragskontoWin = new MitgliedAnzeigenWindow(
                        namiBeitrag.getSessionFactory());
                beitragskontoWin.setVisible(true);
            }
        });

        JButton btnNeueBuchung = new JButton("Neue Buchung");
        panelBuchungen.add(btnNeueBuchung, MIG_BUTTON_CONSTRAINTS);
        btnNeueBuchung.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BuchungDialog diag = new BuchungDialog(namiBeitrag
                        .getSessionFactory());
                diag.setVisible(true);
            }
        });

        /**** Mandate ****/
        JPanel panelMandate = new JPanel();
        getContentPane().add(panelMandate, "cell 1 1,grow");
        panelMandate.setBorder(new TitledBorder(""));
        panelMandate.setLayout(new MigLayout("", "[]", "[][]"));

        JButton btnMandateErfassen = new JButton("Mandat erfassen");
        panelMandate.add(btnMandateErfassen, MIG_BUTTON_CONSTRAINTS);
        btnMandateErfassen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame win = new MandatErstellenWindow(namiBeitrag
                        .getSessionFactory());
                win.setVisible(true);
            }
        });

        JButton btnMandateVerwalten = new JButton("Mandate verwalten");
        panelMandate.add(btnMandateVerwalten, MIG_BUTTON_CONSTRAINTS);
        btnMandateVerwalten.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame win = new MandatVerwaltenWindow(namiBeitrag
                        .getSessionFactory());
                win.setVisible(true);
            }
        });

        JButton btnMitgliederOhneSepa = new JButton(
                "Mitglieder ohne gültiges SEPA-Mandat anzeigen");
        panelMandate.add(btnMitgliederOhneSepa, MIG_BUTTON_CONSTRAINTS);
        btnMitgliederOhneSepa.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reportViewer.viewMitgliederOhneSepaMandat();
            }
        });

        /**** Rechnungen ****/
        JPanel panelRechnungen = new JPanel();
        getContentPane().add(panelRechnungen, "cell 0 2,grow");
        panelRechnungen.setBorder(new TitledBorder(""));
        panelRechnungen.setLayout(new MigLayout("", "[]", "[][]"));

        JButton btnRechnungenErstellen = new JButton("Rechnungen erstellen");
        panelRechnungen.add(btnRechnungenErstellen, MIG_BUTTON_CONSTRAINTS);
        btnRechnungenErstellen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RechnungenErstellenWindow win = new RechnungenErstellenWindow(
                        namiBeitrag.getSessionFactory(), letterGenerator);
                win.setVisible(true);
            }
        });

        JButton btnRechnungenVerwalten = new JButton("Rechnungen verwalten");
        panelRechnungen.add(btnRechnungenVerwalten, MIG_BUTTON_CONSTRAINTS);
        btnRechnungenVerwalten.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame win = new RechnungenVerwaltenWindow(namiBeitrag
                        .getSessionFactory(), letterGenerator);
                win.setVisible(true);
            }
        });

        /**** Lastschriften ****/
        JPanel panelLastschriften = new JPanel();
        getContentPane().add(panelLastschriften, "cell 1 2,grow");
        panelLastschriften.setBorder(new TitledBorder(""));
        panelLastschriften.setLayout(new MigLayout("", "[]", "[][]"));

        JButton btnLastschriftenErstellen = new JButton(
                "Lastschriften erstellen");
        panelLastschriften.add(btnLastschriftenErstellen,
                MIG_BUTTON_CONSTRAINTS);
        btnLastschriftenErstellen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame win = new LastschriftErstellenWindow(namiBeitrag
                        .getSessionFactory());
                win.setVisible(true);
            }
        });

        JButton btnLastschriftenVerwalten = new JButton(
                "Lastschriften verwalten");
        panelLastschriften.add(btnLastschriftenVerwalten,
                MIG_BUTTON_CONSTRAINTS);
        btnLastschriftenVerwalten.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame win = new LastschriftVerwaltenWindow(namiBeitrag
                        .getSessionFactory(), letterGenerator, conf);
                win.setVisible(true);
            }
        });

        /**** Briefe verwalten ****/
        JPanel panelBriefeVerwalten = new JPanel();
        getContentPane().add(panelBriefeVerwalten, "cell 0 3,grow");
        panelBriefeVerwalten.setBorder(new TitledBorder(""));
        panelBriefeVerwalten.setLayout(new MigLayout("", "[]", "[]"));
        JButton btnBriefeVerwalten = new JButton("Briefe verwalten");
        panelBriefeVerwalten.add(btnBriefeVerwalten, MIG_BUTTON_CONSTRAINTS);
        btnBriefeVerwalten.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame win = new BriefeWindow(namiBeitrag.getSessionFactory(),
                        letterDirectory, conf);
                win.setVisible(true);
            }
        });

        /**** Abmeldungen ****/
        JPanel panelAbmeldungen = new JPanel();
        getContentPane().add(panelAbmeldungen, "cell 1 3,grow");
        panelAbmeldungen.setBorder(new TitledBorder(""));
        panelAbmeldungen.setLayout(new MigLayout("", "[]", "[][]"));

        JButton btnAbmeldungVormerken = new JButton("Abmeldung vormerken");
        panelAbmeldungen.add(btnAbmeldungVormerken, MIG_BUTTON_CONSTRAINTS);
        btnAbmeldungVormerken.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame win = new AbmeldungErstellenWindow(namiBeitrag
                        .getSessionFactory());
                win.setVisible(true);
            }
        });

        JButton btnAbmeldungenVerwalten = new JButton("Abmeldungen verwalten");
        panelAbmeldungen.add(btnAbmeldungenVerwalten, MIG_BUTTON_CONSTRAINTS);
        btnAbmeldungenVerwalten.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame win = new AbmeldungenAnzeigenWindow(namiBeitrag
                        .getSessionFactory());
                win.setVisible(true);
            }
        });

        /**** Auswertungen ****/
        JPanel panelAuswertungen = new JPanel();
        getContentPane().add(panelAuswertungen, "cell 0 4,grow");
        panelAuswertungen.setBorder(new TitledBorder("Auswertungen"));
        panelAuswertungen.setLayout(new MigLayout("", "[]", "[][]"));

        JButton btnRepAbrechnungHalbjahr = new JButton("Abrechnung Halbjahr");
        panelAuswertungen.add(btnRepAbrechnungHalbjahr, MIG_BUTTON_CONSTRAINTS);
        btnRepAbrechnungHalbjahr.addActionListener(e -> {
            HalbjahrMitgliedSelectDialog halbjahrSel;
            halbjahrSel = new HalbjahrMitgliedSelectDialog(MainWindow.this);
            halbjahrSel.setVisible(true);
            Halbjahr halbjahr = halbjahrSel.getChosenHalbjahr();
            if (halbjahr != null) {
                reportViewer.viewAbrechnungHalbjahr(halbjahr, true);
            }
        });

        JButton btnRepAbrechnungHalbjahr2 = new JButton("Abrechnung Halbjahr (nur offene Beiträge)");
        panelAuswertungen.add(btnRepAbrechnungHalbjahr2, MIG_BUTTON_CONSTRAINTS);
        btnRepAbrechnungHalbjahr2.addActionListener(e -> {
            HalbjahrMitgliedSelectDialog halbjahrSel;
            halbjahrSel = new HalbjahrMitgliedSelectDialog(MainWindow.this);
            halbjahrSel.setVisible(true);
            Halbjahr halbjahr = halbjahrSel.getChosenHalbjahr();
            if (halbjahr != null) {
                reportViewer.viewAbrechnungHalbjahr(halbjahr, false);
            }
        });

        /**** Beenden ****/
        JButton buttonClose = new JButton("Beenden");
        getContentPane().add(buttonClose, "cell 0 5,span,alignx center");
        buttonClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
