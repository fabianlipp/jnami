package nami.beitrag.gui;

import java.awt.Component;
import java.awt.Font;
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
import javax.swing.table.DefaultTableCellRenderer;

import nami.beitrag.db.BeitragBuchung;
import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.BeitragMitglied;
import nami.beitrag.db.BeitragRechnung;
import nami.beitrag.db.RechnungenMapper;
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

    private final SqlSessionFactory sessionFactory;

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
    private JTable detailsTable;
    private BuchungListTableModel detailsTableModel;
    // aktuell im Details-Tab angezeigte Daten
    private Halbjahr detailsShownHalbjahr;
    private int detailsShownId;
    MandateVerwaltenComponent mandateVerwalten;

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
        detailsPanel.add(halbjahrSelect,
                "cell 0 0,alignx left,aligny top");
        JLabel lblLegende = new JLabel(
                "Kursiv: Vorausberechnung/Offene Rechnung");
        detailsPanel.add(lblLegende, "cell 0 0");

        detailsTable = new JTable(new BuchungListTableModel());
        detailsTable
                .setDefaultRenderer(Object.class, new BuchungCellRenderer());
        detailsTable.setAutoCreateColumnsFromModel(false);
        detailsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        detailsTable.addMouseListener(new BuchungListClickListener());
        detailsTable.addKeyListener(new BuchungListKeyListener());
        JScrollPane detailsTablePane = new JScrollPane();
        detailsTablePane.setViewportView(detailsTable);
        detailsPanel.add(detailsTablePane, "cell 0 1,grow");

        // SEPA-Mandat-Tab
        JPanel mandatePanel = new JPanel();
        mandatePanel.setLayout(new MigLayout("", "[grow]", "[grow]"));
        mandateVerwalten = new MandateVerwaltenComponent(
                sessionFactory);
        mandatePanel.add(mandateVerwalten, "cell 0 0");

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
        tabbedPane.addTab("SEPA-Mandate", null, mandatePanel, null);
        tabbedPane.setMnemonicAt(3, KeyEvent.VK_M);

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
            } else if (tabbedPane.getSelectedComponent() == detailsPanel){
                refreshDetails();
            } else {
                mandateVerwalten.changeMitglied(mitgliedSelect.getMitgliedId());
            }
        }

    }

    /**
     * Stellt ein Panel mit den Stammdaten eines Mitglieds bereit.
     */
    private final class StammdatenPanel extends JPanel {
        private static final long serialVersionUID = 7500743610141285252L;

        private int shownMitgliedId;

        private final JLabel mitgliedId;
        private final JLabel mitgliedsNummer;
        private final JLabel nachname;
        private final JLabel vorname;
        private final JLabel status;
        private final JLabel mitgliedstyp;
        private final JLabel beitragsart;
        private final JLabel eintrittsdatum;
        private final JLabel strasse;
        private final JLabel plz;
        private final JLabel ort;
        private final JLabel email;
        private final JLabel version;
        private final JCheckBox deleted;

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

            JLabel lblMitgliedstyp = new JLabel("Mitgliedstyp:");
            this.add(lblMitgliedstyp, "");
            mitgliedstyp = new JLabel("");
            this.add(mitgliedstyp, "wrap");

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
                mitgliedstyp.setText("");
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

            try (SqlSession session = sessionFactory.openSession()) {
                BeitragMapper mapper = session.getMapper(BeitragMapper.class);
                BeitragMitglied mgl = mapper.getMitglied(selMitgliedId);

                mitgliedId.setText(Integer.toString(mgl.getMitgliedId()));
                mitgliedsNummer.setText(Integer.toString(mgl
                        .getMitgliedsnummer()));
                nachname.setText(mgl.getNachname());
                vorname.setText(mgl.getVorname());
                if (mgl.getStatus() != null) {
                    status.setText(mgl.getStatus().toString());
                } else {
                    status.setText("");
                }
                mitgliedstyp.setText(mgl.getMitgliedstyp().toString());
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

        try (SqlSession session = sessionFactory.openSession()) {
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
    private static class OverviewTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 8639929360446497274L;
        private final ArrayList<ZeitraumSaldo> salden;

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

        try (SqlSession session = sessionFactory.openSession()) {
            BeitragMapper beitragMapper = session
                    .getMapper(BeitragMapper.class);
            RechnungenMapper rechnungMapper = session
                    .getMapper(RechnungenMapper.class);

            Collection<BeitragBuchung> buchungen = beitragMapper
                    .getBuchungenByHalbjahr(halbjahr, mitgliedId);
            Collection<BeitragRechnung> rechnungen = rechnungMapper
                    .getOffeneRechnungenByHalbjahr(halbjahr, mitgliedId);

            detailsTableModel = new BuchungListTableModel(buchungen, rechnungen);
            detailsTable.setModel(detailsTableModel);
            if (buchungen.size() > 0) {
                detailsTable.setRowSelectionInterval(0, 0);
            }

            // use TAB to give focus to next component instead of next
            // table cell
            detailsTable.setFocusTraversalKeys(
                    KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
            detailsTable.setFocusTraversalKeys(
                    KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

            detailsTable.requestFocusInWindow();
        }
    }

    /**
     * Stellt eine Liste von Buchungen als TableModel dar. Die Zellen der
     * Tabelle können nicht bearbeitet werden.
     */
    private static class BuchungListTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 8639929360446497274L;
        private final List<BeitragBuchung> buchungen;
        private final List<BeitragRechnung> rechnungen;

        private static final int ID_COLUMN_INDEX = 0;
        private static final int B_DATUM_COLUMN_INDEX = 1;
        private static final int B_TYP_COLUMN_INDEX = 2;
        private static final int B_BETRAG_COLUMN_INDEX = 3;
        private static final int B_KOMMENTAR_COLUMN_INDEX = 4;

        private static final int R_DATUM_COLUMN_INDEX = 1;
        private static final int R_FRIST_COLUMN_INDEX = 2;
        private static final int R_BETRAG_COLUMN_INDEX = 3;
        private static final int R_RECHNUNGSNUMMER_COLUMN_INDEX = 4;

        public BuchungListTableModel() {
            this.buchungen = new ArrayList<>();
            this.rechnungen = new ArrayList<>();
        }

        public BuchungListTableModel(Collection<BeitragBuchung> buchungen,
                Collection<BeitragRechnung> rechnungen) {
            this.buchungen = new ArrayList<>(buchungen);
            this.rechnungen = new ArrayList<>(rechnungen);
        }

        @Override
        public int getRowCount() {
            return buchungen.size() + rechnungen.size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
            case ID_COLUMN_INDEX:
                return "ID";
            case B_DATUM_COLUMN_INDEX:
                return "Datum";
            case B_TYP_COLUMN_INDEX:
                return "Typ/Frist";
            case B_BETRAG_COLUMN_INDEX:
                return "Betrag";
            case B_KOMMENTAR_COLUMN_INDEX:
                return "Kommentar";
            default:
                return "";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DateFormat formatter = DateFormat
                    .getDateInstance(DateFormat.MEDIUM);
            if (rowIndex >= buchungen.size()) {
                BeitragRechnung rechnung = rechnungen.get(rowIndex
                        - buchungen.size());
                switch (columnIndex) {
                case ID_COLUMN_INDEX:
                    return rechnung.getRechnungId();
                case R_DATUM_COLUMN_INDEX:
                    return formatter.format(rechnung.getDatum());
                case R_FRIST_COLUMN_INDEX:
                    return formatter.format(rechnung.getFrist());
                case R_BETRAG_COLUMN_INDEX:
                    return rechnung.getBetrag().negate();
                case R_RECHNUNGSNUMMER_COLUMN_INDEX:
                    return "Offene Rechnung "
                            + rechnung.getCompleteRechnungsNummer();
                default:
                    return "";
                }
            } else {
                BeitragBuchung buchung = buchungen.get(rowIndex);
                switch (columnIndex) {
                case ID_COLUMN_INDEX:
                    return buchung.getBuchungId();
                case B_DATUM_COLUMN_INDEX:
                    Date datum = buchung.getDatum();
                    return formatter.format(datum);
                case B_TYP_COLUMN_INDEX:
                    return buchung.getTyp();
                case B_BETRAG_COLUMN_INDEX:
                    return buchung.getBetrag();
                case B_KOMMENTAR_COLUMN_INDEX:
                    return buchung.getKommentar();
                default:
                    return "";
                }
            }
        }

        /**
         * Liefert den Datensatz in einer bestimmten Zeile.
         * 
         * @param rowIndex
         *            Zeile der Tabelle
         * @return Buchung oder Rechnung in dieser Zeile
         */
        public Object getDatensatzAt(int rowIndex) {
            if (rowIndex >= buchungen.size()) {
                return rechnungen.get(rowIndex - buchungen.size());
            } else {
                return buchungen.get(rowIndex);
            }
        }
    }

    /**
     * Formatiert die Zeilen in der Buchungstabelle. Zeilen, die Vorausbuchungen
     * oder offene Rechnungen beschreiben, werden kursiv dargestellt.
     */
    private class BuchungCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = -1601651481159570806L;

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            final Component c = super.getTableCellRendererComponent(table,
                    value, isSelected, hasFocus, row, column);
            Object datensatz = detailsTableModel.getDatensatzAt(row);
            boolean highlight = false;
            if (datensatz instanceof BeitragRechnung) {
                highlight = true;
            } else if (datensatz instanceof BeitragBuchung) {
                BeitragBuchung buchung = (BeitragBuchung) datensatz;
                if (buchung.isVorausberechnung()) {
                    highlight = true;
                }
            }

            if (highlight) {
                Font f = c.getFont();
                c.setFont(f.deriveFont(Font.ITALIC));
            }

            return c;
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
                showBuchungDialogFromDetailsTable(row);
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
                    showBuchungDialogFromDetailsTable(row);
                }
            }
        }
    }

    private void showBuchungDialogFromDetailsTable(int row) {
        Object datensatz = detailsTableModel.getDatensatzAt(row);
        if (datensatz instanceof BeitragBuchung) {
            BeitragBuchung buchung = (BeitragBuchung) datensatz;
            BuchungDialog diag = new BuchungDialog(sessionFactory, buchung);
            diag.setVisible(true);
        } else if (!(datensatz instanceof BeitragRechnung) && datensatz != null) {
            throw new ClassCastException("Wrong class in table model");
        }
        // do nothing for BeitragRechnung
    }
}
