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
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import nami.connector.Halbjahr;
import net.miginfocom.swing.MigLayout;

/**
 * Stellt einen Dialog bereit, mit dem ein Halbjahr ausgewählt werden kann.
 * 
 * @author Fabian Lipp
 * 
 */
public class HalbjahrSelectDialog extends JDialog {
    private static final long serialVersionUID = -3139318038881653596L;
    private HalbjahrComponent halbjahrSelector;
    private Halbjahr chosenHalbjahr = null;

    /**
     * Erzeugt einen neuen <tt>HalbjahrSelectDialog</tt>.
     * 
     * @param parent
     *            besitzendes Fenster
     */
    public HalbjahrSelectDialog(Window parent) {
        super(parent, DEFAULT_MODALITY_TYPE);
        setTitle("Halbjahr auswählen");
        buildFrame();
    }

    private void buildFrame() {
        getContentPane().setLayout(
                new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        getContentPane().add(panel);
        panel.setLayout(new MigLayout("", "[][grow]", "[][]"));

        JLabel lblHalbjahr = new JLabel("Halbjahr");
        halbjahrSelector = new HalbjahrComponent();
        lblHalbjahr.setLabelFor(halbjahrSelector);
        lblHalbjahr.setDisplayedMnemonic('h');
        panel.add(lblHalbjahr, "");
        panel.add(halbjahrSelector, "grow,wrap");

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
                dispatchEvent(new WindowEvent(HalbjahrSelectDialog.this,
                        WindowEvent.WINDOW_CLOSING));
            }
        };
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "jnami.ESC");
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
     * Trägt bei Auswahl des OK-Buttons die Rückgabe ein und schließt den Dialog.
     */
    private class AcceptButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            chosenHalbjahr = halbjahrSelector.getValue();
            HalbjahrSelectDialog.this.setVisible(false);
        }
    }
}
