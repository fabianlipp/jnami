package nami.beitrag.gui;

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.swing.JCheckBox;
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
import nami.beitrag.db.BeitragMitglied;
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
public class MitgliedAnzeigenWindow extends JFrame {
    private static final long serialVersionUID = 4398977549939312811L;

    private SqlSessionFactory sessionFactory;

    private MitgliedSelectComponent mitgliedSelect;
    private JTabbedPane tabbedPane;

    // Stammdaten-Tab
    private StammdatenPanel stammdatenPanel;

    // Beitragskonto-Tab
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
    private BuchungListTableModel detailsTableModel;
    // aktuell im Details-Tab angezeigte Daten
    private Halbjahr detailsShownHalbjahr;
    private int detailsShownId;

    /**
     * Erzeugt ein neues Beitragskonto-Fenster.
     * 
     * @param sqlSessionFactory
     *            Verbindung zur Datenbank
     */
    public MitgliedAnzeigenWindow(SqlSessionFactory sqlSessionFactory) {
        super("Mitglied anzeigen");
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

        // Stammdaten-Tab
        stammdatenPanel = new StammdatenPanel();

        // Beitragskonto-Tab
        overviewTableModel = new OverviewTableModel();
        overviewTable = new JTable(overviewTableModel);
        // verhindert, dass die Spaltenbreiten beim Austausch des Models
        // zurückgesetzt werden
        overviewTable.setAutoCreateColumnsFromModel(false);
        overviewTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        overviewTable.addMouseListener(new OverviewTableClickListener());
        overviewTable.addKeyListener(new OverviewTableKeyListener());
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
        detailsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        detailsTable.addMouseListener(new BuchungListClickListener());
        detailsTable.addKeyListener(new BuchungListKeyListener());
        detailsTablePane = new JScrollPane();
        detailsTablePane.setViewportView(detailsTable);
        detailsPanel.add(detailsTablePane, "cell 0 1,grow");

        // TabbedPane zusammenstellen
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.addChangeListener(refreshListener);
        getContentPane().add(tabbedPane, "cell 0 1,grow");
        tabbedPane.addTab("Stammdaten", null, stammdatenPanel, null);
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_S);
        tabbedPane.addTab("Beitragskonto", null, overviewTablePane, null);
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_B);
        tabbedPane.addTab("Details", null, detailsPanel, null);
        tabbedPane.setMnemonicAt(2, KeyEvent.VK_D);

        pack();
    }

    /**
     * Aktualisiert die Tabelle, wenn sich etwas an den Eingabefeldern
     * (Mitglied-ID, Halbjahr oder angezeigter Tab) ändert.
     */
    private class RefreshListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            if (tabbedPane.getSelectedComponent() == stammdatenPanel) {
                stammdatenPanel.refreshStammdaten();
            } else if (tabbedPane.getSelectedComponent() == overviewTablePane) {
                refreshOverview();
            } else {
                refreshDetails();
            }
        }

    }

    /**
     * Stellt ein Panel mit den Stammdaten eines Mitglieds bereit.
     */
    private final class StammdatenPanel extends JPanel {
        private static final long serialVersionUID = 7500743610141285252L;

        private int shownMitgliedId;

        private JLabel mitgliedId;
        private JLabel mitgliedsNummer;
        private JLabel nachname;
        private JLabel vorname;
        private JLabel status;
        private JLabel beitragsart;
        private JLabel eintrittsdatum;
        private JLabel strasse;
        private JLabel plz;
        private JLabel ort;
        private JLabel email;
        private JLabel version;
        private JCheckBox deleted;

        private StammdatenPanel() {
            this.setLayout(new MigLayout("", "[][grow]", ""));

            JLabel lblMitgliedId = new JLabel("Mitglied-ID:");
            this.add(lblMitgliedId, "");
            mitgliedId = new JLabel("");
            this.add(mitgliedId, "wrap");

            JLabel lblMitgliedsNummer = new JLabel("Mitgliedsnummer:");
            this.add(lblMitgliedsNummer, "");
            mitgliedsNummer = new JLabel("");
            this.add(mitgliedsNummer, "wrap");

            JLabel lblNachname = new JLabel("Nachname:");
            this.add(lblNachname, "");
            nachname = new JLabel("");
            this.add(nachname, "wrap");

            JLabel lblVorname = new JLabel("Vorname:");
            this.add(lblVorname, "");
            vorname = new JLabel("");
            this.add(vorname, "wrap");

            JLabel lblStatus = new JLabel("Status:");
            this.add(lblStatus, "");
            status = new JLabel("");
            this.add(status, "wrap");

            JLabel lblBeitragsart = new JLabel("Beitragsart:");
            this.add(lblBeitragsart, "");
            beitragsart = new JLabel("");
            this.add(beitragsart, "wrap");

            JLabel lblEintrittsdatum = new JLabel("Eintrittsdatum:");
            this.add(lblEintrittsdatum, "");
            eintrittsdatum = new JLabel("");
            this.add(eintrittsdatum, "wrap");

            JLabel lblStrasse = new JLabel("Straße:");
            this.add(lblStrasse, "");
            strasse = new JLabel("");
            this.add(strasse, "wrap");

            JLabel lblPlz = new JLabel("PLZ:");
            this.add(lblPlz, "");
            plz = new JLabel("");
            this.add(plz, "wrap");

            JLabel lblOrt = new JLabel("Ort:");
            this.add(lblOrt, "");
            ort = new JLabel("");
            this.add(ort, "wrap");

            JLabel lblEmail = new JLabel("E-Mail:");
            this.add(lblEmail, "");
            email = new JLabel("");
            this.add(email, "wrap");

            JLabel lblVersion = new JLabel("Version:");
            this.add(lblVersion, "");
            version = new JLabel("");
            this.add(version, "wrap");

            JLabel lblDeleted = new JLabel("Gelöscht in NaMi:");
            this.add(lblDeleted, "");
            deleted = new JCheckBox();
            deleted.setEnabled(false);
            this.add(deleted, "wrap");

            pack();
        }

        private void refreshStammdaten() {
            int selMitgliedId = mitgliedSelect.getMitgliedId();
            // Mitglied has not changed -> nothing to do
            if (selMitgliedId == shownMitgliedId) {
                return;
            }
            shownMitgliedId = selMitgliedId;

            if (selMitgliedId == -1) {
                mitgliedId.setText("");
                mitgliedsNummer.setText("");
                nachname.setText("");
                vorname.setText("");
                status.setText("");
                beitragsart.setText("");
                eintrittsdatum.setText("");
                strasse.setText("");
                plz.setText("");
                ort.setText("");
                email.setText("");
                version.setText("");
                deleted.setSelected(false);
                return;
            }

            SqlSession session = sessionFactory.openSession();
            try {
                BeitragMapper mapper = session.getMapper(BeitragMapper.class);
                BeitragMitglied mgl = mapper.getMitglied(selMitgliedId);

                mitgliedId.setText(Integer.toString(mgl.getMitgliedId()));
                mitgliedsNummer.setText(Integer.toString(mgl
                        .getMitgliedsnummer()));
                nachname.setText(mgl.getNachname());
                vorname.setText(mgl.getVorname());
                status.setText(mgl.getStatus().toString());
                beitragsart.setText(mgl.getBeitragsart().toString());
                String datum = new SimpleDateFormat("dd.MM.yyyy").format(mgl
                        .getEintrittsdatum());
                eintrittsdatum.setText(datum);
                strasse.setText(mgl.getStrasse());
                plz.setText(mgl.getPlz());
                ort.setText(mgl.getOrt());
                email.setText(mgl.getEmail());
                version.setText(Integer.toString(mgl.getVersion()));
                deleted.setSelected(mgl.isDeleted());
            } finally {
                session.close();
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
            detailsTableModel = new BuchungListTableModel();
            detailsTable.setModel(detailsTableModel);
            return;
        }

        SqlSession session = sessionFactory.openSession();
        try {
            BeitragMapper mapper = session.getMapper(BeitragMapper.class);

            Collection<BeitragBuchung> results = mapper.getBuchungenByHalbjahr(
                    halbjahr, mitgliedId);

            detailsTableModel = new BuchungListTableModel(results);
            detailsTable.setModel(detailsTableModel);
            if (results.size() > 0) {
                detailsTable.setRowSelectionInterval(0, 0);
            }

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
         * Liefert die Buchung in einer bestimmten Zeile.
         * 
         * @param rowIndex
         *            Zeile der Tabelle
         * @return Buchung in dieser Zeile
         */
        public BeitragBuchung getBuchungAt(int rowIndex) {
            return buchungen.get(rowIndex);
        }

    }

    /**
     * Zeigt alle Daten einer Buchung an, wenn sie durch einen Doppelklick in
     * der Tabelle ausgewählt wird.
     */
    private class BuchungListClickListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (detailsTable != e.getComponent()) {
                throw new IllegalArgumentException(
                        "Used Listener on unexpected object");
            }

            if (e.getClickCount() == 2) {
                int row = detailsTable.rowAtPoint(e.getPoint());
                BeitragBuchung buchung = detailsTableModel.getBuchungAt(row);
                BuchungDialog diag = new BuchungDialog(sessionFactory, buchung);
                diag.setVisible(true);
            }
        }
    }

    /**
     * Zeigt alle Daten einer Buchung an, die durch die Enter-Taste in der
     * Tabelle ausgewählt wird.
     */
    private class BuchungListKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (detailsTable != e.getComponent()) {
                throw new IllegalArgumentException(
                        "Used Listener on unexpected object");
            }

            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                int row = detailsTable.getSelectedRow();
                if (row >= 0) {
                    BeitragBuchung buchung = detailsTableModel
                            .getBuchungAt(row);
                    BuchungDialog diag = new BuchungDialog(sessionFactory,
                            buchung);
                    diag.setVisible(true);
                }
            }
        }
    }
}