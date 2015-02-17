package nami.beitrag.gui;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import org.apache.ibatis.session.SqlSessionFactory;

import nami.connector.Halbjahr;
import net.miginfocom.swing.MigLayout;

/**
 * Stellt einen Dialog bereit, mit dem ein Halbjahr und ggf. gleichzeitig auch
 * ein Mitglied ausgewählt werden kann.
 * 
 * @author Fabian Lipp
 * 
 */
public class HalbjahrMitgliedSelectDialog extends JDialog {
    private static final long serialVersionUID = -3139318038881653596L;

    private SqlSessionFactory sessionFactory;
    private HalbjahrComponent halbjahrSelector;
    private MitgliedSelectComponent mitgliedSelector;
    private Halbjahr chosenHalbjahr = null;
    private int chosenMitgliedId = -1;

    /**
     * Erzeugt einen neuen <tt>HalbjahrMitgliedSelectDialog</tt> ohne ein
     * Eingabefeld für ein Mitglied (also nur Halbjahr).
     * 
     * @param parent
     *            besitzendes Fenster
     */
    public HalbjahrMitgliedSelectDialog(Window parent) {
        super(parent, DEFAULT_MODALITY_TYPE);
        setTitle("Halbjahr auswählen");
        this.sessionFactory = null;
        buildFrame();
    }

    /**
     * Erzeugt einen neuen <tt>HalbjahrMitgliedSelectDialog</tt> inkl. eines
     * Eingabefeldes für ein Mitglied.
     * 
     * @param parent
     *            besitzendes Fenster
     * @param sessionFactory
     *            Verbindung zur SQL-Datenbank mit den Mitgliederdaten
     */
    public HalbjahrMitgliedSelectDialog(Window parent,
            SqlSessionFactory sessionFactory) {
        super(parent, DEFAULT_MODALITY_TYPE);
        setTitle("Halbjahr und Mitglied auswählen");
        this.sessionFactory = sessionFactory;
        buildFrame();
    }

    private void buildFrame() {
        getContentPane().setLayout(
                new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        getContentPane().add(panel);
        String migRows;
        if (sessionFactory != null) {
            migRows = "[][][]";
        } else {
            migRows = "[][]";
        }
        panel.setLayout(new MigLayout("", "[][grow]", migRows));

        JLabel lblHalbjahr = new JLabel("Halbjahr");
        halbjahrSelector = new HalbjahrComponent();
        lblHalbjahr.setLabelFor(halbjahrSelector);
        lblHalbjahr.setDisplayedMnemonic('h');
        panel.add(lblHalbjahr, "");
        panel.add(halbjahrSelector, "grow,wrap");

        if (sessionFactory != null) {
            JLabel lblMitglied = new JLabel("Mitglied");
            mitgliedSelector = new MitgliedSelectComponent(sessionFactory);
            lblMitglied.setLabelFor(mitgliedSelector);
            lblMitglied.setDisplayedMnemonic('m');
            panel.add(lblMitglied, "");
            panel.add(mitgliedSelector, "grow,wrap");
        }

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
                dispatchEvent(new WindowEvent(
                        HalbjahrMitgliedSelectDialog.this,
                        WindowEvent.WINDOW_CLOSING));
            }
        };
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "jnami.ESC");
        rootPane.getActionMap().put("jnami.ESC", escListener);

        pack();
    }

    /**
     * Liefert das gewählte Halbjahr.
     * 
     * @return gewähltes Halbjahr
     */
    public Halbjahr getChosenHalbjahr() {
        return chosenHalbjahr;
    }

    /**
     * Liefert die ID des gewählten Mitglieds.
     * 
     * @return ID des Mitglieds
     */
    public int getChosenMitgliedId() {
        return chosenMitgliedId;
    }

    /**
     * Trägt bei Auswahl des OK-Buttons die Rückgabe ein und schließt den
     * Dialog.
     */
    private class AcceptButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean valid = true;
            chosenHalbjahr = halbjahrSelector.getValue();
            if (chosenHalbjahr == null) {
                JOptionPane.showMessageDialog(
                        HalbjahrMitgliedSelectDialog.this,
                        "Kein Halbjahr ausgewählt", "Fehlende Eingabe",
                        JOptionPane.ERROR_MESSAGE);
                valid = false;
            }

            if (sessionFactory != null) {
                chosenMitgliedId = mitgliedSelector.getMitgliedId();
                if (chosenMitgliedId == -1) {
                    JOptionPane.showMessageDialog(
                            HalbjahrMitgliedSelectDialog.this,
                            "Kein Mitglied ausgewählt", "Fehlende Eingabe",
                            JOptionPane.ERROR_MESSAGE);
                    valid = false;
                }
            }

            if (valid) {
                HalbjahrMitgliedSelectDialog.this.setVisible(false);
            }
        }
    }
}
