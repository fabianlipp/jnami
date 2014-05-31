package nami.beitrag.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import nami.beitrag.db.AbmeldungenMapper;
import nami.beitrag.db.AbmeldungenMapper.DataAbmeldungMitglied;
import nami.beitrag.db.BeitragAbmeldung;
import net.miginfocom.swing.MigLayout;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * Stellt ein Fenster dar, in dem vorgemerkte Abmeldungen angezeigt und als
 * erledigt markiert werden können.
 * 
 * @author Fabian Lipp
 * 
 */
public class AbmeldungenAnzeigenWindow extends JFrame {
    private static final long serialVersionUID = 7409328875312329467L;

    private SqlSessionFactory sqlSessionFactory;

    // Komponenten für Suche
    private JCheckBox chckbxUnbearbeitet;

    // Komponenten für Tabelle
    private JTable table;
    // Model der Tabelle
    private AbmeldungenModel tableModel;

    private static Logger logger = Logger
            .getLogger(AbmeldungenAnzeigenWindow.class.getName());

    /**
     * Erzeugt ein neues Rechnungs-Fenster.
     * 
     * @param sqlSessionFactory
     *            Zugriff auf die Datenbank
     */
    public AbmeldungenAnzeigenWindow(SqlSessionFactory sqlSessionFactory) {
        super("Abmeldungen anzeigen");
        this.sqlSessionFactory = sqlSessionFactory;

        buildFrame();
    }

    private void buildFrame() {
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new MigLayout("", "[grow]", "[][grow][][]"));

        /*** Buchungs-Suche ***/
        contentPane.add(createSearchPanel(), "cell 0 0,grow");

        /*** Tabelle vorbereiten ***/
        JScrollPane scrollPane = new JScrollPane();
        contentPane.add(scrollPane, "cell 0 1,grow");

        tableModel = new AbmeldungenModel();
        table = new JTable(tableModel);
        // Verhindert, dass die Spalten neu initialisiert werden, wenn sich das
        // Model verändert (dabei gehen der TableCellRenderer und die
        // Spaltenbreiten verloren)
        table.setAutoCreateColumnsFromModel(false);
        // Reagiert auf die Auswahl einer Zeile durch den Benutzer
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scrollPane.setViewportView(table);

        /*** Aktions-Buttons ***/
        JPanel aktionenPanel = new JPanel();
        aktionenPanel.setBorder(new TitledBorder(null, "Aktionen",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        aktionenPanel.setLayout(new MigLayout("", "[][]", "[]"));
        contentPane.add(aktionenPanel, "cell 0 3,growx");

        JButton btnEingetragen = new JButton("In NaMi eingetragen");
        aktionenPanel.add(btnEingetragen, "cell 0 0,aligny top");
        btnEingetragen
                .addActionListener(new AbmeldungInNamiEingetragenListener());

        refreshTable();

        pack();
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel();
        searchPanel.setBorder(new TitledBorder(new LineBorder(new Color(184,
                207, 229)), "Abmeldungen suchen", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        searchPanel.setLayout(new MigLayout("", "[]", "[]"));

        chckbxUnbearbeitet = new JCheckBox("Nur unbearbeitete anzeigen");
        chckbxUnbearbeitet.addActionListener(new AbmeldungenSucheListener());
        searchPanel.add(chckbxUnbearbeitet, "cell 0 0");

        return searchPanel;
    }

    /**
     * Liefert die Abmeldung, die momentan in der Tabelle ausgewählt ist.
     * 
     * @return ausgewählte Abmeldung; <tt>null</tt>, falls keiner ausgewählt ist
     */
    private BeitragAbmeldung getSelectedAbmeldung() {
        int row = table.getSelectedRow();
        if (row != -1) {
            return tableModel.getAbmeldungAt(row);
        } else {
            return null;
        }
    }

    /**
     * Listet bei einer Änderung der Filterkriterien alle Briefe in der Tabelle
     * auf, die die eingegeben Kriterien erfüllen.
     */
    private class AbmeldungenSucheListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            refreshTable();
        }
    }

    /**
     * Lädt die in der Tabelle angezeigten Daten neu aus der Datenbank. Dabei
     * werden die Filterkriterien neu aus den Steuerelementen eingelesen.
     */
    private void refreshTable() {
        tableModel.reloadAbmeldungen(chckbxUnbearbeitet.isSelected());
    }

    /**
     * Trägt das aktuelle Datum in der Abmeldung nach und markiert sie damit als
     * eingetragen in NaMi.
     */
    private class AbmeldungInNamiEingetragenListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            BeitragAbmeldung selected = getSelectedAbmeldung();
            if (selected == null) {
                logger.warning("Keine Abmeldung ausgewählt.");
            }

            selected.setNamiEingetragen(new Date());
            SqlSession session = sqlSessionFactory.openSession();
            try {
                AbmeldungenMapper mapper = session
                        .getMapper(AbmeldungenMapper.class);
                mapper.updateAbmeldung(selected);
                session.commit();
            } finally {
                session.close();
            }
        }
    }

    /**
     * Stellt eine Liste von Briefen, die nach bestimmten Filterkriterien aus
     * der Datenbank geholt werden, in einer Tabelle dar.
     */
    private class AbmeldungenModel extends AbstractTableModel {
        private static final long serialVersionUID = -8344734930779903398L;

        // Angezeigte Briefe
        private ArrayList<DataAbmeldungMitglied> abmeldungen;

        private static final int ID_COLUMN_INDEX = 0;
        private static final int MITGLIEDSNR_COLUMN_INDEX = 1;
        private static final int NAME_COLUMN_INDEX = 2;
        private static final int VORNAME_COLUMN_INDEX = 3;
        private static final int DATUM_COLUMN_INDEX = 4;
        private static final int FAELLIGKEIT_COLUMN_INDEX = 5;
        private static final int TYP_COLUMN_INDEX = 6;
        private static final int NAMIEINGETRAGEN_COLUMN_INDEX = 7;

        /**
         * Ersetzt die momentan angezeigten Abmeldungen durch eine neue Liste.
         * 
         * @param unbearbeitetOnly
         *            falls <tt>true</tt>, werden nur Abmeldungen angezeigt, die
         *            noch nicht in NaMi eingetragen sind
         */
        private void reloadAbmeldungen(boolean unbearbeitetOnly) {
            SqlSession session = sqlSessionFactory.openSession();
            try {
                AbmeldungenMapper mapper = session
                        .getMapper(AbmeldungenMapper.class);
                abmeldungen = mapper.findAbmeldungen(unbearbeitetOnly);
                fireTableDataChanged();
            } finally {
                session.close();
            }
        }

        @Override
        public int getRowCount() {
            if (abmeldungen != null) {
                return abmeldungen.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 8;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (abmeldungen == null || rowIndex >= abmeldungen.size()) {
                return null;
            }

            DataAbmeldungMitglied row = abmeldungen.get(rowIndex);
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
            switch (columnIndex) {
            case ID_COLUMN_INDEX:
                return row.getAbmeldung().getAbmeldungId();
            case MITGLIEDSNR_COLUMN_INDEX:
                return row.getMitglied().getMitgliedsnummer();
            case NAME_COLUMN_INDEX:
                return row.getMitglied().getNachname();
            case VORNAME_COLUMN_INDEX:
                return row.getMitglied().getVorname();
            case DATUM_COLUMN_INDEX:
                if (row.getAbmeldung().getDatum() == null) {
                    return "";
                } else {
                    return formatter.format(row.getAbmeldung().getDatum());
                }
            case FAELLIGKEIT_COLUMN_INDEX:
                if (row.getAbmeldung().getFaelligkeit() == null) {
                    return "";
                } else {
                    return formatter
                            .format(row.getAbmeldung().getFaelligkeit());
                }
            case TYP_COLUMN_INDEX:
                return row.getAbmeldung().getTyp();
            case NAMIEINGETRAGEN_COLUMN_INDEX:
                if (row.getAbmeldung().getNamiEingetragen() == null) {
                    return "";
                } else {
                    return formatter.format(row.getAbmeldung()
                            .getNamiEingetragen());
                }
            default:
                return null;
            }
        }

        @Override
        public String getColumnName(int column) {

            switch (column) {
            case ID_COLUMN_INDEX:
                return "ID";
            case MITGLIEDSNR_COLUMN_INDEX:
                return "Mitglieds-Nr.";
            case NAME_COLUMN_INDEX:
                return "Name";
            case VORNAME_COLUMN_INDEX:
                return "Vorname";
            case DATUM_COLUMN_INDEX:
                return "Datum";
            case FAELLIGKEIT_COLUMN_INDEX:
                return "Fälligkeit";
            case TYP_COLUMN_INDEX:
                return "Typ";
            case NAMIEINGETRAGEN_COLUMN_INDEX:
                return "in NaMi eingetragen";
            default:
                return null;
            }
        }

        /**
         * Liefert die Abmeldung, der in einer bestimmten Zeile angezeigt wird.
         * 
         * @param rowIndex
         *            gesuchte Zeile
         * @return Abmeldung in der Zeile
         */
        public BeitragAbmeldung getAbmeldungAt(int rowIndex) {
            if (abmeldungen == null || rowIndex >= abmeldungen.size()) {
                return null;
            }
            return abmeldungen.get(rowIndex).getAbmeldung();
        }
    }
}
