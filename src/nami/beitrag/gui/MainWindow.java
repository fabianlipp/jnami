package nami.beitrag.gui;

import guitest.SelectTest;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import nami.beitrag.NamiBeitrag;
import nami.beitrag.db.BeitragBuchung;
import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.BeitragMitglied;
import nami.beitrag.db.ReportsMapper;
import nami.beitrag.reports.DataAbrechnungHalbjahr;
import nami.beitrag.reports.PDFReportGenerator;
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

    /**
     * Erzeugt das Hauptfenster.
     * 
     * @param namiBeitrag
     *            Objekt für die Beitragslogik (enthält Zugriff auf Datenbank
     *            und NaMi)
     */
    public MainWindow(final NamiBeitrag namiBeitrag) {
        setTitle("NamiBeitrag");

        final BeitragMapper mapper = namiBeitrag.getSessionFactory()
                .openSession().getMapper(BeitragMapper.class);
        final ReportsMapper reportMapper = namiBeitrag.getSessionFactory()
                .openSession().getMapper(ReportsMapper.class);

        JPanel buttons = new JPanel();
        JPanel control = new JPanel();
        buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        control.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttons.setLayout(new MigLayout("", "[][]", "[][][][][]"));

        JButton button1 = new JButton("Mitglieder synchronisieren");
        button1.addActionListener(new ActionListener() {
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
        buttons.add(button1, "grow");
        JLabel label1 = new JLabel(
                "<html>Holt alle Mitglieder mit der entsprechenden "
                        + "Stammgruppierung aus NaMi und speichert sie in die "
                        + "lokale Datenbank bzw. aktualisiert ihre "
                        + "lokalen Datensätze.</html>");
        label1.setLabelFor(button1);
        buttons.add(label1, "grow,wrap");

        JButton button2 = new JButton("Beitragszahlungen holen");
        buttons.add(button2, "grow");
        button2.addActionListener(new ActionListener() {
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
        JLabel label2 = new JLabel("def");
        buttons.add(label2, "grow,wrap");

        JButton button3 = new JButton("Mitglied auswählen");
        buttons.add(button3, "grow");
        button3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MitgliedSelectDialog sel = new MitgliedSelectDialog(
                        MainWindow.this, namiBeitrag.getSessionFactory());
                sel.setVisible(true);
                System.out.println("chosen: " + sel.getChosenMglId());
            }
        });
        JLabel label3 = new JLabel("def");
        buttons.add(label3, "grow,wrap");

        JButton button4 = new JButton("Mitglied auswählen (Panel)");
        buttons.add(button4, "grow");
        button4.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SelectTest sel = new SelectTest(namiBeitrag.getSessionFactory());
                sel.setVisible(true);
            }
        });
        JLabel label4 = new JLabel("def");
        buttons.add(label4, "grow,wrap");

        JButton button5 = new JButton("Vorausberechnung");
        buttons.add(button5, "grow");
        button5.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HalbjahrSelectDialog halbjahrSel = new HalbjahrSelectDialog(
                        MainWindow.this);
                halbjahrSel.setVisible(true);
                namiBeitrag.vorausberechnung(halbjahrSel.getChosenHalbjahr());
            }
        });
        JLabel label5 = new JLabel("def");
        buttons.add(label5, "grow,wrap");

        JButton button6 = new JButton("Beitragskonto");
        buttons.add(button6, "grow");
        button6.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BeitragskontoWindow beitragskontoWin = new BeitragskontoWindow(
                        namiBeitrag.getSessionFactory());
                beitragskontoWin.setVisible(true);
            }
        });
        JLabel label6 = new JLabel("def");
        buttons.add(label6, "grow,wrap");

        JButton button7 = new JButton("Buchung (ID 11) anzeigen");
        buttons.add(button7, "grow");
        button7.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BeitragBuchung buchung = mapper.getBuchungById(11);
                BuchungDialog diag = new BuchungDialog(namiBeitrag
                        .getSessionFactory(), buchung);
                diag.setVisible(true);
            }
        });
        JLabel label7 = new JLabel("def");
        buttons.add(label7, "grow,wrap");

        JButton button8 = new JButton("Neue Buchung");
        buttons.add(button8, "grow");
        button8.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BuchungDialog diag = new BuchungDialog(namiBeitrag
                        .getSessionFactory());
                diag.setVisible(true);
            }
        });
        JLabel label8 = new JLabel("def");
        buttons.add(label8, "grow,wrap");

        JButton button9 = new JButton("Mitglied bearbeiten");
        buttons.add(button9, "grow");
        button9.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MitgliedSelectDialog sel = new MitgliedSelectDialog(
                        MainWindow.this, namiBeitrag.getSessionFactory());
                sel.setVisible(true);

                BeitragMitglied mgl = mapper.getMitglied(sel.getChosenMglId());
                MitgliedDialog diag = new MitgliedDialog(namiBeitrag
                        .getSessionFactory(), mgl);
                diag.setVisible(true);
            }
        });
        JLabel label9 = new JLabel("def");
        buttons.add(label9, "grow,wrap");

        JButton button10 = new JButton("Abrechnung für Halbjahr erzeugen");
        buttons.add(button10, "grow");
        button10.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HalbjahrSelectDialog halbjahrSel = new HalbjahrSelectDialog(
                        MainWindow.this);
                halbjahrSel.setVisible(true);

                HashMap<String, Object> params = new HashMap<>();
                params.put("HALBJAHR", halbjahrSel.getChosenHalbjahr()
                        .toString());
                Collection<DataAbrechnungHalbjahr> data = reportMapper
                        .abrechnungHalbjahr(halbjahrSel.getChosenHalbjahr());

                PDFReportGenerator.generateReport("abrechnung_halbjahr",
                        params, data, "abrechnungHalbjahr.pdf");
            }
        });
        JLabel label10 = new JLabel("def");
        buttons.add(label10, "grow,wrap");
        
        JButton button11 = new JButton("Rechnungen");
        buttons.add(button11, "grow");
        button11.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RechnungWindow win = new RechnungWindow(namiBeitrag.getSessionFactory());
                win.setVisible(true);
            }
        });
        JLabel label11 = new JLabel("def");
        buttons.add(label11, "grow,wrap");

        JButton buttonClose = new JButton("Beenden");
        buttonClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        control.add(buttonClose);

        getContentPane().setLayout(
                new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        getContentPane().add(buttons);
        getContentPane().add(control);
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /**
     * Main-Funktion zum Testen des Fensters ohne Funktion.
     * 
     * @param args
     *            Kommandozeilen-Argumente
     */
    // TODO: Debug
    public static void main(String[] args) {
        new MainWindow(null).setVisible(true);
    }
}
