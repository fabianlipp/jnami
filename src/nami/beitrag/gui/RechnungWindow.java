package nami.beitrag.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIDefaults;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreePath;

import nami.beitrag.db.BeitragBuchung;
import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.DataMitgliederForderungen;
import nami.beitrag.db.RechnungenMapper;
import nami.beitrag.letters.LetterGenerator;
import nami.connector.Halbjahr;
import net.miginfocom.swing.MigLayout;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

/**
 * Stellt ein Fenster dar, in dem Rechnungen zusammengestellt werden können.
 * Dazu werden nach bestimmten Kriterien Personen und Buchungen aus der
 * Datenbank geholt und angezeigt. Der Benutzer kann manuell Personen und/oder
 * Buchungen deaktivieren und lässt anschließend die Rechnungen erstellen.
 * 
 * @author Fabian Lipp
 * 
 */
public class RechnungWindow extends JFrame {
    private static final long serialVersionUID = 7409328875312329467L;

    private SqlSessionFactory sqlSessionFactory;

    private HalbjahrComponent halbjahr;
    // Das Halbjahr, das gerade in der Tabelle angezeigt wird
    private Halbjahr tableHalbjahr;

    // Tabelle
    private JXTreeTable treeTable;
    // Model der Tabelle
    private RechnungTreeTableModel treeTableModel;
    // Person, deren Zeile momentan in der Tabelle ausgewählt ist (null, falls
    // keine Zeile oder keine Person ausgewählt ist)
    private PersonNode selectedPerson;

    // Farben für die Hervorhebung von Zeilen in der Tabelle
    private static final UIDefaults UIDEFAULTS = javax.swing.UIManager
            .getDefaults();
    private static final Color COL_BUCHUNG_BG = UIDEFAULTS
            .getColor("Label.background");
    private static final Color COL_BUCHUNG_SELECTED_BG = UIDEFAULTS
            .getColor("List.selectionBackground");
    private static final Color COL_DEACTIVATED_FG = UIDEFAULTS
            .getColor("Label.disabledForeground");

    // Icon, mit denen Personen in der Tabelle markiert werden
    private static final Icon ICON_PERSON;
    // Icon, mit denen Buchungen in der Tabelle markiert werden
    private static final Icon ICON_BUCHUNG;

    static {
        ImageIcon nativeIcon;
        Image scaled;

        nativeIcon = new ImageIcon(
                RechnungWindow.class.getResource("icons/edit-user.png"));
        scaled = nativeIcon.getImage().getScaledInstance(16, 16,
                Image.SCALE_SMOOTH);
        ICON_PERSON = new ImageIcon(scaled);

        nativeIcon = new ImageIcon(
                RechnungWindow.class.getResource("icons/text-plain.png"));
        scaled = nativeIcon.getImage().getScaledInstance(16, 16,
                Image.SCALE_SMOOTH);
        ICON_BUCHUNG = new ImageIcon(scaled);
    }

    /**
     * Erzeugt ein neues Rechnungs-Fenster.
     * 
     * @param sqlSessionFactory
     *            Zugriff auf die Datenbank
     */
    public RechnungWindow(SqlSessionFactory sqlSessionFactory) {
        super("Rechnungen");
        this.sqlSessionFactory = sqlSessionFactory;
        buildFrame();
    }

    private void buildFrame() {
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new MigLayout("", "[grow][][]", "[][][grow][]"));

        JLabel lblHalbjahr = new JLabel("Halbjahr:");
        contentPane.add(lblHalbjahr, "flowx,cell 0 0");
        halbjahr = new HalbjahrComponent();
        contentPane.add(halbjahr, "cell 0 0,alignx left");

        JButton btnAlleOffenenForderungen = new JButton(
                "Alle offenen Forderungen");
        btnAlleOffenenForderungen
                .addActionListener(new AlleForderungenListener());
        contentPane.add(btnAlleOffenenForderungen, "cell 0 1 3 1");

        /*** Tabelle vorbereiten ***/
        JScrollPane scrollPane = new JScrollPane();
        contentPane.add(scrollPane, "cell 0 2 3 1,grow");
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
                COL_BUCHUNG_BG, null);
        treeTable.addHighlighter(high);
        // Färbt den Hintergrund der Buchungen, die zur aktuell ausgewählten
        // Person gehören, ein
        high = new ColorHighlighter(new BuchungSelectedPredicate(),
                COL_BUCHUNG_SELECTED_BG, null);
        treeTable.addHighlighter(high);
        // Färbt den Text der abgewählten Personen und Buchungen ein
        high = new ColorHighlighter(new RowDeactivatedPredicate(), null,
                COL_DEACTIVATED_FG, null, COL_DEACTIVATED_FG);
        treeTable.addHighlighter(high);

        /*** Buttons unter der Tabelle ***/
        JButton btnAlleAusklappen = new JButton("Alle ausklappen");
        btnAlleAusklappen.addActionListener(new AlleAusklappenListener());
        contentPane.add(btnAlleAusklappen, "flowx,cell 0 3");

        JButton btnAlleEinklappen = new JButton("Alle einklappen");
        btnAlleEinklappen.addActionListener(new AlleEinklappenListener());
        contentPane.add(btnAlleEinklappen, "cell 0 3");

        JButton btnRechnungenErzeugen = new JButton("Rechnungen erzeugen");
        btnRechnungenErzeugen
                .addActionListener(new RechnungenErzeugenListener());
        contentPane.add(btnRechnungenErzeugen, "cell 1 3,alignx right");

        pack();
    }

    /**
     * Fragt alle Personen ab, deren Beitragskonto für das gegebene Halbjahr
     * nicht ausgeglichen ist.
     */
    private class AlleForderungenListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            tableHalbjahr = halbjahr.getValue();

            SqlSession session = sqlSessionFactory.openSession();
            RechnungenMapper rechnungenMapper = session
                    .getMapper(RechnungenMapper.class);
            Collection<DataMitgliederForderungen> personsDb = rechnungenMapper
                    .mitgliederOffeneForderungen(tableHalbjahr);
            session.close();

            treeTableModel.reloadPersons(personsDb);
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
     * Erzeugt Rechnungen für die ausgewählten Personen und Buchungen.
     */
    private class RechnungenErzeugenListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (treeTableModel.root == null) {
                return;
            }

            LinkedHashMap<Integer, Collection<BeitragBuchung>> rechnungen;
            rechnungen = new LinkedHashMap<>();
            for (PersonNode pNode : treeTableModel.root.persons) {
                Collection<BeitragBuchung> buchungen = new LinkedList<>();
                if (pNode.checked) {
                    for (BuchungNode bNode : pNode.buchungen) {
                        if (bNode.checked) {
                            buchungen.add(bNode.buchung);
                        }
                    }
                    rechnungen.put(pNode.person.getMitgliedId(), buchungen);
                }
            }

            LetterGenerator gen = new LetterGenerator(sqlSessionFactory);
            gen.generateRechnungen(rechnungen);
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
     * <li>Die Zeile ist eine Person und alle zugehörigen Buchungen sind
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
     * Modifiziert den DefaultRenderer so, dass Steuerelemente disabled werden
     * (enabled=false), wenn die entsprechende Zelle nicht editable ist.
     */
    private static class DisabledCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = -1264933737399197781L;

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {

            TableCellRenderer renderer = table.getDefaultRenderer(table
                    .getColumnClass(column));
            Component c = renderer.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);

            // Deaktiviert die Komponente, falls das Feld nicht bearbeitbar ist
            if (!table.isCellEditable(row, column)) {
                c.setEnabled(false);
            }

            return c;
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
        private void reloadPersons(
                Collection<DataMitgliederForderungen> personsDb) {
            root = new RootNode(personsDb);
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
        private RootNode(Collection<DataMitgliederForderungen> personsDb) {
            // Setzt die Personen in die entsprechenden Objekte für den Baum um
            persons = new ArrayList<>(personsDb.size());
            for (DataMitgliederForderungen person : personsDb) {
                persons.add(new PersonNode(person));
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

        private PersonNode(DataMitgliederForderungen person) {
            this.person = person;

            // Lade Buchungen aus Datenbank
            SqlSession session = sqlSessionFactory.openSession();
            BeitragMapper beitragMapper = session
                    .getMapper(BeitragMapper.class);
            Collection<BeitragBuchung> buchungenDb = beitragMapper
                    .getBuchungenByHalbjahr(tableHalbjahr,
                            person.getMitgliedId());
            session.close();

            buchungen = new ArrayList<>(buchungenDb.size());
            for (BeitragBuchung buchung : buchungenDb) {
                buchungen.add(new BuchungNode(this, buchung));
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
