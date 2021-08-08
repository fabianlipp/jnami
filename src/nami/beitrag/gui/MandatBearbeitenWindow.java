package nami.beitrag.gui;

import nami.beitrag.db.BeitragSepaMandat;
import nami.beitrag.db.MandateMapper;
import net.miginfocom.swing.MigLayout;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Stellt ein Fenster bereit, mit dem ein SEPA-Lastschrift-Mandat bearbeitet
 * werden kann.
 * 
 * Die Felder Kontoinhaber, IBAN, BIC, Mandats-Datum können nicht bearbeitet
 * werden, weil das eine Mandatsänderung wäre, die bei der nächsten Lastschrift
 * mit an die Bank übertragen werden müsste. Diesen Aufwand umgehen wir dadurch,
 * dass wir in diesem Fall einfach ein neues Mandat anlegen.
 * 
 * @author Fabian Lipp
 * 
 */
public class MandatBearbeitenWindow extends JFrame {
    private static final long serialVersionUID = -4160091692061910940L;

    private final SqlSessionFactory sqlSessionFactory;
    private final BeitragSepaMandat mandat;

    private MandatDatenComponent mandatDaten;

    /**
     * Erzeugt ein neues Rechnungs-Fenster.
     * 
     * @param sqlSessionFactory
     *            Zugriff auf die Datenbank
     * @param mandat
     *            Das Mandat, das bearbeitet werden soll
     */
    public MandatBearbeitenWindow(SqlSessionFactory sqlSessionFactory,
            BeitragSepaMandat mandat) {
        super("SEPA-Mandat bearbeiten");
        this.sqlSessionFactory = sqlSessionFactory;
        this.mandat = mandat;
        buildFrame();
    }

    private void buildFrame() {
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new MigLayout("", "[]",
                "[][]"));

        mandatDaten = new MandatDatenComponent(this.mandat);
        contentPane.add(mandatDaten, "cell 0 0");

        JButton btnAbbrechen = new JButton("Abbrechen");
        btnAbbrechen.addActionListener(e -> dispose());
        contentPane.add(btnAbbrechen, "cell 0 1 2 1,flowx,alignx right");

        JButton btnMandatSpeichern = new JButton("Mandat speichern");
        btnMandatSpeichern.addActionListener(new SpeichernAction());
        contentPane.add(btnMandatSpeichern, "cell 0 1 2 1");

        pack();
    }

    /**
     * Speichert die Änderungen am Mandat in die Datenbank.
     */
    private class SpeichernAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try (SqlSession session = sqlSessionFactory.openSession()) {
                MandateMapper mandateMapper = session
                        .getMapper(MandateMapper.class);
                mandatDaten.writeInputsToMandat(mandat);
                mandateMapper.updateMandat(mandat);
                session.commit();
                setVisible(false);
            }
        }
    }
}
