package nami.beitrag.gui;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;

import com.toedter.calendar.JDateChooser;

/**
 * Stellt einen Dialog bereit, mit dem eine Rechnungsdatei (im CSV-Format), eine
 * Rechnungsnummer und das Datum der Rechnung erfasst werden.
 * 
 * @author Fabian Lipp
 * 
 */
public class RechnungImportDialog extends JDialog {
    private static final long serialVersionUID = -3139318038881653596L;

    private File chosenFile = null;
    private JLabel lblSelectedFile;
    private JTextField rechnungsNummer = null;
    private JDateChooser rechnungsDatum = null;
    private boolean accepted = false;

    /**
     * Erzeugt einen neuen <tt>RechnungImportDialog</tt>.
     * 
     * @param parent
     *            besitzendes Fenster
     */
    public RechnungImportDialog(Window parent) {
        super(parent, DEFAULT_MODALITY_TYPE);
        setTitle("Rechnung auswählen");
        buildFrame();
    }

    private void buildFrame() {
        getContentPane().setLayout(
                new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        getContentPane().add(panel);
        panel.setLayout(new MigLayout("", "[][200px:n,grow][]", "[][][][]"));

        JLabel lblFile = new JLabel("Datei");
        lblSelectedFile = new JLabel();
        JButton btnSelectFile = new JButton("Datei wählen");
        btnSelectFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                int returnVal = chooser
                        .showOpenDialog(RechnungImportDialog.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    chosenFile = chooser.getSelectedFile();
                    lblSelectedFile.setText(chosenFile.getName());
                }
            }
        });
        panel.add(lblFile, "");
        panel.add(lblSelectedFile, "");
        panel.add(btnSelectFile, "grow,wrap");

        JLabel lblRechnungsNummer = new JLabel("Rechnungsnummer");
        rechnungsNummer = new JTextField();
        lblRechnungsNummer.setLabelFor(rechnungsNummer);
        lblRechnungsNummer.setDisplayedMnemonic('n');
        panel.add(lblRechnungsNummer, "");
        panel.add(rechnungsNummer, "grow,span,wrap");

        JLabel lblRechnungsDatum = new JLabel("Rechnungsdatum");
        rechnungsDatum = new JDateChooser();
        lblRechnungsDatum.setLabelFor(rechnungsDatum);
        lblRechnungsDatum.setDisplayedMnemonic('d');
        panel.add(lblRechnungsDatum, "");
        panel.add(rechnungsDatum, "grow,span,wrap");

        JButton btnAccept = new JButton("OK");
        btnAccept.setMnemonic('o');
        btnAccept.addActionListener(new AcceptButtonListener());
        panel.add(btnAccept, "span,trail");

        JRootPane rootPane = getRootPane();
        rootPane.setDefaultButton(btnAccept);
        // Handle ESC-key
        Action escListener = new AbstractAction() {
            private static final long serialVersionUID = 8011175609689348329L;

            @Override
            public void actionPerformed(ActionEvent e) {
                dispatchEvent(new WindowEvent(RechnungImportDialog.this,
                        WindowEvent.WINDOW_CLOSING));
            }
        };
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "jnami.ESC");
        rootPane.getActionMap().put("jnami.ESC", escListener);

        pack();
    }

    /**
     * Liefert die ausgewählte Datei.
     * 
     * @return ausgewählte Datei
     */
    public File getChosenFile() {
        return chosenFile;
    }

    /**
     * Liefert die eingegebene Rechnungsnummer.
     * 
     * @return eingegebene Rechnungsnummer
     */
    public String getRechnungsNummer() {
        return rechnungsNummer.getText();
    }

    /**
     * Liefert das ausgewählte Rechnungsdatum.
     * 
     * @return ausgewähltes Datum
     */
    public Date getRechnungsDatum() {
        return rechnungsDatum.getDate();
    }

    /**
     * Gibt an, ob das Fenster mit dem OK-Button bestätigt wurden und die
     * Eingaben gültig sind.
     * 
     * @return true, wenn die Eingaben bestätigt wurden
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Trägt bei Auswahl des OK-Buttons die Rückgabe ein und schließt den
     * Dialog.
     */
    private class AcceptButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean valid = true;
            if (chosenFile == null || !chosenFile.exists()) {
                JOptionPane.showMessageDialog(RechnungImportDialog.this,
                        "Keine Rechnungs-Datei ausgewählt", "Fehlende Eingabe",
                        JOptionPane.ERROR_MESSAGE);
                valid = false;
            }

            if (rechnungsNummer.getText() == null
                    || rechnungsNummer.getText().isEmpty()) {
                JOptionPane.showMessageDialog(RechnungImportDialog.this,
                        "Keine Rechnungsnummer eingegeben", "Fehlende Eingabe",
                        JOptionPane.ERROR_MESSAGE);
                valid = false;
            }

            if (rechnungsDatum.getDate() == null
                    || rechnungsDatum.getDate().before(
                            new GregorianCalendar(2000, 01, 01).getTime())) {
                JOptionPane.showMessageDialog(RechnungImportDialog.this,
                        "Ungültiges Datum ausgewählt", "Fehlende Eingabe",
                        JOptionPane.ERROR_MESSAGE);
                valid = false;
            }

            if (valid) {
                RechnungImportDialog.this.setVisible(false);
                accepted = true;
            }
        }
    }
}
