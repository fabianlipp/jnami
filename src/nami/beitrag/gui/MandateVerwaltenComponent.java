package nami.beitrag.gui;

import nami.beitrag.db.BeitragMitglied;
import nami.beitrag.db.BeitragSepaMandat;
import nami.beitrag.db.MandateMapper;
import net.miginfocom.swing.MigLayout;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;

public class MandateVerwaltenComponent extends JComponent {
    public enum MandateFilterType {
        MITGLIED, MANDAT
    }

    private final SqlSessionFactory sqlSessionFactory;

    private JTable mandateTable;
    private MandateTableModel mandateModel;
    private JTable mitgliederTable;
    private MitgliederTableModel mitgliederModel;
    private LastschriftenTableModel lastschriftenModel;

    public MandateVerwaltenComponent(SqlSessionFactory sessionFactory) {
        this.sqlSessionFactory = sessionFactory;
        createPanel();
    }

    public void filterMandate(MandateFilterType typ, int id) {
        mitgliederModel.clear();
        lastschriftenModel.clear();
        mandateModel.loadMandate(typ, id);
    }

    private void createPanel() {
        setLayout(new MigLayout("", "[grow]", "[][grow][][][grow][][][grow]"));

        JLabel lblMandate = new JLabel("Mandate:");
        add(lblMandate, "cell 0 0");

        JScrollPane mandatePane = new JScrollPane();
        add(mandatePane, "cell 0 1,grow");
        mandateModel = new MandateTableModel();
        mandateTable = new JTable();
        mandateTable.setModel(mandateModel);
        mandateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mandateTable.getSelectionModel().addListSelectionListener(
                new MandateSelectionListener());
        mandatePane.setViewportView(mandateTable);

        JButton btnGueltig = new JButton("Gültig machen");
        btnGueltig.addActionListener(new GueltigAction(true));
        add(btnGueltig, "flowx,cell 0 2");

        JButton btnUngueltig = new JButton("Ungültig machen");
        btnUngueltig.addActionListener(new GueltigAction(false));
        add(btnUngueltig, "cell 0 2");

        JButton btnBearbeiten = new JButton("Bearbeiten");
        btnBearbeiten.addActionListener(new BearbeitenAction());
        add(btnBearbeiten, "cell 0 2");

        JLabel lblZugeordneteMitglieder = new JLabel("Zugeordnete Mitglieder:");
        add(lblZugeordneteMitglieder, "cell 0 3");

        JScrollPane mitgliederPane = new JScrollPane();
        add(mitgliederPane, "cell 0 4,grow");
        mitgliederModel = new MitgliederTableModel();
        mitgliederTable = new JTable();
        mitgliederTable.setModel(mitgliederModel);
        mitgliederTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mitgliederPane.setViewportView(mitgliederTable);

        JButton btnAktiv = new JButton("Aktiv machen");
        btnAktiv.addActionListener(new AktivAction());
        add(btnAktiv, "flowx,cell 0 5");

        JButton btnInaktiv = new JButton("Inaktiv machen");
        btnInaktiv.addActionListener(new InaktivAction());
        add(btnInaktiv, "cell 0 5");

        JButton btnAddMitglied = new JButton("Mitglied hinzufügen");
        btnAddMitglied.addActionListener(new AddMitgliedAction());
        add(btnAddMitglied, "cell 0 5");

        JLabel lblDurchgefuehrteLastschriften = new JLabel("Durchgeführte Lastschriften:");
        add(lblDurchgefuehrteLastschriften, "cell 0 6");

        JScrollPane lastschriftenPane = new JScrollPane();
        add(lastschriftenPane, "cell 0 7,grow");
        lastschriftenModel = new LastschriftenTableModel();
        JTable lastschriftenTable = new JTable();
        lastschriftenTable.setModel(lastschriftenModel);
        lastschriftenTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lastschriftenPane.setViewportView(lastschriftenTable);
    }

    /**
     * Aktualisiert die Tabelle, in der die Mitglieder angezeigt werden, wenn
     * ein anderes Mandat selektiert wird.
     */
    private class MandateSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                int mandatId = mandateModel.getIdAtRow(mandateTable
                        .getSelectedRow());
                mitgliederModel.loadMitglieder(mandatId);
                lastschriftenModel.loadLastschriften(mandatId);
            }
        }
    }

    /**
     * Macht das ausgewählte Mandat gültig bzw. ungültig.
     */
    private final class GueltigAction implements ActionListener {
        // Status (gültig=true, ungültig=false), den das Mandat bekommen soll
        private final boolean state;

        private GueltigAction(boolean state) {
            this.state = state;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            BeitragSepaMandat mandat = mandateModel.getMandatAtRow(mandateTable
                    .getSelectedRow());
            if (mandat != null) {
                mandat.setGueltig(state);
                try (SqlSession session = sqlSessionFactory.openSession()) {
                    MandateMapper mapper = session.getMapper(MandateMapper.class);
                    mapper.updateMandat(mandat);
                    session.commit();
                    mandateModel.reload();
                }
            }
        }
    }

    /**
     * Öffnet ein Fenster, in dem das ausgewählte Mandat bearbeitet werden kann.
     */
    private class BearbeitenAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            BeitragSepaMandat mandat = mandateModel.getMandatAtRow(mandateTable
                    .getSelectedRow());
            if (mandat != null) {
                MandatBearbeitenWindow win = new MandatBearbeitenWindow(
                        sqlSessionFactory, mandat);
                win.setVisible(true);
            }
        }
    }

    /**
     * Markiert das ausgewählte Mandat als das Aktive für das ausgewählte
     * Mitglied.
     */
    private class AktivAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try (SqlSession session = sqlSessionFactory.openSession()) {
                MandateMapper mapper = session.getMapper(MandateMapper.class);
                int mandatId = mandateModel.getIdAtRow(mandateTable
                        .getSelectedRow());
                int mitgliedId = mitgliederModel.getIdAtRow(mitgliederTable
                        .getSelectedRow());
                mapper.setAktivesMandat(mandatId, mitgliedId);
                session.commit();
                mitgliederModel.reload();
            }
        }
    }

    /**
     * Löscht das aktive Mandat beim ausgewählten Mitglied. In der Datenbank
     * wird im entsprechenden Feld NULL gespeichert, d.h. dem Mitglied ist
     * anschließend kein aktives Mandat mehr zugewiesen, es können also keine
     * Lastschriften eingezogen werden.
     */
    private class InaktivAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try (SqlSession session = sqlSessionFactory.openSession()) {
                MandateMapper mapper = session.getMapper(MandateMapper.class);
                int mitgliedId = mitgliederModel.getIdAtRow(mitgliederTable
                        .getSelectedRow());
                mapper.setAktivesMandat(null, mitgliedId);
                session.commit();
                mitgliederModel.reload();
            }
        }
    }

    /**
     * Öffnet ein Fenster, in dem ein Mitglied ausgewählt werden kann, das dann
     * dem Mandat hinzugefügt wird.
     */
    private class AddMitgliedAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Window parent = SwingUtilities.getWindowAncestor(MandateVerwaltenComponent.this);
            MitgliedSelectDialog dlg = new MitgliedSelectDialog(
                    parent, sqlSessionFactory);
            dlg.setVisible(true);
            int mitgliedId = dlg.getChosenMglId();

            if (mitgliedId == -1) {
                return;
            }

            try (SqlSession session = sqlSessionFactory.openSession()) {
                MandateMapper mapper = session.getMapper(MandateMapper.class);
                int mandatId = mandateModel.getIdAtRow(mandateTable
                        .getSelectedRow());
                mapper.addMitgliedForMandat(mandatId, mitgliedId);
                session.commit();
                mitgliederModel.reload();
            }
        }
    }

    /**
     * Stellt eine Liste von Mandaten bereit, die einem vorgegebenen Mitglied
     * zugeordnet sind.
     */
    private class MandateTableModel extends AbstractTableModel {
        private static final long serialVersionUID = -8838913454158676762L;

        private static final int ID_COLUMN_INDEX = 0;
        private static final int KONTOINH_COLUMN_INDEX = 1;
        private static final int IBAN_COLUMN_INDEX = 2;
        private static final int BIC_COLUMN_INDEX = 3;
        private static final int DATUM_COLUMN_INDEX = 4;
        private static final int GUELTIG_COLUMN_INDEX = 5;

        private MandateFilterType filterTyp = MandateFilterType.MITGLIED;
        private int filterId = -1;
        private ArrayList<BeitragSepaMandat> mandate = null;

        /**
         * Füllt die Mandate-Liste mit den Mandaten eines Mitglieds bzw. mit
         * einem bestimmten Mandat.
         *
         * @param newTyp
         *            Typ des Filters (nach Mitgliedsnummer oder Mandatsnummer)
         * @param newId
         *            ID, nach der gefiltert wird
         */
        public void loadMandate(MandateFilterType newTyp, int newId) {
            // Testen, ob sich die ID tatsächlich geändert hat
            if (filterTyp != newTyp || this.filterId != newId) {
                this.filterTyp = newTyp;
                this.filterId = newId;
                reload();
            }
        }

        /**
         * Lädt die Liste neu aus der Datenbank (wird nach Änderungen aufgerufen).
         */
        public void reload() {
            if (filterId == -1) {
                mandate = null;
            } else {
                try (SqlSession session = sqlSessionFactory.openSession()) {
                    MandateMapper mapper = session.getMapper(MandateMapper.class);
                    switch (filterTyp) {
                        case MITGLIED:
                            // Mandate für Mitglied aus Datenbank laden
                            mandate = mapper.findMandateByMitglied(filterId);
                            break;
                        case MANDAT:
                            // Mandat aus Datenbank laden
                            mandate = new ArrayList<>(
                                    Collections.singletonList(mapper.getMandat(filterId)));
                            break;
                    }
                }
            }
            fireTableDataChanged();
            // Aktuellstes Mandat auswählen und entsprechende Mitglieder/Lastschriften laden
            if (mandate != null && mandate.size() >= 1) {
                mandateTable.setRowSelectionInterval(mandate.size() - 1, mandate.size() - 1);
                int mandatId = mandate.get(mandate.size() - 1).getMandatId();
                mitgliederModel.loadMitglieder(mandatId);
                lastschriftenModel.loadLastschriften(mandatId);
            }
        }

        @Override
        public int getRowCount() {
            if (mandate == null) {
                return 0;
            } else {
                return mandate.size();
            }
        }

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case ID_COLUMN_INDEX:
                    return "ID";
                case KONTOINH_COLUMN_INDEX:
                    return "Kontoinhaber";
                case IBAN_COLUMN_INDEX:
                    return "IBAN";
                case BIC_COLUMN_INDEX:
                    return "BIC";
                case DATUM_COLUMN_INDEX:
                    return "Datum";
                case GUELTIG_COLUMN_INDEX:
                    return "Gültig";
                default:
                    return super.getColumnName(column);
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == GUELTIG_COLUMN_INDEX) {
                return Boolean.class;
            } else {
                return super.getColumnClass(columnIndex);
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (mandate == null || rowIndex >= mandate.size()) {
                return null;
            }

            BeitragSepaMandat mandat = mandate.get(rowIndex);
            switch (columnIndex) {
                case ID_COLUMN_INDEX:
                    return mandat.getMandatId();
                case KONTOINH_COLUMN_INDEX:
                    return mandat.getKontoinhaber();
                case IBAN_COLUMN_INDEX:
                    return mandat.getIban();
                case BIC_COLUMN_INDEX:
                    return mandat.getBic();
                case DATUM_COLUMN_INDEX:
                    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
                    return formatter.format(mandat.getDatum());
                case GUELTIG_COLUMN_INDEX:
                    return mandat.isGueltig();
                default:
                    return null;
            }
        }

        public int getIdAtRow(int rowIndex) {
            if (mandate == null || rowIndex < 0 || rowIndex >= mandate.size()) {
                return -1;
            } else {
                return mandate.get(rowIndex).getMandatId();
            }
        }

        public BeitragSepaMandat getMandatAtRow(int rowIndex) {
            if (mandate == null || rowIndex < 0 || rowIndex >= mandate.size()) {
                return null;
            } else {
                return mandate.get(rowIndex);
            }
        }

    }

    /**
     * Stellt eine Liste von Mitgliedern bereit, die einem vorgegebenen Mandat
     * zugeordnet sind.
     */
    private class MitgliederTableModel extends AbstractTableModel {
        private static final long serialVersionUID = -3924791317378669299L;

        private int mandatId = -1;
        private ArrayList<BeitragMitglied> mitglieder = null;

        private static final int NACHNAME_COLUMN_INDEX = 0;
        private static final int VORNAME_COLUMN_INDEX = 1;
        private static final int MITGLIEDSNUMMER_COLUMN_INDEX = 2;
        private static final int AKTIV_COLUMN_INDEX = 3;

        /**
         * Füllt die Mitglieder-Liste mit den Mitgliedern, die einem bestimmten
         * Mandat zugeordnet sind.
         *
         * @param newMandatId
         *            ID des Mandats
         */
        public void loadMitglieder(int newMandatId) {
            if (newMandatId != this.mandatId) {
                this.mandatId = newMandatId;
                reload();
            }
        }

        /**
         * Lädt die Liste neu aus der Datenbank (wird nach Änderungen
         * aufgerufen).
         */
        public void reload() {
            if (mandatId == -1) {
                mitglieder = null;
            } else {
                // Mandate für Mitglied aus Datenbank laden
                try (SqlSession session = sqlSessionFactory.openSession()) {
                    MandateMapper mapper = session
                            .getMapper(MandateMapper.class);
                    mitglieder = mapper.findMitgliederByMandat(mandatId);
                }
            }
            fireTableDataChanged();
        }

        /**
         * Leert die Liste (es wird dann also eine leere Tabelle angezeigt).
         */
        public void clear() {
            mandatId = -1;
            reload();
        }

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
            return 4;
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
                case AKTIV_COLUMN_INDEX:
                    if (mitglied.getAktivesMandat() == null) {
                        return false;
                    } else {
                        return ((mitglied.getAktivesMandat()) == mandatId);
                    }
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
                case AKTIV_COLUMN_INDEX:
                    return "Aktiv";
                default:
                    return "";
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == AKTIV_COLUMN_INDEX) {
                return Boolean.class;
            } else {
                return super.getColumnClass(columnIndex);
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
            if (mitglieder == null || rowIndex < 0
                    || rowIndex >= mitglieder.size()) {
                return null;
            }

            return mitglieder.get(rowIndex);
        }
    }


    /**
     * Stellt eine Liste von Mitgliedern bereit, die einem vorgegebenen Mandat
     * zugeordnet sind.
     */
    private class LastschriftenTableModel extends AbstractTableModel {

        private int mandatId = -1;
        private ArrayList<MandateMapper.DataLastschriftSammellastschrift> lastschriften = null;

        private static final int FAELLIGKEIT_COLUMN_INDEX = 0;
        private static final int VERWENDUNGSZWECK_COLUMN_INDEX = 1;
        private static final int BETRAG_COLUMN_INDEX = 2;

        /**
         * Füllt die Lastschriften-Liste mit den Lastschriften, die einem bestimmten
         * Mandat zugeordnet sind.
         *
         * @param newMandatId
         *            ID des Mandats
         */
        public void loadLastschriften(int newMandatId) {
            if (newMandatId != this.mandatId) {
                this.mandatId = newMandatId;
                reload();
            }
        }

        /**
         * Lädt die Liste neu aus der Datenbank (wird nach Änderungen
         * aufgerufen).
         */
        public void reload() {
            if (mandatId == -1) {
                lastschriften = null;
            } else {
                // Lastschriften für Mitglied aus Datenbank laden
                try (SqlSession session = sqlSessionFactory.openSession()) {
                    MandateMapper mapper = session
                            .getMapper(MandateMapper.class);
                    lastschriften = mapper.findLastschriftenByMandat(mandatId);
                }
            }
            fireTableDataChanged();
        }

        /**
         * Leert die Liste (es wird dann also eine leere Tabelle angezeigt).
         */
        public void clear() {
            mandatId = -1;
            reload();
        }

        @Override
        public int getRowCount() {
            if (lastschriften == null) {
                return 0;
            } else {
                return lastschriften.size();
            }
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (lastschriften == null || rowIndex >= lastschriften.size()) {
                return null;
            }

            MandateMapper.DataLastschriftSammellastschrift lastschrift = lastschriften.get(rowIndex);
            switch (columnIndex) {
                case FAELLIGKEIT_COLUMN_INDEX:
                    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
                    return formatter.format(lastschrift.getSammelLastschrift().getFaelligkeit());
                case VERWENDUNGSZWECK_COLUMN_INDEX:
                    return lastschrift.getLastschrift().getVerwendungszweck();
                case BETRAG_COLUMN_INDEX:
                    return lastschrift.getLastschrift().getBetrag().negate();
                default:
                    return null;
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case FAELLIGKEIT_COLUMN_INDEX:
                    return "Fälligkeit";
                case VERWENDUNGSZWECK_COLUMN_INDEX:
                    return "Verwendungszweck";
                case BETRAG_COLUMN_INDEX:
                    return "Betrag";
                default:
                    return "";
            }
        }
    }
}
