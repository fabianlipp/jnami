package nami.beitrag.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import nami.beitrag.db.BeitragSepaMandat;
import nami.beitrag.db.MandateMapper;
import net.miginfocom.swing.MigLayout;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import com.toedter.calendar.JDateChooser;

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

    private SqlSessionFactory sqlSessionFactory;
    private BeitragSepaMandat mandat;

    private JTextField txtKontoinhaber;
    private JTextField txtStrasse;
    private JTextField txtPlz;
    private JTextField txtOrt;
    private JTextField txtEmail;
    private JTextField txtIban;
    private JTextField txtBic;
    private JDateChooser dateChooser;

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
        buildFrame();

        this.mandat = mandat;

        txtKontoinhaber.setText(mandat.getKontoinhaber());
        txtStrasse.setText(mandat.getStrasse());
        txtPlz.setText(mandat.getPlz());
        txtOrt.setText(mandat.getOrt());
        txtEmail.setText(mandat.getEmail());
        txtIban.setText(mandat.getIban());
        txtBic.setText(mandat.getBic());
        dateChooser.setDate(mandat.getDatum());
    }

    private void buildFrame() {
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new MigLayout("", "[][grow]",
                "[grow][][][][][][][][][]"));

        JLabel lblKontoinhaber = new JLabel("Kontoinhaber:");
        contentPane.add(lblKontoinhaber, "cell 0 1");
        txtKontoinhaber = new JTextField();
        txtKontoinhaber.setEditable(false);
        txtKontoinhaber.setColumns(50);
        contentPane.add(txtKontoinhaber, "cell 1 1");

        JLabel lblStrasse = new JLabel("Straße:");
        contentPane.add(lblStrasse, "cell 0 2");
        txtStrasse = new JTextField();
        txtStrasse.setColumns(50);
        contentPane.add(txtStrasse, "cell 1 2");

        JLabel lblPlz = new JLabel("PLZ:");
        contentPane.add(lblPlz, "cell 0 3");
        txtPlz = new JTextField();
        txtPlz.setColumns(5);
        contentPane.add(txtPlz, "cell 1 3");

        JLabel lblOrt = new JLabel("Ort:");
        contentPane.add(lblOrt, "cell 0 4");
        txtOrt = new JTextField();
        txtOrt.setColumns(50);
        contentPane.add(txtOrt, "cell 1 4");

        JLabel lblEmail = new JLabel("E-Mail:");
        contentPane.add(lblEmail, "cell 0 5");
        txtEmail = new JTextField();
        txtEmail.setColumns(50);
        contentPane.add(txtEmail, "cell 1 5");

        JLabel lblIban = new JLabel("IBAN:");
        contentPane.add(lblIban, "cell 0 6");
        txtIban = new JTextField();
        txtIban.setEditable(false);
        txtIban.setColumns(34);
        contentPane.add(txtIban, "cell 1 6");

        JLabel lblBic = new JLabel("BIC:");
        contentPane.add(lblBic, "cell 0 7");
        txtBic = new JTextField();
        txtBic.setEditable(false);
        txtBic.setColumns(11);
        contentPane.add(txtBic, "cell 1 7");

        JLabel lblDatum = new JLabel("Datum des Mandats:");
        contentPane.add(lblDatum, "cell 0 8");
        dateChooser = new JDateChooser(new Date());
        dateChooser.setEnabled(false);
        contentPane.add(dateChooser, "cell 1 8");

        JButton btnMandatSpeichern = new JButton("Mandat speichern");
        btnMandatSpeichern.addActionListener(new SpeichernAction());
        contentPane.add(btnMandatSpeichern, "cell 0 9 2 1,alignx right");

        pack();
    }

    /**
     * Speichert die Änderungen am Mandat in die Datenbank.
     */
    private class SpeichernAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            SqlSession session = sqlSessionFactory.openSession();
            try {
                MandateMapper mandateMapper = session
                        .getMapper(MandateMapper.class);
                mandat.setIban(txtIban.getText());
                mandat.setBic(txtBic.getText());
                mandat.setDatum(dateChooser.getDate());
                mandat.setKontoinhaber(txtKontoinhaber.getText());
                mandat.setStrasse(txtStrasse.getText());
                mandat.setPlz(txtPlz.getText());
                mandat.setOrt(txtOrt.getText());
                mandat.setEmail(txtEmail.getText());
                mandat.setGueltig(true);

                mandateMapper.updateMandat(mandat);
                session.commit();
                setVisible(false);
            } finally {
                session.close();
            }
        }

    }

}
