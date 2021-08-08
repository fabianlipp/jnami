package nami.beitrag.gui;

import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.BeitragMitglied;
import nami.beitrag.db.BeitragSepaMandat;
import nami.beitrag.db.MandateMapper;
import net.miginfocom.swing.MigLayout;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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

    private final SqlSessionFactory sqlSessionFactory;

    // Komponenten für Tabelle
    private JTable mitgliederTable;
    private MitgliedTableModel mitgliederModel;

    private MitgliedSelectComponent mitgliedSelector;
    private MandatDatenComponent mandatDaten;
    private JCheckBox chckbxAktivesMandat;

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
        contentPane.setLayout(new MigLayout("", "[grow]", "[grow][][][]"));

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Mitglieder",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPane.add(panel, "cell 0 0 2 1,grow");
        panel.setLayout(new MigLayout("", "[grow][]", "[][][][]"));

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

        mandatDaten = new MandatDatenComponent();
        contentPane.add(mandatDaten, "cell 0 1");

        JLabel lblZumAktivenMandat = new JLabel("Zum aktiven Mandat machen:");
        contentPane.add(lblZumAktivenMandat, "cell 0 2,flowx");

        chckbxAktivesMandat = new JCheckBox("");
        chckbxAktivesMandat.setSelected(true);
        contentPane.add(chckbxAktivesMandat, "cell 0 2");

        JButton btnAbbrechen = new JButton("Abbrechen");
        btnAbbrechen.addActionListener(e -> dispose());
        contentPane.add(btnAbbrechen, "cell 0 3 2 1,flowx,alignx right");

        JButton btnMandatSpeichern = new JButton("Mandat speichern");
        btnMandatSpeichern.addActionListener(new SpeichernAction());
        contentPane.add(btnMandatSpeichern, "cell 0 3 2 1");

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
            mandatDaten.fillFromMitglied(mgl);
        }
    }

    /**
     * Speichert das eingegebene Lastschrift-Mandat in die Datenbank.
     */
    private class SpeichernAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try (SqlSession session = sqlSessionFactory.openSession()) {
                MandateMapper mandateMapper = session
                        .getMapper(MandateMapper.class);
                BeitragSepaMandat mand = new BeitragSepaMandat();
                mandatDaten.writeInputsToMandat(mand);
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
            }
        }
    }

    /**
     * Stellt eine Tabelle von Mitgliedern bereit, zu der Personen hinzugefügt
     * werden können und auch wieder entfernt werden können.
     */
    private class MitgliedTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 9181646451834946830L;

        private final ArrayList<BeitragMitglied> mitglieder = new ArrayList<>();
        private final Set<Integer> mitgliederIds = new HashSet<>();

        private static final int NACHNAME_COLUMN_INDEX = 0;
        private static final int VORNAME_COLUMN_INDEX = 1;
        private static final int MITGLIEDSNUMMER_COLUMN_INDEX = 2;

        @Override
        public int getRowCount() {
            return mitglieder.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= mitglieder.size()) {
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
            if (rowIndex >= mitglieder.size()) {
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

            try (SqlSession session = sqlSessionFactory.openSession()) {
                BeitragMapper mapper = session.getMapper(BeitragMapper.class);
                BeitragMitglied mgl = mapper.getMitglied(mitgliedId);
                if (mgl != null) {
                    mitglieder.add(mgl);
                    mitgliederIds.add(mgl.getMitgliedId());
                }
                fireTableDataChanged();
            }
        }

        public void deleteMitglied(int mitgliedId) {
            boolean contained = mitgliederIds.remove(mitgliedId);
            if (contained) {
                mitglieder.removeIf(mgl -> mgl.getMitgliedId() == mitgliedId);
                fireTableDataChanged();
            }
        }
    }

}
