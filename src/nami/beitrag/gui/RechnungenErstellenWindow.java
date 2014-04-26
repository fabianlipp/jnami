package nami.beitrag.gui;

import jas.util.CheckBoxBorderPanel;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import nami.beitrag.Rechnungsstatus;
import nami.beitrag.db.BeitragBuchung;
import nami.beitrag.db.BeitragRechnung;
import nami.beitrag.db.DataMitgliederForderungen;
import nami.beitrag.db.RechnungenMapper;
import nami.beitrag.db.RechnungenMapper.DataRechnungMitBuchungen;
import nami.beitrag.db.RechnungenMapper.FilterSettings;
import nami.beitrag.db.RechnungenMapper.VorausberechnungFilter;
import nami.beitrag.db.RechnungenMapper.ZahlungsartFilter;
import nami.beitrag.gui.utils.Colors;
import nami.beitrag.gui.utils.DisabledCellRenderer;
import nami.beitrag.letters.LetterGenerator;
import nami.beitrag.letters.LetterType;
import net.miginfocom.swing.MigLayout;

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
 * Stellt ein Fenster dar, in dem Rechnungen zusammengestellt werden können.
 * Dazu werden nach bestimmten Kriterien Personen und Buchungen aus der
 * Datenbank geholt und angezeigt. Der Benutzer kann manuell Personen und/oder
 * Buchungen deaktivieren und lässt anschließend die Rechnungen erstellen.
 * 
 * @author Fabian Lipp
 * 
 */
public class RechnungenErstellenWindow extends JFrame {
    private static final long serialVersionUID = 7409328875312329467L;

    private SqlSessionFactory sqlSessionFactory;
    private LetterGenerator letterGenerator;

    // Komponenten für Suche
    private JCheckBox chckbxHalbjahrFilter;
    private HalbjahrComponent inputHalbjahrVon;
    private HalbjahrComponent inputHalbjahrBis;
    private JRadioButton rdbtnKeineVorausberechnungen;
    private JRadioButton rdbtnAuchVorausberechnungen;
    private JRadioButton rdbtnNurVorausberechnungen;
    private JRadioButton rdbtnAlle;
    private JRadioButton rdbtnRechnung;
    private JRadioButton rdbtnLastschrift;
    private JCheckBox chckbxBereitsBerechnet;

    // Komponenten für Tabelle
    private JXTreeTable treeTable;
    // Model der Tabelle
    private RechnungTreeTableModel treeTableModel;
    // Person, deren Zeile momentan in der Tabelle ausgewählt ist (null, falls
    // keine Zeile oder keine Person ausgewählt ist)
    private PersonNode selectedPerson;

    // Komponenten für "Rechnung erstellen"
    private JDateChooser rechnungsdatum;
    private JDateChooser frist;

    // Icon, mit denen Personen in der Tabelle markiert werden
    private static final Icon ICON_PERSON;
    // Icon, mit denen Buchungen in der Tabelle markiert werden
    private static final Icon ICON_BUCHUNG;

    static {
        ImageIcon nativeIcon;
        Image scaled;

        nativeIcon = new ImageIcon(
                RechnungenErstellenWindow.class
                        .getResource("icons/edit-user.png"));
        scaled = nativeIcon.getImage().getScaledInstance(16, 16,
                Image.SCALE_SMOOTH);
        ICON_PERSON = new ImageIcon(scaled);

        nativeIcon = new ImageIcon(
                RechnungenErstellenWindow.class
                        .getResource("icons/text-plain.png"));
        scaled = nativeIcon.getImage().getScaledInstance(16, 16,
                Image.SCALE_SMOOTH);
        ICON_BUCHUNG = new ImageIcon(scaled);
    }

    /**
     * Erzeugt ein neues Rechnungs-Fenster.
     * 
     * @param sqlSessionFactory
     *            Zugriff auf die Datenbank
     * @param letterGenerator
     *            Generator für Briefe
     */
    public RechnungenErstellenWindow(SqlSessionFactory sqlSessionFactory,
            LetterGenerator letterGenerator) {
        super("Rechnungen erstellen");
        this.sqlSessionFactory = sqlSessionFactory;
        this.letterGenerator = letterGenerator;
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
        treeTable = new JXTreeTable(new RechnungTreeTableModel());
        // Model initialisieren
        treeTableModel = new RechnungTreeTableModel();
        treeTable.setTreeTableModel(treeTableModel);
        // Verhindert, dass die Spalten neu initialisiert werden, wenn sich das
        // Model verändert (dabei gehen der TableCellRenderer und die
        // Spaltenbreiten verloren)
        treeTable.setAutoCreateColumnsFromModel(false);
        // Setzt den Renderer, der dafür sorgt, dass Checkboxen, die nicht
        // bearbeitet werden können, disabled werden
        treeTable.getColumn(RechnungTreeTableModel.CHECK_COLUMN_INDEX)
                .setCellRenderer(new DisabledCellRenderer());
        // Icons für den Baum festlegen
        treeTable.setClosedIcon(ICON_PERSON);
        treeTable.setOpenIcon(ICON_PERSON);
        treeTable.setLeafIcon(ICON_BUCHUNG);
        // Reagiert auf die Auswahl einer Zeile durch den Benutzer
        treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        treeTable.addTreeSelectionListener(new SelectPersonListener());
        scrollPane.setViewportView(treeTable);

        Highlighter high;
        // Färbt den Hintergrund aller Buchungen ein
        high = new ColorHighlighter(new BuchungHighlightPredicate(),
                Colors.TT_CHILD_BG, null);
        treeTable.addHighlighter(high);
        // Färbt den Hintergrund der Buchungen, die zur aktuell ausgewählten
        // Person gehören, ein
        high = new ColorHighlighter(new BuchungSelectedPredicate(),
                Colors.TT_SEL_BG, null);
        treeTable.addHighlighter(high);
        // Färbt den Text der abgewählten Personen und Buchungen ein
        high = new ColorHighlighter(new RowDeactivatedPredicate(), null,
                Colors.TT_DEACTIV_FG, null, Colors.TT_DEACTIV_FG);
        treeTable.addHighlighter(high);

        /*** Buttons unter der Tabelle ***/
        JPanel btnsBelow = new JPanel();
        btnsBelow.setBorder(new TitledBorder(null, "Alle Personen",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPane.add(btnsBelow, "cell 0 2,growx");
        btnsBelow.setLayout(new MigLayout("", "[][grow]", "[]"));

        JButton btnAlleAusklappen = new JButton("Alle ausklappen");
        btnsBelow.add(btnAlleAusklappen, "cell 0 0,alignx left");
        btnAlleAusklappen.addActionListener(new AlleAusklappenListener());

        JButton btnAlleEinklappen = new JButton("Alle einklappen");
        btnsBelow.add(btnAlleEinklappen, "cell 0 0,alignx left");
        btnAlleEinklappen.addActionListener(new AlleEinklappenListener());

        JButton btnAuswaehlen = new JButton("Auswählen");
        btnsBelow.add(btnAuswaehlen, "cell 1 0,span,alignx right");
        btnAuswaehlen.addActionListener(new PersonSelectListener(true));

        JButton btnAbwaehlen = new JButton("Abwählen");
        btnsBelow.add(btnAbwaehlen, "cell 1 0,span,alignx right");
        btnAbwaehlen.addActionListener(new PersonSelectListener(false));

        /*** Rechnungs-Erstellungs-Einstellungen und -Button ***/
        JPanel erstellenPanel = new JPanel();
        erstellenPanel.setBorder(new TitledBorder(null, "Rechnungen erstellen",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        erstellenPanel.setLayout(new MigLayout("", "[][grow]",
                "[grow][25px,grow][]"));
        contentPane.add(erstellenPanel, "cell 0 3,growx");

        JLabel lblRechnungsdatum = new JLabel("Rechnungsdatum:");
        erstellenPanel.add(lblRechnungsdatum, "cell 0 0");

        rechnungsdatum = new JDateChooser();
        rechnungsdatum.setDate(new Date());
        rechnungsdatum.addPropertyChangeListener("date",
                new RechnungsdatumAendernListener());
        erstellenPanel.add(rechnungsdatum, "cell 1 0,growx");

        JLabel lblFrist = new JLabel("Frist:");
        erstellenPanel.add(lblFrist, "cell 0 1");

        frist = new JDateChooser();
        frist.setDate(getFristForDatum(new Date()));
        erstellenPanel.add(frist, "cell 1 1,growx");

        JButton btnRechnungenErzeugen = new JButton("Rechnungen erzeugen");
        erstellenPanel.add(btnRechnungenErzeugen,
                "cell 0 2,span,alignx right,aligny top");
        btnRechnungenErzeugen
                .addActionListener(new RechnungenErzeugenListener());

        pack();
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel();
        searchPanel.setBorder(new TitledBorder(null, "Buchungen suchen",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        searchPanel.setLayout(new MigLayout("", "[][][grow]", "[][]"));

        /*** Zeitraum ***/
        /*
         * MigLayout verhält sich in Kombination mit dem CheckBoxBorderPanel
         * etwas merkwürdig. Für die Checkbox muss manuell das Constraint "span"
         * gesetzt werden, weil sonst die nächsten Komponenten verschoben
         * werden. Die Column 0 wird nur von der Checkbox verwendet. Alle
         * anderen Komponenten beginnen bei Row 1.
         */
        MigLayout layout = new MigLayout("", "[][][]", "[][]");
        CheckBoxBorderPanel zeitraumPanel = new CheckBoxBorderPanel("Zeitraum",
                layout);
        chckbxHalbjahrFilter = zeitraumPanel.getCheckBox();
        layout.setComponentConstraints(chckbxHalbjahrFilter, "span");
        chckbxHalbjahrFilter.setSelected(true);
        searchPanel
                .add(zeitraumPanel, "cell 0 0,shrink,alignx left,aligny top");

        JLabel lblVon = new JLabel("Von:");
        zeitraumPanel.add(lblVon, "cell 1 0,flowx,alignx left");
        inputHalbjahrVon = new HalbjahrComponent();
        zeitraumPanel.add(inputHalbjahrVon, "cell 2 0");

        JLabel lblBis = new JLabel("Bis:");
        zeitraumPanel.add(lblBis, "cell 1 1,alignx left");
        inputHalbjahrBis = new HalbjahrComponent();
        zeitraumPanel.add(inputHalbjahrBis, "cell 2 1");

        /*** Vorausberechnung ***/
        JPanel vorausberechnungPanel = new JPanel();
        vorausberechnungPanel.setBorder(new TitledBorder(null,
                "Vorausberechnungen", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        vorausberechnungPanel.setLayout(new BoxLayout(vorausberechnungPanel,
                BoxLayout.Y_AXIS));
        searchPanel.add(vorausberechnungPanel,
                "cell 1 0,alignx left,aligny top");

        rdbtnKeineVorausberechnungen = new JRadioButton(
                "keine Vorausberechnungen");
        vorausberechnungPanel.add(rdbtnKeineVorausberechnungen);
        rdbtnAuchVorausberechnungen = new JRadioButton(
                "auch Vorausberechnungen");
        vorausberechnungPanel.add(rdbtnAuchVorausberechnungen);
        rdbtnNurVorausberechnungen = new JRadioButton("nur Vorausberechnungen");
        vorausberechnungPanel.add(rdbtnNurVorausberechnungen);

        ButtonGroup vorausberechnungGrp = new ButtonGroup();
        vorausberechnungGrp.add(rdbtnKeineVorausberechnungen);
        vorausberechnungGrp.add(rdbtnAuchVorausberechnungen);
        vorausberechnungGrp.add(rdbtnNurVorausberechnungen);
        rdbtnKeineVorausberechnungen.setSelected(true);

        /*** Zahlungsart ***/
        JPanel zahlungsartPanel = new JPanel();
        zahlungsartPanel.setBorder(new TitledBorder(null, "SEPA-Mandat",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        zahlungsartPanel.setLayout(new BoxLayout(zahlungsartPanel,
                BoxLayout.Y_AXIS));
        searchPanel.add(zahlungsartPanel, "cell 2 0,alignx left,aligny top");

        rdbtnAlle = new JRadioButton("Alle");
        zahlungsartPanel.add(rdbtnAlle);
        rdbtnRechnung = new JRadioButton("ohne Lastschrift-Mandat");
        zahlungsartPanel.add(rdbtnRechnung);
        rdbtnLastschrift = new JRadioButton("mit Lastschrift-Mandat");
        zahlungsartPanel.add(rdbtnLastschrift);

        ButtonGroup zahlungsartGrp = new ButtonGroup();
        zahlungsartGrp.add(rdbtnAlle);
        zahlungsartGrp.add(rdbtnRechnung);
        zahlungsartGrp.add(rdbtnLastschrift);
        rdbtnAlle.setSelected(true);

        /*** Untere Zeile ***/
        JPanel sucheBottomPanel = new JPanel();
        searchPanel.add(sucheBottomPanel, "cell 0 1 2097051 1,growx");
        sucheBottomPanel.setLayout(new MigLayout("insets 0", "[][grow]", "[]"));
        chckbxBereitsBerechnet = new JCheckBox(
                "Buchungen, für die bereits eine Rechnung gestellt wurde");
        sucheBottomPanel.add(chckbxBereitsBerechnet,
                "cell 0 0,alignx left,growy");
        JButton btnSuchen = new JButton("Suchen");
        sucheBottomPanel.add(btnSuchen, "cell 1 0,alignx right,aligny top");
        btnSuchen.addActionListener(new PersonenSucheListener());

        return searchPanel;
    }

    /**
     * Liefert den Standardwert für die Frist (in Abhängigkeit vom
     * Rechnungsdatum).
     * 
     * @param datum
     *            Rechnugsdatum
     * @return passende Frist zum Rechnungsdatum
     */
    private static Date getFristForDatum(Date datum) {
        Calendar c = Calendar.getInstance();
        c.setTime(datum);
        c.add(Calendar.DATE, 14);
        return c.getTime();
    }

    /**
     * Listet alle Personen und deren Buchungen in der Tabelle auf, die die
     * eingegeben Kriterien erfüllen.
     */
    private class PersonenSucheListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            RechnungenMapper.FilterSettings filterSettings = new FilterSettings();

            if (chckbxHalbjahrFilter.isSelected()) {
                filterSettings.setHalbjahrVon(inputHalbjahrVon.getValue());
                filterSettings.setHalbjahrBis(inputHalbjahrBis.getValue());
            }

            if (rdbtnKeineVorausberechnungen.isSelected()) {
                filterSettings
                        .setVorausberechnung(VorausberechnungFilter.KEINE);
            } else if (rdbtnAuchVorausberechnungen.isSelected()) {
                filterSettings.setVorausberechnung(VorausberechnungFilter.AUCH);
            } else if (rdbtnNurVorausberechnungen.isSelected()) {
                filterSettings.setVorausberechnung(VorausberechnungFilter.NUR);
            }

            if (rdbtnRechnung.isSelected()) {
                filterSettings
                        .setZahlungsart(ZahlungsartFilter.KEINE_LASTSCHRIFT);
            } else if (rdbtnLastschrift.isSelected()) {
                filterSettings.setZahlungsart(ZahlungsartFilter.LASTSCHRIFT);
            } else if (rdbtnAlle.isSelected()) {
                filterSettings.setZahlungsart(ZahlungsartFilter.ALLE);
            }

            filterSettings.setBereitsBerechnet(chckbxBereitsBerechnet
                    .isSelected());

            treeTableModel.reloadPersons(filterSettings);
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
    private final class PersonSelectListener implements ActionListener {
        private boolean desiredState;

        private PersonSelectListener(boolean desiredState) {
            this.desiredState = desiredState;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (treeTableModel.root == null) {
                // es stehen keine Personen in der Tabelle
                return;
            }
            for (PersonNode pNode : treeTableModel.root.persons) {
                pNode.checked = desiredState;
            }
            treeTable.repaint();
        }
    }

    /**
     * Passt die Frist an, wenn das Rechnungsdatum geändert wird.
     */
    private class RechnungsdatumAendernListener implements
            PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent e) {
            if ("date".equals(e.getPropertyName())) {
                frist.setDate(getFristForDatum(rechnungsdatum.getDate()));
            }
        }
    }

    /**
     * Erzeugt Rechnungen für die ausgewählten Personen und Buchungen.
     */
    private class RechnungenErzeugenListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (treeTableModel.root == null) {
                // es stehen keine Personen in der Tabelle
                return;
            }

            // Rechnungen, die später in den Brief eingefügt werden
            LinkedList<Integer> rechnungIds = new LinkedList<>();

            SqlSession session = sqlSessionFactory.openSession();
            try {
                RechnungenMapper mapper = session
                        .getMapper(RechnungenMapper.class);

                for (PersonNode pNode : treeTableModel.root.persons) {
                    if (!pNode.checked || pNode.buchungen == null
                            || pNode.buchungen.isEmpty()) {
                        // erzeuge keine Rechnung
                        continue;
                    }

                    // Buchungen auslesen, die ausgewählt sind
                    LinkedList<BeitragBuchung> aktiveBuchungen = new LinkedList<>();
                    for (BuchungNode bNode : pNode.buchungen) {
                        if (bNode.checked) {
                            aktiveBuchungen.add(bNode.buchung);
                        }
                    }

                    if (aktiveBuchungen.isEmpty()) {
                        // zu dieser Person ist keine Buchung ausgewählt
                        // => keine Rechnung erzeugen
                        continue;
                    }

                    // Rechnung in Datenbank einfügen
                    BeitragRechnung rechnung = new BeitragRechnung();
                    rechnung.setMitgliedId(pNode.person.getMitgliedId());
                    int jahr = Calendar.getInstance().get(Calendar.YEAR);
                    int rechnungsNummer = mapper.maxRechnungsnummer(jahr) + 1;
                    rechnung.setRechnungsNummer(rechnungsNummer);
                    rechnung.setDatum(rechnungsdatum.getDate());
                    rechnung.setFrist(frist.getDate());
                    rechnung.setStatus(Rechnungsstatus.OFFEN);
                    mapper.insertRechnung(rechnung);
                    int rechnungId = rechnung.getRechnungId();

                    // Posten in Datenbank einfügen
                    for (BeitragBuchung buchung : aktiveBuchungen) {
                        String kommentar = buchung.getKommentar();
                        if (kommentar == null) {
                            kommentar = "";
                        }
                        mapper.insertPosten(rechnungId, buchung.getBuchungId(),
                                kommentar);
                    }

                    rechnungIds.add(rechnung.getRechnungId());
                    session.commit();
                }

                if (rechnungIds.size() == 0) {
                    JOptionPane.showMessageDialog(
                            RechnungenErstellenWindow.this,
                            "Keine Personen/Buchungen ausgewählt", "Fehler",
                            JOptionPane.WARNING_MESSAGE);
                } else if (rechnungIds.size() == 1) {
                    int rechnungId = rechnungIds.getFirst();
                    DataRechnungMitBuchungen rechnung = mapper
                            .getRechnungMitBuchungen(rechnungId);
                    String nachname = rechnung.getMitglied().getNachname();
                    String vorname = rechnung.getMitglied().getVorname();
                    letterGenerator.generateLetter(LetterType.RECHNUNG,
                            rechnungId, rechnungsdatum.getDate(), nachname,
                            vorname);
                    dispose();
                } else {
                    letterGenerator.generateLetters(LetterType.RECHNUNG,
                            rechnungIds, rechnungsdatum.getDate());
                    dispose();
                }
            } finally {
                session.close();
            }
        }
    }

    /**
     * Wählt alle Zeilen aus, die Buchungen sind.
     */
    private class BuchungHighlightPredicate implements HighlightPredicate {
        @Override
        public boolean isHighlighted(Component renderer,
                ComponentAdapter adapter) {
            int row = adapter.convertRowIndexToModel(adapter.row);
            TreePath path = treeTable.getPathForRow(row);
            return (path.getLastPathComponent() instanceof BuchungNode);
        }
    }

    /**
     * Wählt alle Buchungen aus, bei denen die zugeordnete Person vom Benutzer
     * ausgewählt wurde.
     */
    private class BuchungSelectedPredicate implements HighlightPredicate {
        @Override
        public boolean isHighlighted(Component renderer,
                ComponentAdapter adapter) {
            if (selectedPerson != null) {
                int row = adapter.convertRowIndexToModel(adapter.row);
                TreePath path = treeTable.getPathForRow(row);
                return (path.getPathComponent(1) == selectedPerson);
            }
            return false;
        }
    }

    /**
     * Wählt alle Zeilen der Tabelle aus, die deaktiviert sind. Dafür gibt es
     * folgende Ursachen:
     * <ol>
     * <li>Die zur Zeile gehörige Person ist deaktiviert</li>
     * <li>Die Zeile selbst ist deaktiviert</li>
     * <li>Die Zeile ist eine Person un)d alle zugehörigen Buchungen sind
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

                    // Erfüllt, wenn entsprechende Person deaktiviert ist
                    PersonNode pNode = (PersonNode) path.getPathComponent(1);
                    if (!pNode.checked) {
                        return true;
                    }

                    // Erfüllt, wenn dies eine Buchung ist, die deaktiviert ist
                    if (lastComp instanceof BuchungNode) {
                        BuchungNode bNode = (BuchungNode) lastComp;
                        if (!bNode.checked) {
                            return true;
                        }
                    }

                    // Erfüllt, wenn dies eine Person ist und alle Buchungen
                    // deaktiviert sind
                    if (lastComp instanceof PersonNode) {
                        pNode = (PersonNode) lastComp;
                        if (pNode.buchungen != null) {
                            boolean allUnchecked = true;
                            for (BuchungNode bNode : pNode.buchungen) {
                                allUnchecked &= !bNode.checked;
                            }
                            if (allUnchecked) {
                                return true;
                            }
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
    private class SelectPersonListener implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            if (e.getNewLeadSelectionPath() != null) {
                Object selected = e.getNewLeadSelectionPath()
                        .getLastPathComponent();
                if (selected instanceof PersonNode) {
                    selectedPerson = (PersonNode) selected;
                } else {
                    selectedPerson = null;
                }
            } else {
                selectedPerson = null;
            }

            // neu zeichnen, damit Highlighter neu ausgeführt werden (einer
            // davon beachtet selectedPerson)
            treeTable.repaint();
        }
    }

    /**
     * Das Model für die Rechnungs-Vorbereitungs-Tabelle.
     */
    private class RechnungTreeTableModel extends AbstractTreeTableModel {

        // beschreibt die Wurzel des dargestellten Baumes
        private RootNode root = null;

        private static final int ID_COLUMN_INDEX = 0;
        private static final int DATUM_COLUMN_INDEX = 1;
        private static final int TYP_COLUMN_INDEX = 2;
        private static final int KOMMENTAR_COLUMN_INDEX = 3;
        private static final int BETRAG_COLUMN_INDEX = 4;
        private static final int CHECK_COLUMN_INDEX = 5;

        /**
         * Ersetzt die momentan angezeigten Personen durch eine neue Liste.
         * 
         * @param personsDb
         *            Ergebnis einer Datenbankabfrage nach Personen
         */
        private void reloadPersons(FilterSettings filterSettings) {
            root = new RootNode(filterSettings);
            this.modelSupport.fireNewRoot();
        }

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            if (column == KOMMENTAR_COLUMN_INDEX) {
                return String.class;
            } else if (column == CHECK_COLUMN_INDEX) {
                return Boolean.class;
            }
            return super.getColumnClass(column);
        }

        @Override
        public boolean isCellEditable(Object node, int column) {
            // Kommentar-Texte sind nur bearbeitbar, wenn die Buchung selbst und
            // die zugehörige Person aktiviert sind
            if (column == KOMMENTAR_COLUMN_INDEX && node instanceof BuchungNode) {
                BuchungNode bNode = (BuchungNode) node;
                if (bNode.checked && bNode.person.checked) {
                    return true;
                }
            }

            // Die Buchungs-Checkboxen sind nicht bearbeitbar, wenn die
            // zugehörige Person deaktiviert ist
            if (column == CHECK_COLUMN_INDEX) {
                if (node instanceof BuchungNode) {
                    BuchungNode bNode = (BuchungNode) node;
                    if (!bNode.person.checked) {
                        return false;
                    }
                }
                return true;
            }
            return super.isCellEditable(node, column);
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case ID_COLUMN_INDEX:
                return "ID";
            case DATUM_COLUMN_INDEX:
                return "Datum";
            case TYP_COLUMN_INDEX:
                return "Buchungstyp";
            case KOMMENTAR_COLUMN_INDEX:
                return "Kommentar";
            case BETRAG_COLUMN_INDEX:
                return "Betrag";
            case CHECK_COLUMN_INDEX:
                return "";
            default:
                throw new IllegalArgumentException("Invalid column index: "
                        + column);
            }
        }

        @Override
        public Object getValueAt(Object node, int column) {
            if (node instanceof PersonNode) {
                PersonNode rNode = (PersonNode) node;
                switch (column) {
                case ID_COLUMN_INDEX:
                    return rNode.person.getMitgliedsnummer();
                case DATUM_COLUMN_INDEX:
                    return rNode.person.getNachname();
                case TYP_COLUMN_INDEX:
                    return rNode.person.getVorname();
                case KOMMENTAR_COLUMN_INDEX:
                    return "";
                case BETRAG_COLUMN_INDEX:
                    return rNode.person.getSaldo();
                case CHECK_COLUMN_INDEX:
                    return rNode.checked;
                default:
                    throw new IllegalArgumentException("Invalid column index: "
                            + column);
                }
            } else if (node instanceof BuchungNode) {
                BuchungNode rNode = (BuchungNode) node;
                switch (column) {
                case ID_COLUMN_INDEX:
                    return rNode.buchung.getBuchungId();
                case DATUM_COLUMN_INDEX:
                    DateFormat formatter = DateFormat
                            .getDateInstance(DateFormat.MEDIUM);
                    return formatter.format(rNode.buchung.getDatum());
                case TYP_COLUMN_INDEX:
                    return rNode.buchung.getTyp();
                case KOMMENTAR_COLUMN_INDEX:
                    return rNode.buchung.getKommentar();
                case BETRAG_COLUMN_INDEX:
                    return rNode.buchung.getBetrag();
                case CHECK_COLUMN_INDEX:
                    return rNode.checked;
                default:
                    throw new IllegalArgumentException("Invalid column index: "
                            + column);
                }
            } else {
                return "";
            }
        }

        @Override
        public void setValueAt(Object value, Object node, int column) {
            if (column == KOMMENTAR_COLUMN_INDEX && node instanceof BuchungNode) {
                String stringVal = (String) value;
                BuchungNode bNode = (BuchungNode) node;
                bNode.buchung.setKommentar(stringVal);
            } else if (column == CHECK_COLUMN_INDEX
                    && node instanceof PersonNode) {
                Boolean boolVal = (Boolean) value;
                PersonNode pNode = (PersonNode) node;
                pNode.checked = boolVal;
            } else if (column == CHECK_COLUMN_INDEX
                    && node instanceof BuchungNode) {
                Boolean boolVal = (Boolean) value;
                BuchungNode bNode = (BuchungNode) node;
                bNode.checked = boolVal;
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
        private ArrayList<PersonNode> persons;

        /**
         * Initialisiert den Baum.
         * 
         * @param personsDb
         *            Personen, die im Baum angezeigt werden sollen
         */
        private RootNode(FilterSettings filterSettings) {
            SqlSession session = sqlSessionFactory.openSession();
            try {
                RechnungenMapper rechnungenMapper = session
                        .getMapper(RechnungenMapper.class);
                Collection<DataMitgliederForderungen> personsDb = rechnungenMapper
                        .mitgliederOffeneForderungen(filterSettings);

                // Setzt die Personen in die entsprechenden Objekte für den Baum
                // um
                persons = new ArrayList<>(personsDb.size());
                for (DataMitgliederForderungen person : personsDb) {
                    persons.add(new PersonNode(person, filterSettings));
                }
            } finally {
                session.close();
            }

        }

        @Override
        public int getChildCount() {
            return persons.size();
        }

        @Override
        public boolean isLeaf() {
            return persons.isEmpty();
        }

        @Override
        public int getIndexOfChild(Object child) {
            return persons.indexOf(child);
        }

        @Override
        public Object getChild(int index) {
            return persons.get(index);
        }
    }

    /**
     * Klasse, zu der die Personeneinträge im Baum gehören. Das sind genau die
     * Knoten mit Tiefe 1 (also direkt unterhalb der Wurzel).
     */
    private final class PersonNode extends Node {
        private DataMitgliederForderungen person;

        // Gibt an, ob eine Rechnung für die Person erzeugt werden soll (kann in
        // der Tabelle bearbeitet werden)
        private Boolean checked = true;

        // Die Buchungsliste wird leer initialisiert und erst bei Bedarf (d.h.
        // wenn der entsprechende Zweig im Baum aufgeklappt wird) aus der
        // Datenbank geladen.
        private ArrayList<BuchungNode> buchungen = null;

        private PersonNode(DataMitgliederForderungen person,
                FilterSettings filterSettings) {
            this.person = person;

            // Lade Buchungen aus Datenbank
            SqlSession session = sqlSessionFactory.openSession();
            try {
                RechnungenMapper mapper = session
                        .getMapper(RechnungenMapper.class);
                Collection<BeitragBuchung> buchungenDb = mapper
                        .getBuchungenFiltered(person.getMitgliedId(),
                                filterSettings);

                buchungen = new ArrayList<>(buchungenDb.size());
                for (BeitragBuchung buchung : buchungenDb) {
                    buchungen.add(new BuchungNode(this, buchung));
                }
            } finally {
                session.close();
            }
        }

        @Override
        public int getChildCount() {
            return buchungen.size();
        }

        // wir gehen davon aus, dass es für alle Personen in der Liste Buchungen
        // gibt
        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        public int getIndexOfChild(Object child) {
            return buchungen.indexOf(child);
        }

        @Override
        public Object getChild(int index) {
            return buchungen.get(index);
        }
    }

    /**
     * Klasse, zu der die Buchungs-Einträge im Baum gehören. Das sind genau die
     * Knoten mit Tiefe 2 (also Abstand 2 von der Wurzel).
     */
    private final class BuchungNode extends Node {
        private BeitragBuchung buchung;

        // Person, zu der die Buchung gehört (nötig, um im Baum auch aufwärts
        // laufen zu können)
        private PersonNode person;

        private Boolean checked = true;

        private BuchungNode(PersonNode person, BeitragBuchung buchung) {
            this.person = person;
            this.buchung = buchung;
        }

        @Override
        public int getChildCount() {
            // Buchungen sind die tiefsten Einträge im Baum
            return 0;
        }

        @Override
        public boolean isLeaf() {
            // Buchungen sind die tiefsten Einträge im Baum
            return true;
        }

        @Override
        public int getIndexOfChild(Object child) {
            // Funktion sollte nie aufgerufen werden
            throw new IllegalArgumentException(
                    "BuchungNode never has any children");
        }

        @Override
        public Object getChild(int index) {
            // Funktion sollte nie aufgerufen werden
            throw new IllegalArgumentException(
                    "BuchungNode never has any children");
        }
    }
}
