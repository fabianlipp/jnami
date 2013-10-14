package nami.beitrag.gui;

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import nami.beitrag.db.BeitragBuchung;
import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.ZeitraumSaldo;
import nami.connector.Halbjahr;
import net.miginfocom.swing.MigLayout;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * Stellt ein Fenster bereit, mit dem das Beitragskonto eines Mitglieds
 * abgefragt werden kann.
 * 
 * Im Fenster wird ein Mitglied ausgewählt, dessen Beitragsdaten angezeigt
 * werden. Das Fenster stellt zwei Tabs bereit:
 * <ul>
 * <li>Übersicht: zeigt die Summe aller Buchungen für jedes Halbjahr an</li>
 * <li>Details: zeigt alle Buchungen in einem bestimmten Halbjahr an</li>
 * </ul>
 * 
 * @author Fabian Lipp
 * 
 */
public class BeitragskontoWindow extends JFrame {
    private static final long serialVersionUID = 4398977549939312811L;

    private SqlSessionFactory sessionFactory;

    private MitgliedSelectComponent mitgliedSelect;
    private JTabbedPane tabbedPane;

    // Übersichts-Tab
    private JScrollPane overviewTablePane;
    private JTable overviewTable;
    private OverviewTableModel overviewTableModel;
    // aktuell im Übersicht-Tab angezeigte Daten
    private int overviewShownId;

    // Details-Tab
    private JPanel detailsPanel;
    private HalbjahrComponent halbjahrSelect;
    private JScrollPane detailsTablePane;
    private JTable detailsTable;
    // aktuell im Details-Tab angezeigte Daten
    private Halbjahr detailsShownHalbjahr;
    private int detailsShownId;

    /**
     * Erzeugt ein neues Beitragskonto-Fenster.
     * 
     * @param sqlSessionFactory
     *            Verbindung zur Datenbank
     */
    public BeitragskontoWindow(SqlSessionFactory sqlSessionFactory) {
        this.sessionFactory = sqlSessionFactory;
        buildFrame();
    }

    private void buildFrame() {
        ChangeListener refreshListener = new RefreshListener();

        getContentPane().setLayout(new MigLayout("", "[grow]", "[][grow]"));

        mitgliedSelect = new MitgliedSelectComponent(sessionFactory);
        mitgliedSelect.addChangeListener(refreshListener);
        JLabel lblMitglied = new JLabel("Mitglied:");
        lblMitglied.setLabelFor(mitgliedSelect);
        lblMitglied.setDisplayedMnemonic('m');
        getContentPane().add(lblMitglied, "flowx,cell 0 0");
        getContentPane().add(mitgliedSelect, "cell 0 0,alignx left,aligny top");

        // Overview-Tab
        overviewTableModel = new OverviewTableModel();
        overviewTable = new JTable(overviewTableModel);
        // verhindert, dass die Spaltenbreiten beim Austausch des Models
        // zurückgesetzt werden
        overviewTable.setAutoCreateColumnsFromModel(false);
        overviewTable.addMouseListener(new OverviewTableClickListener());
        overviewTablePane = new JScrollPane();
        overviewTablePane.setViewportView(overviewTable);

        // Details-Tab
        detailsPanel = new JPanel();
        detailsPanel.setLayout(new MigLayout("", "[grow]", "[][grow]"));

        halbjahrSelect = new HalbjahrComponent();
        halbjahrSelect.addChangeListener(refreshListener);
        JLabel lblHalbjahr = new JLabel("Halbjahr:");
        lblHalbjahr.setLabelFor(halbjahrSelect);
        lblHalbjahr.setDisplayedMnemonic('h');
        detailsPanel.add(lblHalbjahr, "flowx,cell 0 0");
        detailsPanel.add(halbjahrSelect, "cell 0 0,alignx left,aligny top");

        detailsTable = new JTable(new BuchungListTableModel());
        detailsTable.setAutoCreateColumnsFromModel(false);
        detailsTablePane = new JScrollPane();
        detailsTablePane.setViewportView(detailsTable);
        detailsPanel.add(detailsTablePane, "cell 0 1,grow");

        // TabbedPane zusammenstellen
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.addChangeListener(refreshListener);
        getContentPane().add(tabbedPane, "cell 0 1,grow");
        tabbedPane.addTab("Übersicht", null, overviewTablePane, null);
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_B);
        tabbedPane.addTab("Details", null, detailsPanel, null);
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_D);

        pack();
    }

    /**
     * Aktualisiert die Tabelle, wenn sich etwas an den Eingabefeldern
     * (Mitglied-ID, Halbjahr oder angezeigter Tab) ändert.
     */
    private class RefreshListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            if (tabbedPane.getSelectedComponent() == overviewTablePane) {
                refreshOverview();
            } else {
                refreshDetails();
            }
        }

    }

    /**
     * Aktualisiert die angezeigte Tabelle auf dem Übersichts-Tab, falls sich
     * die Mitglieds-ID geändert hat.
     */
    private void refreshOverview() {
        int mitgliedId = mitgliedSelect.getMitgliedId();

        // Mitglied has not changed -> nothing to do
        if (mitgliedId == overviewShownId) {
            return;
        }
        overviewShownId = mitgliedId;

        if (mitgliedId == -1) {
            overviewTableModel = new OverviewTableModel();
            overviewTable.setModel(overviewTableModel);
            return;
        }

        SqlSession session = sessionFactory.openSession();
        try {
            BeitragMapper mapper = session.getMapper(BeitragMapper.class);

            Collection<ZeitraumSaldo> results = mapper
                    .getSaldoPerHalbjahr(mitgliedId);

            overviewTableModel = new OverviewTableModel(results);
            overviewTable.setModel(overviewTableModel);
            if (results.size() > 0) {
                overviewTable.setRowSelectionInterval(0, 0);
            }
            overviewTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            overviewTable.addKeyListener(new OverviewTableKeyListener());

            // use TAB to give focus to next component instead of next
            // table cell
            overviewTable.setFocusTraversalKeys(
                    KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
            overviewTable.setFocusTraversalKeys(
                    KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

            overviewTable.requestFocusInWindow();
        } finally {
            session.close();
        }
    }

    /**
     * Zeigt die Details, wenn ein Halbjahr in der Tabelle auf der
     * Übersichtsseite durch Doppelklick ausgewählt wird.
     */
    private class OverviewTableClickListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (overviewTable != e.getComponent()) {
                throw new IllegalArgumentException(
                        "Used Listener on unexpected object");
            }

            if (e.getClickCount() == 2) {
                int row = overviewTable.rowAtPoint(e.getPoint());
                Halbjahr halbjahr = overviewTableModel.getHalbjahrAt(row);
                halbjahrSelect.setValue(halbjahr);
                tabbedPane.setSelectedComponent(detailsPanel);
            }
        }
    }

    /**
     * Erlaubt es ein Halbjahr durch Drücken der Eingabetaste in der Tabelle
     * auszuwählen und zeigt dann die entsprechenden Details (einzelne
     * Buchungen) an.
     */
    private class OverviewTableKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (overviewTable != e.getComponent()) {
                throw new IllegalArgumentException(
                        "Used Listener on unexpected object");
            }

            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                int row = overviewTable.getSelectedRow();
                if (row >= 0) {
                    Halbjahr halbjahr = overviewTableModel.getHalbjahrAt(row);
                    halbjahrSelect.setValue(halbjahr);
                    tabbedPane.setSelectedComponent(detailsPanel);
                }
            }
        }
    }

    /**
     * Stellt Halbjahre mit ihrem Buchungssaldo als TableModel dar. Die Zellen
     * der Tabelle können nicht bearbeitet werden.
     */
    private class OverviewTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 8639929360446497274L;
        private ArrayList<ZeitraumSaldo> salden;

        public OverviewTableModel() {
            this.salden = new ArrayList<>();
        }

        public OverviewTableModel(Collection<ZeitraumSaldo> salden) {
            this.salden = new ArrayList<>(salden);
        }

        @Override
        public int getRowCount() {
            return salden.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
            case 0:
                return "Halbjahr";
            case 1:
                return "Anzahl Buchungen";
            case 2:
                return "Saldo";
            default:
                return "";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ZeitraumSaldo saldo = salden.get(rowIndex);
            switch (columnIndex) {
            case 0:
                return saldo.getHalbjahr();
            case 1:
                return saldo.getAnzahlBuchungen();
            case 2:
                return saldo.getSaldo();
            default:
                return "";
            }
        }

        /**
         * Liefert das Halbjahr in einer bestimmten Zeile.
         * 
         * @param rowIndex
         *            Zeile der Tabelle
         * @return Halbjahr in dieser Zeile
         */
        public Halbjahr getHalbjahrAt(int rowIndex) {
            return salden.get(rowIndex).getHalbjahr();
        }
    }

    /**
     * Aktualisiert die angezeigte Tabelle auf dem Details-Tab, wenn sich
     * Mitglieds-ID oder Halbjahr geändert haben.
     */
    private void refreshDetails() {
        int mitgliedId = mitgliedSelect.getMitgliedId();
        Halbjahr halbjahr = halbjahrSelect.getValue();

        // Mitglied and Halbjahr has not changed -> nothing to do
        if (mitgliedId == detailsShownId
                && halbjahr.equals(detailsShownHalbjahr)) {
            return;
        }
        detailsShownId = mitgliedId;
        detailsShownHalbjahr = halbjahr;

        if (mitgliedId == -1) {
            detailsTable.setModel(new BuchungListTableModel());
            return;
        }

        SqlSession session = sessionFactory.openSession();
        try {
            BeitragMapper mapper = session.getMapper(BeitragMapper.class);

            Collection<BeitragBuchung> results = mapper.getBuchungenByHalbjahr(
                    halbjahr, mitgliedId);

            detailsTable.setModel(new BuchungListTableModel(results));
            if (results.size() > 0) {
                detailsTable.setRowSelectionInterval(0, 0);
            }
            detailsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            /*
             * table.addMouseListener(new TableClickListener());
             * table.addKeyListener(new TableKeyListener());
             */

            // use TAB to give focus to next component instead of next
            // table cell
            detailsTable.setFocusTraversalKeys(
                    KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
            detailsTable.setFocusTraversalKeys(
                    KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

            detailsTable.requestFocusInWindow();
        } finally {
            session.close();
        }
    }

    /**
     * Stellt eine Liste von Buchungen als TableModel dar. Die Zellen der
     * Tabelle können nicht bearbeitet werden.
     */
    private class BuchungListTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 8639929360446497274L;
        private List<BeitragBuchung> buchungen;

        public BuchungListTableModel() {
            this.buchungen = new ArrayList<>();
        }

        public BuchungListTableModel(Collection<BeitragBuchung> buchungen) {
            this.buchungen = new ArrayList<>(buchungen);
        }

        @Override
        public int getRowCount() {
            return buchungen.size();
        }

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
            case 0:
                return "ID";
            case 1:
                return "Vorausberechnung";
            case 2:
                return "Datum";
            case 3:
                return "Typ";
            case 4:
                return "Betrag";
            case 5:
                return "Kommentar";
            default:
                return "";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            BeitragBuchung buchung = buchungen.get(rowIndex);
            switch (columnIndex) {
            case 0:
                return buchung.getBuchungId();
            case 1:
                if (buchung.isVorausberechnung()) {
                    return "Ja";
                } else {
                    return "";
                }
            case 2:
                Date datum = buchung.getDatum();
                DateFormat formatter = DateFormat
                        .getDateInstance(DateFormat.MEDIUM);
                return formatter.format(datum);
            case 3:
                return buchung.getTyp();
            case 4:
                return buchung.getBetrag();
            case 5:
                return buchung.getKommentar();
            default:
                return "";
            }
        }

        /**
         * Liefert die ID der Buchung in einer bestimmten Zeile.
         * 
         * @param rowIndex
         *            Zeile der Tabelle
         * @return Buchung-ID in dieser Zeile
         */
        public int getIdAt(int rowIndex) {
            return buchungen.get(rowIndex).getBuchungId();
        }

    }
}
