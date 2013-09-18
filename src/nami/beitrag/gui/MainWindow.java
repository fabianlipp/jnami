package nami.beitrag.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import nami.beitrag.NamiBeitrag;
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
