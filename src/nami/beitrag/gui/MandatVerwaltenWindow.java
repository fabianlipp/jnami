package nami.beitrag.gui;

import net.miginfocom.swing.MigLayout;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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

    private MitgliedSelectComponent mitgliedSelector;
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

        JLabel lblMitglied = new JLabel("Mitglied:");
        getContentPane().add(lblMitglied, "flowx,cell 0 0");

        mitgliedSelector = new MitgliedSelectComponent(sqlSessionFactory);
        mitgliedSelector.addChangeListener(new MitgliedFilterChange());
        getContentPane().add(mitgliedSelector, "cell 0 0");

        mandateVerwalten = new MandateVerwaltenComponent(sqlSessionFactory);
        getContentPane().add(mandateVerwalten, "cell 0 1");

        pack();
    }

    /**
     * Aktualisiert die Mandatsliste, wenn ein neues Mitglied im Eingabefeld
     * eingetragen wird.
     */
    private class MitgliedFilterChange implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            mandateVerwalten.changeMitglied(mitgliedSelector.getMitgliedId());
        }
    }
}
