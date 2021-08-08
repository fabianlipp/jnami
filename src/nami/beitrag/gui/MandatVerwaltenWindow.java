package nami.beitrag.gui;

import nami.beitrag.gui.utils.MyStringUtils;
import net.miginfocom.swing.MigLayout;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Stellt ein Fenster bereit, mit dem bestehende Mandate bearbeitet,
 * (de-)aktiviert und anderen Mitgliedern zugewiesen werden können.
 * 
 * @author Fabian Lipp
 * 
 */
public class MandatVerwaltenWindow extends JFrame {
    private static final long serialVersionUID = -6932254055456226188L;

    private final SqlSessionFactory sqlSessionFactory;

    private JPanel panelMitglied;
    private MitgliedSelectComponent mitgliedSelector;
    private JPanel panelMandatsnummer;
    private JTextField fieldMandatsnummer = null;
    private JTabbedPane tabbedPane;
    private MandateVerwaltenComponent mandateVerwalten;

    /**
     * Erzeugt ein neues Rechnungs-Fenster.
     * 
     * @param sqlSessionFactory
     *            Zugriff auf die Datenbank
     */
    public MandatVerwaltenWindow(SqlSessionFactory sqlSessionFactory) {
        super("SEPA-Mandate verwalten");
        this.sqlSessionFactory = sqlSessionFactory;
        buildFrame();
    }

    private void buildFrame() {

        getContentPane().setLayout(
                new MigLayout("", "[grow]", "[][grow]"));

        panelMitglied = new JPanel();
        panelMitglied.setLayout(new FlowLayout());
        mitgliedSelector = new MitgliedSelectComponent(sqlSessionFactory);
        panelMitglied.add(mitgliedSelector);

        panelMandatsnummer = new JPanel();
        panelMandatsnummer.setLayout(new FlowLayout());
        fieldMandatsnummer = new JTextField();
        fieldMandatsnummer.setColumns(5);
        panelMandatsnummer.add(fieldMandatsnummer);

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        getContentPane().add(tabbedPane, "cell 0 0,grow");
        tabbedPane.addTab("Mitgliedersuche", null, panelMitglied, null);
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_M);
        tabbedPane.addTab("Mandatsnummer", null, panelMandatsnummer, null);
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_A);

        mandateVerwalten = new MandateVerwaltenComponent(sqlSessionFactory);
        getContentPane().add(mandateVerwalten, "cell 0 1");

        pack();

        mitgliedSelector.addChangeListener(e -> updateShownMandate());
        fieldMandatsnummer.addActionListener(e -> updateShownMandate());
        tabbedPane.addChangeListener(e -> updateShownMandate());
    }

    private void updateShownMandate() {
        if (tabbedPane.getSelectedComponent() == panelMitglied) {
            mandateVerwalten.filterMandate(MandateVerwaltenComponent.MandateFilterType.MITGLIED, mitgliedSelector.getMitgliedId());
        } else if (tabbedPane.getSelectedComponent() == panelMandatsnummer) {
            int mandatsNummer = MyStringUtils.parseIntDefaultMinusOne(fieldMandatsnummer.getText());
            mandateVerwalten.filterMandate(MandateVerwaltenComponent.MandateFilterType.MANDAT, mandatsNummer);
        }
    }

}
