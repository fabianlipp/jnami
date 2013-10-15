package nami.beitrag.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import nami.beitrag.Buchungstyp;
import nami.beitrag.db.BeitragBuchung;
import nami.beitrag.db.BeitragMapper;
import net.miginfocom.swing.MigLayout;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import com.toedter.calendar.JDateChooser;

/**
 * Dialog zum Einfügen oder Anzeigen einer Buchung.
 * 
 * @author Fabian Lipp
 * 
 */
public class BuchungDialog extends JFrame {
    private static final long serialVersionUID = -5350557200646276237L;

    private SqlSessionFactory sessionFactory;

    private JLabel lblId;
    private MitgliedSelectComponent mitglied;
    private JLabel namiBuchungId;
    private JLabel rechnungsNummer;
    private JComboBox<Buchungstyp> typ;
    private JDateChooser dateChooser;
    private JTextField betrag;
    private HalbjahrComponent halbjahr;
    private JCheckBox vorausberechnung;
    private JTextField kommentar;
    private JButton btnAbbrechen;
    private JButton btnSpeichern;

    /**
     * Zeigt einer vorhandene Buchung an. Alle Eingabefelder werden gesperrt,
     * die Buchung kann nicht bearbeitet werden.
     * 
     * @param sessionFactory
     *            Verbindung zur SQL-Datenbank
     * @param buchung
     *            anzuzeigende Buchung
     */
    public BuchungDialog(SqlSessionFactory sessionFactory,
            BeitragBuchung buchung) {
        this.sessionFactory = sessionFactory;
        buildFrame(false);

        // Daten einsetzen
        lblId.setText(Integer.toString(buchung.getBuchungId()));
        mitglied.setMitgliedId(buchung.getMitgliedId());
        mitglied.setEnabled(false);
        namiBuchungId.setText(Integer.toString(buchung.getNamiBuchungId()));
        rechnungsNummer.setText(buchung.getRechnungsNummer());
        typ.setSelectedItem(buchung.getTyp());
        typ.setEnabled(false);
        dateChooser.setDate(buchung.getDatum());
        dateChooser.setEnabled(false);
        betrag.setText(buchung.getBetrag().toString());
        betrag.setEnabled(false);
        halbjahr.setValue(buchung.getHalbjahr());
        halbjahr.setEnabled(false);
        vorausberechnung.setSelected(buchung.isVorausberechnung());
        vorausberechnung.setEnabled(false);
        kommentar.setText(buchung.getKommentar());
        kommentar.setEnabled(false);
    }

    /**
     * Zeigt einen leeren Dialog, in den die Daten einer neuen Buchung
     * eingetragen werden können.
     * 
     * @param sessionFactory
     *            Verbindung zur SQL-Datenbank
     * @wbp.parser.constructor
     */
    public BuchungDialog(SqlSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        buildFrame(true);
    }

    /**
     * Erzeugt die Komponenten des Dialogs.
     * 
     * @param saveButton
     *            gibt an, ob es einen Speicherbutton gibt
     */
    private void buildFrame(boolean saveButton) {
        getContentPane().setLayout(new MigLayout("", "[][grow]", ""));

        JLabel lbllblId = new JLabel("ID:");
        getContentPane().add(lbllblId, "cell 0 0");
        lblId = new JLabel("");
        getContentPane().add(lblId, "cell 1 0");

        JLabel lblMitglied = new JLabel("Mitglied:");
        lblMitglied.setDisplayedMnemonic('m');
        getContentPane().add(lblMitglied, "cell 0 1");
        mitglied = new MitgliedSelectComponent(sessionFactory);
        lblMitglied.setLabelFor(mitglied);
        getContentPane().add(mitglied, "cell 1 1");

        JLabel lbllblNamibuchungsid = new JLabel("NaMi-Buchung-ID:");
        getContentPane().add(lbllblNamibuchungsid, "cell 0 2");
        namiBuchungId = new JLabel("");
        getContentPane().add(namiBuchungId, "cell 1 2");

        JLabel lblRechnungsnummer = new JLabel("Rechnungsnummer:");
        getContentPane().add(lblRechnungsnummer, "cell 0 3");
        rechnungsNummer = new JLabel("");
        getContentPane().add(rechnungsNummer, "cell 1 3");

        JLabel lblTyp = new JLabel("Typ:");
        lblTyp.setDisplayedMnemonic('t');
        getContentPane().add(lblTyp, "cell 0 4,alignx left");
        typ = new JComboBox<Buchungstyp>();
        lblTyp.setLabelFor(typ);
        typ.setModel(new DefaultComboBoxModel<>(Buchungstyp.values()));
        getContentPane().add(typ, "cell 1 4,growx");

        JLabel lblDatum = new JLabel("Datum:");
        lblDatum.setDisplayedMnemonic('d');
        getContentPane().add(lblDatum, "cell 0 5");
        dateChooser = new JDateChooser();
        lblDatum.setLabelFor(dateChooser);
        getContentPane().add(dateChooser, "cell 1 5,grow");

        JLabel lblBetrag = new JLabel("Betrag:");
        lblBetrag.setDisplayedMnemonic('b');
        getContentPane().add(lblBetrag, "cell 0 6,alignx left");
        betrag = new JTextField();
        lblBetrag.setLabelFor(betrag);
        getContentPane().add(betrag, "cell 1 6,growx");
        betrag.setColumns(10);

        JLabel lblHalbjahr = new JLabel("Halbjahr:");
        lblHalbjahr.setDisplayedMnemonic('h');
        getContentPane().add(lblHalbjahr, "cell 0 7");
        halbjahr = new HalbjahrComponent();
        lblHalbjahr.setLabelFor(halbjahr);
        getContentPane().add(halbjahr, "cell 1 7");

        JLabel lblVorausberechnung = new JLabel("Vorausberechnung:");
        lblVorausberechnung.setDisplayedMnemonic('v');
        getContentPane().add(lblVorausberechnung, "cell 0 8");
        vorausberechnung = new JCheckBox("");
        lblVorausberechnung.setLabelFor(vorausberechnung);
        getContentPane().add(vorausberechnung, "cell 1 8");

        JLabel lblKommentar = new JLabel("Kommentar:");
        lblKommentar.setDisplayedMnemonic('k');
        getContentPane().add(lblKommentar, "cell 0 9,alignx left");
        kommentar = new JTextField();
        lblKommentar.setLabelFor(kommentar);
        getContentPane().add(kommentar, "cell 1 9,growx");
        kommentar.setColumns(10);

        if (saveButton) {
            btnAbbrechen = new JButton("Abbrechen");
            btnAbbrechen.setMnemonic('a');
            btnAbbrechen.addActionListener(new CloseActionListener());
            getContentPane().add(btnAbbrechen,
                    "flowx,cell 1 10,alignx trailing");

            btnSpeichern = new JButton("Speichern");
            btnSpeichern.setMnemonic('s');
            btnSpeichern.addActionListener(new SaveActionListener());
            getContentPane().add(btnSpeichern, "cell 1 10,alignx trailing");
        } else {
            btnAbbrechen = new JButton("Schließen");
            btnAbbrechen.setMnemonic('s');
            btnAbbrechen.addActionListener(new CloseActionListener());
            getContentPane().add(btnAbbrechen,
                    "flowx,cell 1 10,alignx trailing");
        }

        pack();
    }

    /**
     * Speichert die eingegebene Buchung beim Klick auf den Speichern-Button.
     */
    private class SaveActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            BeitragBuchung buchung = new BeitragBuchung();

            int mitgliedId = mitglied.getMitgliedId();
            if (mitgliedId == -1) {
                return;
            }
            buchung.setMitgliedId(mitgliedId);

            if (typ.getSelectedIndex() == -1) {
                return;
            }
            buchung.setTyp(typ.getItemAt(typ.getSelectedIndex()));

            if (dateChooser.getDate() == null) {
                return;
            }
            buchung.setDatum(dateChooser.getDate());

            BigDecimal betragDec;
            try {
                betragDec = new BigDecimal(betrag.getText());
            } catch (NumberFormatException ex) {
                return;
            }
            buchung.setBetrag(betragDec);

            buchung.setHalbjahr(halbjahr.getValue());
            buchung.setVorausberechnung(vorausberechnung.isSelected());
            buchung.setKommentar(kommentar.getText());

            SqlSession session = sessionFactory.openSession();
            try {
                BeitragMapper mapper = session.getMapper(BeitragMapper.class);
                mapper.insertBuchung(buchung);
                session.commit();
            } finally {
                session.close();
            }
            System.out.println("Stored Buchung");
        }
    }

    /**
     * Schließt das Fenster ohne zu Speichern beim Klick auf den Button.
     */
    private class CloseActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }
}
