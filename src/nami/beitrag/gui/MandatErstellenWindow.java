package nami.beitrag.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.BeitragMitglied;
import nami.beitrag.db.BeitragSepaMandat;
import nami.beitrag.db.MandateMapper;
import net.miginfocom.swing.MigLayout;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import com.toedter.calendar.JDateChooser;

/**
 * Fenster, mit dem ein neues SEPA-Lastschrift-Mandat erfasst werden kann. Dabei
 * kann eine beliebige Zahl von Mitgliedern gewählt werden, denen das Mandat
 * zugeordnet wird.
 * 
 * @author Fabian Lipp
 * 
 */
public class MandatErstellenWindow extends JFrame {
    private static final long serialVersionUID = 7121215418429194498L;

    private SqlSessionFactory sqlSessionFactory;

    // Komponenten für Tabelle
    private JTable mitgliederTable;
    private MitgliedTableModel mitgliederModel;

    private JTextField txtKontoinhaber;
    private JTextField txtStrasse;
    private JTextField txtPlz;
    private JTextField txtOrt;
    private JTextField txtEmail;
    private JTextField txtIban;
    private JTextField txtBic;
    private MitgliedSelectComponent mitgliedSelector;
    private JCheckBox chckbxAktivesMandat;
    private JDateChooser dateChooser;

    /**
     * Erzeugt ein neues Rechnungs-Fenster.
     * 
     * @param sqlSessionFactory
     *            Zugriff auf die Datenbank
     */
    public MandatErstellenWindow(SqlSessionFactory sqlSessionFactory) {
        super("SEPA-Mandat erfassen");
        this.sqlSessionFactory = sqlSessionFactory;
        buildFrame();
    }

    private void buildFrame() {
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new MigLayout("", "[][grow]",
                "[grow][][][][][][][][][][]"));

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Mitglieder",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPane.add(panel, "cell 0 0 2 1,grow");
        panel.setLayout(new MigLayout("", "[][]", "[][][][]"));

        mitgliedSelector = new MitgliedSelectComponent(sqlSessionFactory);
        panel.add(mitgliedSelector, "cell 0 0");
        JButton btnAddMitglied = new JButton("Hinzufügen");
        btnAddMitglied.addActionListener(new AddMitgliedAction());
        panel.add(btnAddMitglied, "cell 1 0,growx,aligny top");

        JScrollPane scrollPane = new JScrollPane();
        panel.add(scrollPane, "cell 0 1 1 3,grow");
        mitgliederModel = new MitgliedTableModel();
        mitgliederTable = new JTable();
        mitgliederTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mitgliederTable.setModel(mitgliederModel);
        scrollPane.setViewportView(mitgliederTable);

        JButton btnDelMitglied = new JButton("Entfernen");
        btnDelMitglied.addActionListener(new DeleteMitgliedAction());
        panel.add(btnDelMitglied, "cell 1 2,growx,aligny top");

        JButton btnUseMitglied = new JButton("Verwende Daten");
        btnUseMitglied.addActionListener(new UseMitgliedAction());
        panel.add(btnUseMitglied, "cell 1 3,growx,aligny top");

        JLabel lblKontoinhaber = new JLabel("Kontoinhaber:");
        contentPane.add(lblKontoinhaber, "cell 0 1");
        txtKontoinhaber = new JTextField();
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
        txtIban.setColumns(34);
        contentPane.add(txtIban, "cell 1 6");

        JLabel lblBic = new JLabel("BIC:");
        contentPane.add(lblBic, "cell 0 7");
        txtBic = new JTextField();
        txtBic.setColumns(11);
        contentPane.add(txtBic, "cell 1 7");

        JLabel lblDatum = new JLabel("Datum des Mandats:");
        contentPane.add(lblDatum, "cell 0 8");
        dateChooser = new JDateChooser(new Date());
        contentPane.add(dateChooser, "cell 1 8");

        JLabel lblZumAktivenMandat = new JLabel("Zum aktiven Mandat machen:");
        contentPane.add(lblZumAktivenMandat, "cell 0 9");

        chckbxAktivesMandat = new JCheckBox("");
        chckbxAktivesMandat.setSelected(true);
        contentPane.add(chckbxAktivesMandat, "cell 1 9");

        JButton btnMandatSpeichern = new JButton("Mandat speichern");
        btnMandatSpeichern.addActionListener(new SpeichernAction());
        contentPane.add(btnMandatSpeichern, "cell 0 10 2 1,alignx right");

        pack();
    }

    /**
     * Fügt das im Eingabefeld eingegebene Mitglied in die Liste ein.
     */
    private class AddMitgliedAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int mitgliedId = mitgliedSelector.getMitgliedId();
            if (mitgliedId == -1) {
                // Kein Mitglied eingegeben
                return;
            }

            mitgliederModel.addMitglied(mitgliedId);
        }
    }

    /**
     * Löscht das selektierte Mitglied aus der Liste.
     */
    private class DeleteMitgliedAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int selectedId = mitgliederModel.getIdAtRow(mitgliederTable
                    .getSelectedRow());
            if (selectedId != -1) {
                mitgliederModel.deleteMitglied(selectedId);
            }

        }
    }

    /**
     * Überträgt die Grunddaten (Name, Adresse, Email) des selektierten
     * Mitglieds in die Eingabefelder.
     */
    private class UseMitgliedAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            BeitragMitglied mgl = mitgliederModel
                    .getMitgliedAtRow(mitgliederTable.getSelectedRow());
            if (mgl != null) {
                txtKontoinhaber.setText(mgl.getVorname() + " "
                        + mgl.getNachname());
                txtStrasse.setText(mgl.getStrasse());
                txtPlz.setText(mgl.getPlz());
                txtOrt.setText(mgl.getOrt());
                txtEmail.setText(mgl.getEmail());
            }
        }
    }

    /**
     * Speichert das eingegebene Lastschrift-Mandat in die Datenbank.
     */
    private class SpeichernAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            SqlSession session = sqlSessionFactory.openSession();
            try {
                MandateMapper mandateMapper = session
                        .getMapper(MandateMapper.class);
                BeitragSepaMandat mand = new BeitragSepaMandat();
                mand.setIban(txtIban.getText());
                mand.setBic(txtBic.getText());
                mand.setDatum(dateChooser.getDate());
                mand.setKontoinhaber(txtKontoinhaber.getText());
                mand.setStrasse(txtStrasse.getText());
                mand.setPlz(txtPlz.getText());
                mand.setOrt(txtOrt.getText());
                mand.setEmail(txtEmail.getText());
                mand.setGueltig(true);

                // Mandat einfügen
                mandateMapper.insertMandat(mand);
                int mandatId = mand.getMandatId();

                // Zuordnung von Mandat zu Mitgliedern
                for (int mitgliedId : mitgliederModel.mitgliederIds) {
                    mandateMapper.addMitgliedForMandat(mandatId, mitgliedId);
                    if (chckbxAktivesMandat.isSelected()) {
                        mandateMapper.setAktivesMandat(mandatId, mitgliedId);
                    }
                }

                session.commit();

                JOptionPane.showMessageDialog(MandatErstellenWindow.this,
                        "Mandat angelegt mit ID " + mandatId + ".");
                setVisible(false);
            } finally {
                session.close();
            }
        }
    }

    /**
     * Stellt eine Tabelle von Mitgliedern bereit, zu der Personen hinzugefügt
     * werden können und auch wieder entfernt werden können.
     */
    private class MitgliedTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 9181646451834946830L;

        private ArrayList<BeitragMitglied> mitglieder = new ArrayList<>();
        private Set<Integer> mitgliederIds = new HashSet<>();

        private static final int NACHNAME_COLUMN_INDEX = 0;
        private static final int VORNAME_COLUMN_INDEX = 1;
        private static final int MITGLIEDSNUMMER_COLUMN_INDEX = 2;

        @Override
        public int getRowCount() {
            if (mitglieder == null) {
                return 0;
            } else {
                return mitglieder.size();
            }
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (mitglieder == null || rowIndex >= mitglieder.size()) {
                return null;
            }

            BeitragMitglied mitglied = mitglieder.get(rowIndex);
            switch (columnIndex) {
            case NACHNAME_COLUMN_INDEX:
                return mitglied.getNachname();
            case VORNAME_COLUMN_INDEX:
                return mitglied.getVorname();
            case MITGLIEDSNUMMER_COLUMN_INDEX:
                return mitglied.getMitgliedsnummer();
            default:
                return null;
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case NACHNAME_COLUMN_INDEX:
                return "Nachname";
            case VORNAME_COLUMN_INDEX:
                return "Vorname";
            case MITGLIEDSNUMMER_COLUMN_INDEX:
                return "Mitgliedsnummer";
            default:
                return "";
            }
        }

        public int getIdAtRow(int rowIndex) {
            BeitragMitglied mgl = getMitgliedAtRow(rowIndex);
            if (mgl != null) {
                return mgl.getMitgliedId();
            } else {
                return -1;
            }
        }

        public BeitragMitglied getMitgliedAtRow(int rowIndex) {
            if (mitglieder == null || rowIndex >= mitglieder.size()) {
                return null;
            }

            return mitglieder.get(rowIndex);
        }

        public void addMitglied(int mitgliedId) {
            if (mitgliederIds.contains(mitgliedId)) {
                // Mitglied ist schon enthalten
                // => nichts zu tun
                return;
            }

            SqlSession session = sqlSessionFactory.openSession();
            try {
                BeitragMapper mapper = session.getMapper(BeitragMapper.class);
                BeitragMitglied mgl = mapper.getMitglied(mitgliedId);
                if (mgl != null) {
                    mitglieder.add(mgl);
                    mitgliederIds.add(mgl.getMitgliedId());
                }
                fireTableDataChanged();
            } finally {
                session.close();
            }
        }

        public void deleteMitglied(int mitgliedId) {
            boolean contained = mitgliederIds.remove(mitgliedId);
            if (contained) {
                Iterator<BeitragMitglied> iter = mitglieder.iterator();
                while (iter.hasNext()) {
                    BeitragMitglied mgl = iter.next();
                    if (mgl.getMitgliedId() == mitgliedId) {
                        iter.remove();
                    }
                }
                fireTableDataChanged();
            }
        }
    }

}
