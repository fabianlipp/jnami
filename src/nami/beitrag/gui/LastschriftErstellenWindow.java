package nami.beitrag.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import nami.beitrag.db.BeitragLastschrift;
import nami.beitrag.db.BeitragMitglied;
import nami.beitrag.db.BeitragRechnung;
import nami.beitrag.db.BeitragSammelLastschrift;
import nami.beitrag.db.BeitragSepaMandat;
import nami.beitrag.db.LastschriftenMapper;
import nami.beitrag.db.LastschriftenMapper.DataMandateRechnungen;
import nami.beitrag.db.LastschriftenMapper.DataRechnungMitglied;
import nami.beitrag.db.LastschriftenMapper.FilterSettings;
import nami.beitrag.gui.utils.Colors;
import nami.beitrag.gui.utils.DisabledCellRenderer;
import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import com.toedter.calendar.JDateChooser;

/**
 * Stellt ein Fenster dar, in dem Sammellastschriften zusammengestellt werden
 * können. Dazu werden nach bestimmten Kriterien offene Rechnungen aus der
 * Datenbank geholt und angezeigt. Der Benutzer kann manuell Rechnungen
 * deaktivieren und lässt anschließend die Sammellastschrift erstellen.
 * 
 * @author Fabian Lipp
 * 
 */
public class LastschriftErstellenWindow extends JFrame {
    private static final long serialVersionUID = 7409328875312329467L;

    private SqlSessionFactory sqlSessionFactory;

    // Komponenten für Suche
    private JCheckBox chckbxRechnungsdatum;
    private JDateChooser inputRechnungsdatum;
    private JCheckBox chckbxBereitsErstellt;

    // Komponenten für Tabelle
    private JXTreeTable treeTable;
    // Model der Tabelle
    private LastschriftTreeTableModel treeTableModel;
    // Mandat, dessen Zeile momentan in der Tabelle ausgewählt ist (null, falls
    // keine Zeile oder kein Mandat ausgewählt ist)
    private MandatNode selectedMandat;

    // Komponenten für "Lastschrift erstellen"
    private JDateChooser inputFaelligkeit;
    private JTextField inputBezeichnung;
    private JRadioButton rdbtnMitgliedernamen;
    private JRadioButton rdbtnRechnungsnummer;

    /**
     * Erzeugt ein neues Rechnungs-Fenster.
     * 
     * @param sqlSessionFactory
     *            Zugriff auf die Datenbank
     */
    public LastschriftErstellenWindow(SqlSessionFactory sqlSessionFactory) {
        super("Sammellastschrift erstellen");
        this.sqlSessionFactory = sqlSessionFactory;
        buildFrame();
    }

    private void buildFrame() {
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new MigLayout("", "[grow]", "[][grow][][]"));

        /*** Rechnungs-Suche ***/
        contentPane.add(createSearchPanel(), "cell 0 0,grow");

        /*** Tabelle vorbereiten ***/
        JScrollPane scrollPane = new JScrollPane();
        contentPane.add(scrollPane, "cell 0 1,grow");
        treeTable = new JXTreeTable(new LastschriftTreeTableModel());
        // Model initialisieren
        treeTableModel = new LastschriftTreeTableModel();
        treeTable.setTreeTableModel(treeTableModel);
        // Verhindert, dass die Spalten neu initialisiert werden, wenn sich das
        // Model verändert (dabei gehen der TableCellRenderer und die
        // Spaltenbreiten verloren)
        treeTable.setAutoCreateColumnsFromModel(false);
        // Setzt den Renderer, der dafür sorgt, dass Checkboxen, die nicht
        // bearbeitet werden können, disabled werden
        treeTable.getColumn(LastschriftTreeTableModel.CHECK_COLUMN_INDEX)
                .setCellRenderer(new DisabledCellRenderer());
        // Reagiert auf die Auswahl einer Zeile durch den Benutzer
        treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        treeTable.addTreeSelectionListener(new SelectMandatListener());
        scrollPane.setViewportView(treeTable);

        Highlighter high;
        // Färbt den Hintergrund aller Rechnungen ein
        high = new ColorHighlighter(new RechnungHighlightPredicate(),
                Colors.TT_CHILD_BG, null);
        treeTable.addHighlighter(high);
        // Färbt den Hintergrund der Rechnungen, die zum aktuell ausgewählten
        // Mandat gehören, ein
        high = new ColorHighlighter(new RechnungSelectedPredicate(),
                Colors.TT_SEL_BG, null);
        treeTable.addHighlighter(high);
        // Färbt den Text der abgewählten Mandate und Rechnungen ein
        high = new ColorHighlighter(new RowDeactivatedPredicate(), null,
                Colors.TT_DEACTIV_FG, null, Colors.TT_DEACTIV_FG);
        treeTable.addHighlighter(high);

        /*** Buttons unter der Tabelle ***/
        JPanel btnsBelow = new JPanel();
        btnsBelow.setBorder(new TitledBorder(null, "Alle Mandate",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPane.add(btnsBelow, "cell 0 2,growx");
        btnsBelow.setLayout(new MigLayout("", "[][grow]", "[]"));

        JButton btnAlleAusklappen = new JButton("Ausklappen");
        btnsBelow.add(btnAlleAusklappen, "cell 0 0,alignx left");
        btnAlleAusklappen.addActionListener(new AlleAusklappenListener());

        JButton btnAlleEinklappen = new JButton("Einklappen");
        btnsBelow.add(btnAlleEinklappen, "cell 0 0,alignx left");
        btnAlleEinklappen.addActionListener(new AlleEinklappenListener());

        JButton btnAuswaehlen = new JButton("Auswählen");
        btnsBelow.add(btnAuswaehlen, "cell 1 0,span,alignx right");
        btnAuswaehlen.addActionListener(new MandateSelectListener(true));

        JButton btnAbwaehlen = new JButton("Abwählen");
        btnsBelow.add(btnAbwaehlen, "cell 1 0,span,alignx right");
        btnAbwaehlen.addActionListener(new MandateSelectListener(false));

        /*** Lastschrift-Erstellungs-Einstellungen und -Button ***/
        contentPane.add(createErstellenPanel(), "cell 0 3,growx");

        pack();
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel();
        searchPanel.setBorder(new TitledBorder(null, "Rechnungen suchen",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        searchPanel.setLayout(new MigLayout("", "[][grow]", "[][grow][]"));

        JLabel lblEsWerdenNur = new JLabel(
                "<html>Es werden nur Rechnungen von Mitgliedern angezeigt, für die ein "
                        + "gültiges, aktives SEPA-Mandat vorhanden ist.");
        searchPanel.add(lblEsWerdenNur, "cell 0 0 2 1,wmax 100%");

        /*** Rechnungsdatum ***/
        chckbxRechnungsdatum = new JCheckBox("Rechnungsdatum:");
        chckbxRechnungsdatum
                .addItemListener(new RechnungsdatumCheckboxListener());
        searchPanel.add(chckbxRechnungsdatum, "flowx,cell 0 1");

        inputRechnungsdatum = new JDateChooser();
        inputRechnungsdatum.setEnabled(false);
        searchPanel.add(inputRechnungsdatum, "cell 1 1,growy");

        /*** Untere Zeile ***/
        JPanel sucheBottomPanel = new JPanel();
        searchPanel.add(sucheBottomPanel, "cell 0 2 2 1,growx");
        sucheBottomPanel.setLayout(new MigLayout("insets 0", "[][grow]", "[]"));

        chckbxBereitsErstellt = new JCheckBox(
                "Rechnungen, für die bereits eine Lastschrift erstellt wurde");
        sucheBottomPanel.add(chckbxBereitsErstellt,
                "cell 0 0,alignx left,growy");

        JButton btnSuchen = new JButton("Suchen");
        btnSuchen.addActionListener(new MandateSucheListener());
        sucheBottomPanel.add(btnSuchen, "cell 1 0,alignx right,aligny top");

        return searchPanel;
    }

    /**
     * Aktiviert das Eingabefeld für das Rechnungsdatum nur, wenn auch die
     * Checkbox aktiviert ist.
     */
    private class RechnungsdatumCheckboxListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            inputRechnungsdatum.setEnabled(chckbxRechnungsdatum.isSelected());
        }
    }

    private JPanel createErstellenPanel() {
        JPanel erstellenPanel = new JPanel();
        erstellenPanel.setBorder(new TitledBorder(null,
                "Sammellastschrift erstellen", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        erstellenPanel.setLayout(new MigLayout("", "[][grow]", "[][][][]"));

        // Fälligkeit
        JLabel lblFaelligkeit = new JLabel("Fälligkeit:");
        erstellenPanel.add(lblFaelligkeit, "cell 0 0");

        inputFaelligkeit = new JDateChooser();
        inputFaelligkeit.setDate(new Date());
        erstellenPanel.add(inputFaelligkeit, "cell 1 0,alignx left");

        // Bezeichnung
        JLabel lblBezeichnung = new JLabel("Bezeichnung:");
        erstellenPanel.add(lblBezeichnung, "cell 0 1,alignx left");

        inputBezeichnung = new JTextField();
        erstellenPanel.add(inputBezeichnung, "cell 1 1,growx");
        inputBezeichnung.setColumns(10);

        // Verwendungszweck
        JLabel lblVerwendungszweck = new JLabel("Verwendungszweck:");
        erstellenPanel.add(lblVerwendungszweck, "cell 0 2");

        rdbtnMitgliedernamen = new JRadioButton("Mitgliedernamen");
        rdbtnMitgliedernamen.setSelected(true);
        erstellenPanel.add(rdbtnMitgliedernamen, "flowx,cell 1 2");

        rdbtnRechnungsnummer = new JRadioButton("Rechnungsnummer");
        erstellenPanel.add(rdbtnRechnungsnummer, "cell 1 2");

        ButtonGroup verwendungszweckGrp = new ButtonGroup();
        verwendungszweckGrp.add(rdbtnMitgliedernamen);
        verwendungszweckGrp.add(rdbtnRechnungsnummer);

        // Button
        JButton btnLastschriftErzeugen = new JButton(
                "Sammellastschrift erstellen");
        btnLastschriftErzeugen
                .addActionListener(new LastschriftErzeugenListener());
        erstellenPanel.add(btnLastschriftErzeugen,
                "cell 0 3 2 1,alignx right,aligny top");

        return erstellenPanel;
    }

    /**
     * Listet alle Mandate und mögliche Rechnungen in der Tabelle auf, die die
     * eingegeben Kriterien erfüllen.
     */
    private class MandateSucheListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            FilterSettings filterSettings = new FilterSettings();

            if (chckbxRechnungsdatum.isSelected()) {
                filterSettings.setRechnungsdatum(inputRechnungsdatum.getDate());
            }

            filterSettings.setBereitsErstellt(chckbxBereitsErstellt
                    .isSelected());

            treeTableModel.reloadMandate(filterSettings);
        }
    }

    /**
     * Zeigt die Buchungen für alle Personen in der Tabelle an.
     */
    private class AlleAusklappenListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            treeTable.expandAll();
        }
    }

    /**
     * Blendet die Buchungen für alle Personen in der Tabelle aus.
     */
    private class AlleEinklappenListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            treeTable.collapseAll();
        }
    }

    /**
     * (De-)Selektiert alle Personen in der Tabelle.
     */
    private final class MandateSelectListener implements ActionListener {
        private boolean desiredState;

        private MandateSelectListener(boolean desiredState) {
            this.desiredState = desiredState;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (treeTableModel.root == null) {
                // es stehen keine Personen in der Tabelle
                return;
            }

            for (MandatNode mNode : treeTableModel.root.mandate) {
                mNode.checked = desiredState;
            }

            treeTable.repaint();
        }
    }

    /**
     * Erzeugt eine Sammellastschrift für die ausgewählten Personen und
     * Buchungen.
     */
    private class LastschriftErzeugenListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {

            if (treeTableModel.root == null) {
                // es stehen keine Mandate in der Tabelle
                return;
            }

            // Gibt an, ob schon eine (Einzel-)Lastschrift in die Datenbank
            // eingefügt wurde (andernfalls wird am Ende der Funktion die
            // Transaktion rückgängig gemacht, also auch keine
            // Sammellastschrift in der Datenbank gespeichert)
            boolean lastschriftEingefuegt = false;

            SqlSession session = sqlSessionFactory.openSession();
            try {
                LastschriftenMapper mapper = session
                        .getMapper(LastschriftenMapper.class);

                // Sammellastschrift einfügen
                BeitragSammelLastschrift sammelLastschrift;
                sammelLastschrift = new BeitragSammelLastschrift();
                sammelLastschrift.setFaelligkeit(inputFaelligkeit.getDate());
                sammelLastschrift.setAusgefuehrt(false);
                sammelLastschrift.setBezeichnung(inputBezeichnung.getText());
                mapper.insertSammelLastschrift(sammelLastschrift);
                int sammelLastschriftId = sammelLastschrift
                        .getSammelLastschriftId();

                for (MandatNode mNode : treeTableModel.root.mandate) {
                    if (!mNode.checked || mNode.rechnungen == null
                            || mNode.rechnungen.isEmpty()
                            || !mNode.hasCheckedRechnung()) {
                        // erzeuge keine Lastschrift
                        continue;
                    }
                    lastschriftEingefuegt = true;

                    // Verwendungszweck vorbereiten
                    TreeSet<String> vzweckStrings = new TreeSet<>();
                    for (RechnungNode rNode : mNode.rechnungen) {
                        if (rNode.checked) {
                            if (rdbtnRechnungsnummer.isSelected()) {
                                vzweckStrings.add(rNode.rechnung
                                        .getCompleteRechnungsNummer());
                            } else {
                                vzweckStrings.add(rNode.mitglied.getVorname()
                                        + " " + rNode.mitglied.getNachname());
                            }
                        }
                    }
                    String verwendungszweck = "DPSG-Beitrag "
                            + StringUtils.join(vzweckStrings, ", ");
                    verwendungszweck = replaceUmlaute(verwendungszweck);

                    // Lastschrift in Datenbank einfügen
                    BeitragLastschrift lastschrift = new BeitragLastschrift();
                    lastschrift.setSammelLastschriftId(sammelLastschriftId);
                    lastschrift.setMandatId(mNode.mandat.getMandatId());
                    lastschrift.setVerwendungszweck(verwendungszweck);
                    mapper.insertLastschrift(lastschrift);
                    int lastschriftId = lastschrift.getLastschriftId();

                    // Rechnungen dazu einfügen
                    for (RechnungNode rNode : mNode.rechnungen) {
                        if (rNode.checked) {
                            mapper.addRechnungToLastschrift(lastschriftId,
                                    rNode.rechnung.getRechnungId());
                        }
                    }
                }

                // Sammellastschrift wieder entfernen, wenn keine
                // Einzellastschrift eingefügt wurde
                if (lastschriftEingefuegt) {
                    session.commit();
                } else {
                    session.rollback();
                }
            } finally {
                session.close();
            }

            if (lastschriftEingefuegt) {
                dispose();
            }
        }
    }

    /**
     * Wählt alle Zeilen aus, die Rechnungen sind.
     */
    private class RechnungHighlightPredicate implements HighlightPredicate {
        @Override
        public boolean isHighlighted(Component renderer,
                ComponentAdapter adapter) {
            int row = adapter.convertRowIndexToModel(adapter.row);
            TreePath path = treeTable.getPathForRow(row);
            return (path.getLastPathComponent() instanceof RechnungNode);
        }
    }

    /**
     * Wählt alle Rechnungen aus, bei denen das zugeordnete Mandat vom Benutzer
     * ausgewählt wurde.
     */
    private class RechnungSelectedPredicate implements HighlightPredicate {
        @Override
        public boolean isHighlighted(Component renderer,
                ComponentAdapter adapter) {
            if (selectedMandat != null) {
                int row = adapter.convertRowIndexToModel(adapter.row);
                TreePath path = treeTable.getPathForRow(row);
                return (path.getPathComponent(1) == selectedMandat);
            }
            return false;
        }
    }

    /**
     * Wählt alle Zeilen der Tabelle aus, die deaktiviert sind. Dafür gibt es
     * folgende Ursachen:
     * <ol>
     * <li>Das zur Zeile gehörige Mandat ist deaktiviert</li>
     * <li>Die Zeile selbst ist deaktiviert</li>
     * <li>Die Zeile ist ein Mandat und alle zugehörigen Rechnungen sind
     * deaktiviert</li>
     * </ol>
     */
    private class RowDeactivatedPredicate implements HighlightPredicate {
        @Override
        public boolean isHighlighted(Component renderer,
                ComponentAdapter adapter) {
            int row = adapter.convertRowIndexToModel(adapter.row);
            TreePath path = treeTable.getPathForRow(row);
            if (path != null) {
                if (path.getPathCount() >= 2) {
                    Object lastComp = path.getLastPathComponent();

                    // Erfüllt, wenn entsprechendes Mandat deaktiviert ist
                    MandatNode mNode = (MandatNode) path.getPathComponent(1);
                    if (!mNode.checked) {
                        return true;
                    }

                    // Erfüllt, wenn dies eine Rechnung ist, die deaktiviert ist
                    if (lastComp instanceof RechnungNode) {
                        RechnungNode bNode = (RechnungNode) lastComp;
                        if (!bNode.checked) {
                            return true;
                        }
                    }

                    // Erfüllt, wenn dies ein Mandat ist und alle Rechnungen
                    // deaktiviert sind
                    if (lastComp instanceof MandatNode) {
                        mNode = (MandatNode) lastComp;
                        if (!mNode.hasCheckedRechnung()) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }
    }

    /**
     * Aktualisiert bei der Auswahl einer Person das entsprechende Feld (wird
     * dann zur Hervorhebung genutzt).
     */
    private class SelectMandatListener implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            if (e.getNewLeadSelectionPath() != null) {
                Object selected = e.getNewLeadSelectionPath()
                        .getLastPathComponent();
                if (selected instanceof MandatNode) {
                    selectedMandat = (MandatNode) selected;
                } else {
                    selectedMandat = null;
                }
            } else {
                selectedMandat = null;
            }

            // neu zeichnen, damit Highlighter neu ausgeführt werden (einer
            // davon beachtet selectedMandat)
            treeTable.repaint();
        }
    }

    /**
     * Das Model für die Lastschrift-Vorbereitungs-Tabelle.
     */
    private class LastschriftTreeTableModel extends AbstractTreeTableModel {

        // beschreibt die Wurzel des dargestellten Baumes
        private RootNode root = null;

        private static final int ID_MANDAT_COLUMN_INDEX = 0;
        private static final int KONTOINH_MANDAT_COLUMN_INDEX = 1;
        private static final int IBAN_MANDAT_COLUMN_INDEX = 2;
        private static final int BIC_MANDAT_COLUMN_INDEX = 3;

        private static final int NR_RECH_COLUMN_INDEX = 0;
        private static final int DATUM_RECH_COLUMN_INDEX = 1;
        private static final int NAME_RECH_COLUMN_INDEX = 2;
        private static final int BETRAG_RECH_COLUMN_INDEX = 3;

        private static final int CHECK_COLUMN_INDEX = 4;

        /**
         * Ersetzt die momentan angezeigten Mandate durch eine neue Liste. Diese
         * neue Liste wird durch die FilterSettings für die Datenbankabfrage
         * beschrieben.
         * 
         * @param filterSettings
         *            Parameter für die Datenbankabfrage
         */
        private void reloadMandate(FilterSettings filterSettings) {
            root = new RootNode(filterSettings);
            this.modelSupport.fireNewRoot();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            if (column == CHECK_COLUMN_INDEX) {
                return Boolean.class;
            }
            return super.getColumnClass(column);
        }

        @Override
        public boolean isCellEditable(Object node, int column) {
            // Die Buchungs-Checkboxen sind nicht bearbeitbar, wenn die
            // zugehörige Person deaktiviert ist
            if (column == CHECK_COLUMN_INDEX) {
                if (node instanceof RechnungNode) {
                    RechnungNode rNode = (RechnungNode) node;
                    if (!rNode.mandat.checked) {
                        return false;
                    }
                }
                return true;
            }

            return super.isCellEditable(node, column);
        }

        @Override
        public String getColumnName(int column) {
            if (column == CHECK_COLUMN_INDEX) {
                return "Ausgewählt";
            }

            String line1, line2;
            switch (column) {
            case ID_MANDAT_COLUMN_INDEX:
                line1 = "MandatsID";
                break;
            case IBAN_MANDAT_COLUMN_INDEX:
                line1 = "IBAN";
                break;
            case BIC_MANDAT_COLUMN_INDEX:
                line1 = "BIC";
                break;
            case KONTOINH_MANDAT_COLUMN_INDEX:
                line1 = "Kontoinhaber";
                break;
            default:
                line1 = "";
            }
            switch (column) {
            case NR_RECH_COLUMN_INDEX:
                line2 = "Rechnungsnummer";
                break;
            case DATUM_RECH_COLUMN_INDEX:
                line2 = "Datum";
                break;
            case NAME_RECH_COLUMN_INDEX:
                line2 = "Name";
                break;
            case BETRAG_RECH_COLUMN_INDEX:
                line2 = "Betrag";
                break;
            default:
                line2 = "";
            }
            return "<html>" + line1 + "<br>" + line2;

        }

        @Override
        public Object getValueAt(Object node, int column) {
            if (node instanceof MandatNode) {
                MandatNode mNode = (MandatNode) node;
                switch (column) {
                case ID_MANDAT_COLUMN_INDEX:
                    return mNode.mandat.getMandatId();
                case IBAN_MANDAT_COLUMN_INDEX:
                    return mNode.mandat.getIban();
                case BIC_MANDAT_COLUMN_INDEX:
                    return mNode.mandat.getBic();
                case KONTOINH_MANDAT_COLUMN_INDEX:
                    return mNode.mandat.getKontoinhaber();
                case CHECK_COLUMN_INDEX:
                    return mNode.checked;
                default:
                    return null;
                }
            } else if (node instanceof RechnungNode) {
                RechnungNode rNode = (RechnungNode) node;
                switch (column) {
                case NR_RECH_COLUMN_INDEX:
                    return rNode.rechnung.getCompleteRechnungsNummer();
                case DATUM_RECH_COLUMN_INDEX:
                    DateFormat formatter = DateFormat
                            .getDateInstance(DateFormat.MEDIUM);
                    return formatter.format(rNode.rechnung.getDatum());
                case NAME_RECH_COLUMN_INDEX:
                    return rNode.mitglied.getVorname() + " "
                            + rNode.mitglied.getNachname();
                case BETRAG_RECH_COLUMN_INDEX:
                    return rNode.rechnung.getBetrag();
                case CHECK_COLUMN_INDEX:
                    return rNode.checked;
                default:
                    return null;
                }
            } else {
                return "";
            }
        }

        @Override
        public void setValueAt(Object value, Object node, int column) {
            if (column == CHECK_COLUMN_INDEX) {
                Boolean boolVal = (Boolean) value;

                if (node instanceof MandatNode) {
                    MandatNode mNode = (MandatNode) node;
                    mNode.checked = boolVal;
                } else if (node instanceof RechnungNode) {
                    RechnungNode rNode = (RechnungNode) node;
                    rNode.checked = boolVal;
                }
            }

            super.setValueAt(value, node, column);
        }

        @Override
        public Object getChild(Object parent, int index) {
            Node rParent = (Node) parent;
            return rParent.getChild(index);
        }

        @Override
        public int getChildCount(Object parent) {
            Node rParent = (Node) parent;
            return rParent.getChildCount();
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            Node rParent = (Node) parent;
            return rParent.getIndexOfChild(child);
        }

        @Override
        public boolean isLeaf(Object node) {
            Node rNode = (Node) node;
            return rNode.isLeaf();
        }

        @Override
        public Object getRoot() {
            return root;
        }

    }

    /**
     * Abstrakte Klasse, zu der alle Einträgen in der Rechnungs-Tabelle gehören.
     */
    private abstract class Node {
        public abstract int getChildCount();

        public abstract int getIndexOfChild(Object child);

        public abstract Object getChild(int index);

        public abstract boolean isLeaf();
    }

    /**
     * Klasse, zu der der Wurzelknoten gehört. Im Baum kommt genau eine Instanz
     * dieser Klasse vor.
     * 
     */
    private final class RootNode extends Node {
        private ArrayList<MandatNode> mandate;

        /**
         * Initialisiert den Baum.
         * 
         * @param filterSettings
         *            Kriterien nach denen die Mandate aus der Datenbank
         *            gefiltert werden sollen
         */
        private RootNode(FilterSettings filterSettings) {
            SqlSession session = sqlSessionFactory.openSession();
            try {
                LastschriftenMapper mapper = session
                        .getMapper(LastschriftenMapper.class);
                Collection<DataMandateRechnungen> mandateDb = mapper
                        .mandateOffeneRechnungen(filterSettings);

                // Setzt die Mandate in die entsprechenden Objekte für den Baum
                // um
                mandate = new ArrayList<>(mandateDb.size());
                for (DataMandateRechnungen mandat : mandateDb) {
                    mandate.add(new MandatNode(mandat, filterSettings));
                }
            } finally {
                session.close();
            }

        }

        @Override
        public int getChildCount() {
            return mandate.size();
        }

        @Override
        public boolean isLeaf() {
            return mandate.isEmpty();
        }

        @Override
        public int getIndexOfChild(Object child) {
            return mandate.indexOf(child);
        }

        @Override
        public Object getChild(int index) {
            return mandate.get(index);
        }
    }

    /**
     * Klasse, zu der die Mandat-Einträge im Baum gehören. Das sind genau die
     * Knoten mit Tiefe 1 (also direkt unterhalb der Wurzel).
     */
    private final class MandatNode extends Node {
        private BeitragSepaMandat mandat;
        private ArrayList<RechnungNode> rechnungen = null;

        private Boolean checked = true;

        private MandatNode(DataMandateRechnungen result,
                FilterSettings filterSettings) {
            this.mandat = result.getMandat();

            rechnungen = new ArrayList<>(result.getRechnungen().size());
            for (DataRechnungMitglied rechnung : result.getRechnungen()) {
                rechnungen.add(new RechnungNode(this, rechnung.getRechnung(),
                        rechnung.getMitglied()));
            }
        }

        @Override
        public int getChildCount() {
            return rechnungen.size();
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        public int getIndexOfChild(Object child) {
            return rechnungen.indexOf(child);
        }

        @Override
        public Object getChild(int index) {
            return rechnungen.get(index);
        }

        public boolean hasCheckedRechnung() {
            boolean hasCheckedRechnung = false;
            if (rechnungen != null) {
                for (RechnungNode rNode : rechnungen) {
                    hasCheckedRechnung |= rNode.checked;
                }
            }
            return hasCheckedRechnung;
        }
    }

    /**
     * Klasse, zu der die Rechnungs-Einträge im Baum gehören. Das sind genau die
     * Knoten mit Tiefe 2 (also Abstand 2 von der Wurzel).
     */
    private final class RechnungNode extends Node {
        private BeitragRechnung rechnung;
        private BeitragMitglied mitglied;

        // Mandat, zu dem die Rechnung gehört (nötig, um im Baum auch aufwärts
        // laufen zu können)
        private MandatNode mandat;

        private Boolean checked = true;

        private RechnungNode(MandatNode mandat, BeitragRechnung rechnung,
                BeitragMitglied mitglied) {
            this.mandat = mandat;
            this.rechnung = rechnung;
            this.mitglied = mitglied;
        }

        @Override
        public int getChildCount() {
            // Rechnungen sind die tiefsten Einträge im Baum
            return 0;
        }

        @Override
        public boolean isLeaf() {
            // Rechnungen sind die tiefsten Einträge im Baum
            return true;
        }

        @Override
        public int getIndexOfChild(Object child) {
            // Funktion sollte nie aufgerufen werden
            throw new IllegalArgumentException(
                    "RechnungNode never has any children");
        }

        @Override
        public Object getChild(int index) {
            // Funktion sollte nie aufgerufen werden
            throw new IllegalArgumentException(
                    "RechnungNode never has any children");
        }
    }

    private static final String[] REPLACE_UML_FROM = { "ä", "ö", "ü", "Ä", "Ö",
            "Ü", "ß" };
    private static final String[] REPLACE_UML_TO = { "ae", "oe", "ue", "Ae",
            "Oe", "Ue", "ss" };

    /**
     * Ersetzt alle Umlaute im übergebenen String durch ihre Umschreibung.
     */
    private static String replaceUmlaute(String text) {
        return StringUtils.replaceEach(text, REPLACE_UML_FROM, REPLACE_UML_TO);
    }
}
