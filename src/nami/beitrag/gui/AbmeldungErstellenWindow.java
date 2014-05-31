package nami.beitrag.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;

import nami.beitrag.AbmeldungTyp;
import nami.beitrag.db.AbmeldungenMapper;
import nami.beitrag.db.BeitragAbmeldung;
import net.miginfocom.swing.MigLayout;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import com.toedter.calendar.JDateChooser;

/**
 * Dialog zum Vormerken einer Abmeldung.
 * 
 * @author Fabian Lipp
 * 
 */
public class AbmeldungErstellenWindow extends JFrame {
    private static final long serialVersionUID = 5728350237853923954L;

    private SqlSessionFactory sessionFactory;
    private MitgliedSelectComponent mitglied;
    private JDateChooser dateChooser;
    private JDateChooser faelligkeit;
    private JComboBox<AbmeldungTyp> typ;
    private JDateChooser namiEingetragen;
    private JButton btnAbbrechen;
    private JButton btnSpeichern;

    /**
     * Zeigt einen leeren Dialog, in den die Daten einer neuen Abmeldung
     * eingetragen werden können.
     * 
     * @param sessionFactory
     *            Verbindung zur SQL-Datenbank
     */
    public AbmeldungErstellenWindow(SqlSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        setTitle("Neue Abmeldung vormerken");
        buildFrame();
    }

    /**
     * Erzeugt die Komponenten des Dialogs.
     * 
     * @param saveButton
     *            gibt an, ob es einen Speicherbutton gibt
     */
    private void buildFrame() {
        getContentPane().setLayout(
                new MigLayout("", "[][grow]", "[][][][][][][][][]"));

        JLabel lblMitglied = new JLabel("Mitglied:");
        lblMitglied.setDisplayedMnemonic('m');
        getContentPane().add(lblMitglied, "");
        mitglied = new MitgliedSelectComponent(sessionFactory);
        lblMitglied.setLabelFor(mitglied);
        getContentPane().add(mitglied, "flowx,wrap");

        JLabel lblDatum = new JLabel("Datum:");
        lblDatum.setDisplayedMnemonic('d');
        getContentPane().add(lblDatum, "");
        dateChooser = new JDateChooser(new Date());
        lblDatum.setLabelFor(dateChooser);
        getContentPane().add(dateChooser, "wrap");

        JLabel lblFaelligkeit = new JLabel("Fälligkeit:");
        lblDatum.setDisplayedMnemonic('f');
        getContentPane().add(lblFaelligkeit, "");
        faelligkeit = new JDateChooser(new Date());
        lblFaelligkeit.setLabelFor(faelligkeit);
        getContentPane().add(faelligkeit, "wrap");

        JLabel lblTyp = new JLabel("Typ:");
        lblTyp.setDisplayedMnemonic('t');
        getContentPane().add(lblTyp, "");
        typ = new JComboBox<AbmeldungTyp>();
        lblTyp.setLabelFor(typ);
        typ.setModel(new DefaultComboBoxModel<>(AbmeldungTyp.values()));
        getContentPane().add(typ, "wrap");

        JLabel lblNamiEingetragen = new JLabel("In NaMi eingetragen:");
        lblDatum.setDisplayedMnemonic('n');
        getContentPane().add(lblNamiEingetragen, "");
        namiEingetragen = new JDateChooser();
        lblNamiEingetragen.setLabelFor(namiEingetragen);
        getContentPane().add(namiEingetragen, "wrap");

        btnAbbrechen = new JButton("Abbrechen");
        btnAbbrechen.setMnemonic('a');
        btnAbbrechen.addActionListener(new CloseActionListener());
        getContentPane()
                .add(btnAbbrechen, "span,split 2,flowx,alignx trailing");

        btnSpeichern = new JButton("Speichern");
        btnSpeichern.setMnemonic('s');
        btnSpeichern.addActionListener(new SaveActionListener());
        getContentPane().add(btnSpeichern, "alignx trailing");

        pack();
    }

    /**
     * Speichert die eingegebene Buchung beim Klick auf den Speichern-Button.
     */
    private class SaveActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!validateForm()) {
                return;
            }

            BeitragAbmeldung abmeld = new BeitragAbmeldung();

            abmeld.setMitgliedId(mitglied.getMitgliedId());
            abmeld.setDatum(dateChooser.getDate());
            abmeld.setFaelligkeit(faelligkeit.getDate());
            abmeld.setTyp(typ.getItemAt(typ.getSelectedIndex()));
            abmeld.setNamiEingetragen(namiEingetragen.getDate());

            SqlSession session = sessionFactory.openSession();
            try {
                AbmeldungenMapper mapper = session
                        .getMapper(AbmeldungenMapper.class);
                mapper.insertAbmeldung(abmeld);
                session.commit();
            } finally {
                session.close();
            }
            System.out.println("Stored Abmeldung");
        }
    }

    private boolean validateForm() {
        boolean hasError = false;

        if (mitglied.getMitgliedId() == -1) {
            hasError = true;
        }

        if (typ.getSelectedIndex() == -1) {
            hasError = true;
        }

        if (dateChooser.getDate() == null) {
            hasError = true;
        }

        if (faelligkeit.getDate() == null) {
            hasError = true;
        }

        return !hasError;
    }

    /**
     * Schließt das Fenster ohne zu Speichern beim Klick auf den Button.
     */
    private class CloseActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
            dispose();
        }
    }
}
