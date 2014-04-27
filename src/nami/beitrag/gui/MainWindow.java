package nami.beitrag.gui;

import guitest.SelectTest;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

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
import nami.beitrag.db.LastschriftenMapper.FilterSettings;
import nami.beitrag.db.RechnungenMapper;
import nami.beitrag.db.RechnungenMapper.DataMahnungKomplett;
import nami.beitrag.db.ReportsMapper;
import nami.beitrag.letters.LetterDirectory;
import nami.beitrag.letters.LetterGenerator;
import nami.beitrag.letters.LetterType;
import nami.beitrag.reports.DataAbrechnungHalbjahr;
import nami.beitrag.reports.PDFReportGenerator;
import nami.connector.exception.NamiApiException;
import net.miginfocom.swing.MigLayout;

import org.apache.ibatis.session.SqlSession;

/**
 * Stellt das Hauptfenster der GUI dar.
 * 
 * @author Fabian Lipp
 * 
 */
// TODO: Lange Strings externalisieren
public class MainWindow extends JFrame {
    private static final long serialVersionUID = 7477838944466651902L;

    private LetterGenerator letterGenerator;

    /**
     * Erzeugt das Hauptfenster.
     * 
     * @param namiBeitrag
     *            Objekt für die Beitragslogik (enthält Zugriff auf Datenbank
     *            und NaMi)
     */
    public MainWindow(final NamiBeitrag namiBeitrag,
            final LetterDirectory letterDirectory) {
        letterGenerator = new LetterGenerator(namiBeitrag.getSessionFactory(),
                letterDirectory);

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

        JButton button11 = new JButton("Rechnungen erstellen");
        buttons.add(button11, "grow");
        button11.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RechnungenErstellenWindow win = new RechnungenErstellenWindow(
                        namiBeitrag.getSessionFactory(), letterGenerator);
                win.setVisible(true);
            }
        });
        JLabel label11 = new JLabel("def");
        buttons.add(label11, "grow,wrap");

        JButton button12 = new JButton("Rechnungen verwalten");
        buttons.add(button12, "grow");
        button12.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame win = new RechnungenVerwaltenWindow(namiBeitrag
                        .getSessionFactory(), letterGenerator);
                win.setVisible(true);
            }
        });
        JLabel label12 = new JLabel("def");
        buttons.add(label12, "grow,wrap");

        JButton button13 = new JButton("Mandat erfassen");
        buttons.add(button13, "grow,wrap");
        button13.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame win = new MandatErstellenWindow(namiBeitrag
                        .getSessionFactory());
                win.setVisible(true);
            }
        });

        JButton button14 = new JButton("Mandate verwalten");
        buttons.add(button14, "grow,wrap");
        button14.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame win = new MandatVerwaltenWindow(namiBeitrag
                        .getSessionFactory());
                win.setVisible(true);
            }
        });

        JButton button15 = new JButton("Teste MyBatis Nested Results");
        buttons.add(button15, "grow,wrap");
        button15.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SqlSession session = namiBeitrag.getSessionFactory()
                        .openSession();
                try {
                    RechnungenMapper mapper = session
                            .getMapper(RechnungenMapper.class);
                    FilterSettings filterSettings = new FilterSettings();
                    filterSettings.setBereitsErstellt(true);
                    DataMahnungKomplett result = mapper.getMahnungKomplett(3);

                    System.out.println("Fertig");
                } finally {
                    session.close();
                }
            }
        });

        JButton button16 = new JButton("Lastschriften erstellen");
        buttons.add(button16, "grow,wrap");
        button16.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame win = new LastschriftErstellenWindow(namiBeitrag
                        .getSessionFactory());
                win.setVisible(true);
            }
        });

        JButton button17 = new JButton("Lastschriften verwalten");
        buttons.add(button17, "grow,wrap");
        button17.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame win = new LastschriftVerwaltenWindow(namiBeitrag
                        .getSessionFactory());
                win.setVisible(true);
            }
        });

        JButton button18 = new JButton(
                "Mahnungen 1-4 mit LetterGenerator erzeugen");
        buttons.add(button18, "grow,wrap");
        button18.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LinkedList<Integer> ids = new LinkedList<>();
                ids.add(1);
                ids.add(2);
                ids.add(3);
                ids.add(4);
                letterGenerator.generateLetters(LetterType.MAHNUNG, ids,
                        new Date());
            }
        });

        JButton button19 = new JButton("Briefe verwalten");
        buttons.add(button19, "grow,wrap");
        button19.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame win = new BriefeWindow(namiBeitrag.getSessionFactory(),
                        letterDirectory);
                win.setVisible(true);
            }
        });

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
        new MainWindow(null, null).setVisible(true);
    }
}
