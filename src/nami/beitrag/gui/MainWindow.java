package nami.beitrag.gui;

import java.awt.Window;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
    private final LetterGenerator letterGenerator;
    private final ReportViewer reportViewer;

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
        btnSync.addActionListener(e -> {
            try {
                namiBeitrag.syncMitglieder();
            } catch (NamiApiException | IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });

        JButton btnFetch = new JButton("<html>Beitragszahlungen aus NaMi holen"
                + "<br>(von NaMi momentan nicht unterstützt)");
        panelNami.add(btnFetch, MIG_BUTTON_CONSTRAINTS);
        btnFetch.addActionListener(e -> {
            try {
                namiBeitrag.fetchBeitragszahlungen();
            } catch (NamiApiException | IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });

        JButton btnCsvImport = new JButton("Rechnung im CSV-Format einlesen");
        panelNami.add(btnCsvImport, MIG_BUTTON_CONSTRAINTS);
        btnCsvImport.addActionListener(e -> {
            RechnungCsvImport imp = new RechnungCsvImport(namiBeitrag
                    .getSessionFactory());
            RechnungImportDialog diag = new RechnungImportDialog(
                    MainWindow.this);
            diag.setVisible(true);
            if (diag.isAccepted()) {
                imp.importCsv(diag.getChosenFile(),
                        diag.getRechnungsNummer(), diag.getRechnungsDatum());
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
        btnVorausberechnung.addActionListener(e -> {
            HalbjahrMitgliedSelectDialog halbjahrSel;
            halbjahrSel = new HalbjahrMitgliedSelectDialog(MainWindow.this);
            halbjahrSel.setVisible(true);
            if (halbjahrSel.getChosenHalbjahr() != null) {
                namiBeitrag.vorausberechnung(halbjahrSel
                        .getChosenHalbjahr());
            }
        });

        JButton btnVorausberechnungUpdate = new JButton(
                "Vorausberechnung aktualisieren ...");
        panelVorausberechnungen.add(btnVorausberechnungUpdate,
                MIG_BUTTON_CONSTRAINTS);
        btnVorausberechnungUpdate.addActionListener(e -> {
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
        });

        JButton btnVorausberechnungDelete = new JButton(
                "Vorausberechnungen für Halbjahr löschen ...");
        panelVorausberechnungen.add(btnVorausberechnungDelete,
                MIG_BUTTON_CONSTRAINTS);
        btnVorausberechnungDelete.addActionListener(e -> {
            HalbjahrMitgliedSelectDialog halbjahrSel;
            halbjahrSel = new HalbjahrMitgliedSelectDialog(MainWindow.this);
            halbjahrSel.setVisible(true);
            if (halbjahrSel.getChosenHalbjahr() != null) {
                namiBeitrag.loescheVorausberechnungen(halbjahrSel
                        .getChosenHalbjahr());
            }
        });

        /**** Buchungen ****/
        JPanel panelBuchungen = new JPanel();
        getContentPane().add(panelBuchungen, "cell 0 1,grow");
        panelBuchungen.setBorder(new TitledBorder("Buchungen"));
        panelBuchungen.setLayout(new MigLayout("", "[]", "[][][]"));

        JButton btnBeitragskonto = new JButton("Beitragskonto");
        panelBuchungen.add(btnBeitragskonto, MIG_BUTTON_CONSTRAINTS);
        btnBeitragskonto.addActionListener(
                this.actionListenerCreateAndShowWindow(() ->
                    new MitgliedAnzeigenWindow(namiBeitrag.getSessionFactory())));

        JButton btnNeueBuchung = new JButton("Neue Buchung");
        panelBuchungen.add(btnNeueBuchung, MIG_BUTTON_CONSTRAINTS);
        btnNeueBuchung.addActionListener(
                this.actionListenerCreateAndShowWindow(() ->
                    new BuchungDialog(namiBeitrag.getSessionFactory())));

        JButton btnRepBuchungenHalbjahr = new JButton("Anzahl Buchungen pro Halbjahr");
        panelBuchungen.add(btnRepBuchungenHalbjahr, MIG_BUTTON_CONSTRAINTS);
        btnRepBuchungenHalbjahr.addActionListener(e -> reportViewer.viewAnzahlBuchungenProHalbjahr());

        /**** Mandate ****/
        JPanel panelMandate = new JPanel();
        getContentPane().add(panelMandate, "cell 1 1,grow");
        panelMandate.setBorder(new TitledBorder("SEPA-Mandate"));
        panelMandate.setLayout(new MigLayout("", "[]", "[][]"));

        JButton btnMandateErfassen = new JButton("Mandat erfassen");
        panelMandate.add(btnMandateErfassen, MIG_BUTTON_CONSTRAINTS);
        btnMandateErfassen.addActionListener(
                this.actionListenerCreateAndShowWindow(() ->
                    new MandatErstellenWindow(namiBeitrag.getSessionFactory())));

        JButton btnMandateVerwalten = new JButton("Mandate verwalten");
        panelMandate.add(btnMandateVerwalten, MIG_BUTTON_CONSTRAINTS);
        btnMandateVerwalten.addActionListener(
                this.actionListenerCreateAndShowWindow(() ->
                    new MandatVerwaltenWindow(namiBeitrag.getSessionFactory())));

        JButton btnMitgliederOhneSepa = new JButton(
                "Mitglieder ohne gültiges SEPA-Mandat anzeigen");
        panelMandate.add(btnMitgliederOhneSepa, MIG_BUTTON_CONSTRAINTS);
        btnMitgliederOhneSepa.addActionListener(e -> reportViewer.viewMitgliederOhneSepaMandat());

        /**** Rechnungen ****/
        JPanel panelRechnungen = new JPanel();
        getContentPane().add(panelRechnungen, "cell 0 2,grow");
        panelRechnungen.setBorder(new TitledBorder("Rechnungen"));
        panelRechnungen.setLayout(new MigLayout("", "[]", "[][]"));

        JButton btnRechnungenErstellen = new JButton("Rechnungen erstellen");
        panelRechnungen.add(btnRechnungenErstellen, MIG_BUTTON_CONSTRAINTS);
        btnRechnungenErstellen.addActionListener(
                this.actionListenerCreateAndShowWindow(() ->
                    new RechnungenErstellenWindow(namiBeitrag.getSessionFactory(), letterGenerator)));

        JButton btnRechnungenVerwalten = new JButton("Rechnungen verwalten");
        panelRechnungen.add(btnRechnungenVerwalten, MIG_BUTTON_CONSTRAINTS);
        btnRechnungenVerwalten.addActionListener(
                this.actionListenerCreateAndShowWindow(() ->
                    new RechnungenVerwaltenWindow(namiBeitrag.getSessionFactory(), letterGenerator)));

        /**** Lastschriften ****/
        JPanel panelLastschriften = new JPanel();
        getContentPane().add(panelLastschriften, "cell 1 2,grow");
        panelLastschriften.setBorder(new TitledBorder("Lastschriften"));
        panelLastschriften.setLayout(new MigLayout("", "[]", "[][]"));

        JButton btnLastschriftenErstellen = new JButton(
                "Lastschriften erstellen");
        panelLastschriften.add(btnLastschriftenErstellen,
                MIG_BUTTON_CONSTRAINTS);
        btnLastschriftenErstellen.addActionListener(
                this.actionListenerCreateAndShowWindow(() ->
                    new LastschriftErstellenWindow(namiBeitrag.getSessionFactory())));

        JButton btnLastschriftenVerwalten = new JButton(
                "Lastschriften verwalten");
        panelLastschriften.add(btnLastschriftenVerwalten,
                MIG_BUTTON_CONSTRAINTS);
        btnLastschriftenVerwalten.addActionListener(
                this.actionListenerCreateAndShowWindow(() ->
                    new LastschriftVerwaltenWindow(namiBeitrag.getSessionFactory(), letterGenerator, conf)));

        /**** Briefe verwalten ****/
        JPanel panelBriefeVerwalten = new JPanel();
        getContentPane().add(panelBriefeVerwalten, "cell 0 3,grow");
        panelBriefeVerwalten.setBorder(new TitledBorder("Briefe"));
        panelBriefeVerwalten.setLayout(new MigLayout("", "[]", "[]"));
        JButton btnBriefeVerwalten = new JButton("Briefe verwalten");
        panelBriefeVerwalten.add(btnBriefeVerwalten, MIG_BUTTON_CONSTRAINTS);
        btnBriefeVerwalten.addActionListener(
                this.actionListenerCreateAndShowWindow(() ->
                    new BriefeWindow(namiBeitrag.getSessionFactory(), letterDirectory, conf)));

        /**** Abmeldungen ****/
        JPanel panelAbmeldungen = new JPanel();
        getContentPane().add(panelAbmeldungen, "cell 1 3,grow");
        panelAbmeldungen.setBorder(new TitledBorder("Abmeldungen"));
        panelAbmeldungen.setLayout(new MigLayout("", "[]", "[][]"));

        JButton btnAbmeldungVormerken = new JButton("Abmeldung vormerken");
        panelAbmeldungen.add(btnAbmeldungVormerken, MIG_BUTTON_CONSTRAINTS);
        btnAbmeldungVormerken.addActionListener(
                this.actionListenerCreateAndShowWindow(() ->
                    new AbmeldungErstellenWindow(namiBeitrag.getSessionFactory())));

        JButton btnAbmeldungenVerwalten = new JButton("Abmeldungen verwalten");
        panelAbmeldungen.add(btnAbmeldungenVerwalten, MIG_BUTTON_CONSTRAINTS);
        btnAbmeldungenVerwalten.addActionListener(
                this.actionListenerCreateAndShowWindow(() ->
                    new AbmeldungenAnzeigenWindow(namiBeitrag.getSessionFactory())));

        /**** Auswertungen ****/
        JPanel panelAuswertungen = new JPanel();
        getContentPane().add(panelAuswertungen, "cell 0 4,grow");
        panelAuswertungen.setBorder(new TitledBorder("Auswertungen"));
        panelAuswertungen.setLayout(new MigLayout("", "[]", "[][]"));

        JButton btnRepAbrechnungHalbjahr1 = new JButton("Abrechnung Halbjahr");
        panelAuswertungen.add(btnRepAbrechnungHalbjahr1, MIG_BUTTON_CONSTRAINTS);
        btnRepAbrechnungHalbjahr1.addActionListener(
                this.actionListenerReportWithHalbjahr(
                        halbjahr -> reportViewer.viewAbrechnungHalbjahr(halbjahr, true)));

        JButton btnRepAbrechnungHalbjahr2 = new JButton("Abrechnung Halbjahr (nur offene Beiträge)");
        panelAuswertungen.add(btnRepAbrechnungHalbjahr2, MIG_BUTTON_CONSTRAINTS);
        btnRepAbrechnungHalbjahr2.addActionListener(
                this.actionListenerReportWithHalbjahr(
                        halbjahr -> reportViewer.viewAbrechnungHalbjahr(halbjahr, false)));

        JButton btnRepAbrechnungHalbjahr3 = new JButton("Abrechnung Halbjahr (nur nach Typen)");
        panelAuswertungen.add(btnRepAbrechnungHalbjahr3, MIG_BUTTON_CONSTRAINTS);
        btnRepAbrechnungHalbjahr3.addActionListener(
                this.actionListenerReportWithHalbjahr(
                        reportViewer::viewAbrechnungHalbjahrNachTypen));

        /**** Beenden ****/
        JButton buttonClose = new JButton("Beenden");
        getContentPane().add(buttonClose, "cell 0 5,span,alignx center");
        buttonClose.addActionListener(e -> System.exit(0));

        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private ActionListener actionListenerCreateAndShowWindow(Supplier<Window> windowSupplier) {
        return actionEvent -> {
            Window window = windowSupplier.get();
            window.setVisible(true);
        };
    }

    private ActionListener actionListenerReportWithHalbjahr(Consumer<Halbjahr> consumer) {
        return actionEvent -> {
            HalbjahrMitgliedSelectDialog halbjahrSel;
            halbjahrSel = new HalbjahrMitgliedSelectDialog(MainWindow.this);
            halbjahrSel.setVisible(true);
            Halbjahr halbjahr = halbjahrSel.getChosenHalbjahr();
            if (halbjahr != null) {
                consumer.accept(halbjahr);
            }
        };
    }
}
