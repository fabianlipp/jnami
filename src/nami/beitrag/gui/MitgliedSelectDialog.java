package nami.beitrag.gui;

import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.BeitragMitglied;
import net.miginfocom.swing.MigLayout;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * Stellt einen Dialog bereit, mit dem ein Mitglied in der lokalen Datenbank
 * gesucht werden kann.
 * 
 * @author Fabian Lipp
 * 
 */
public class MitgliedSelectDialog extends JDialog {
    private static final long serialVersionUID = 3356560792088288305L;
    private JTextField mitgliedsnummer;
    private JTextField vorname;
    private JTextField nachname;
    private JScrollPane tablePane;
    private JTable table;
    private MitgliedListTableModel tableModel;
    private int chosenMglId = -1;

    private SqlSessionFactory sqlSessionFactory;

    /**
     * Erzeugt einen neuen <tt>MitgliedSelectDialog</tt>.
     * 
     * @param parent
     *            besitzendes Fenster
     * @param sqlSessionFactory
     *            Zugriff auf die Datenbank
     */
    public MitgliedSelectDialog(Window parent, SqlSessionFactory sqlSessionFactory) {
        super(parent, DEFAULT_MODALITY_TYPE);
        setTitle("Mitglied auswählen");
        this.sqlSessionFactory = sqlSessionFactory;
        buildFrame();
    }

    private void buildFrame() {
        getContentPane().setLayout(
                new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        getContentPane().add(panel);
        panel.setLayout(new MigLayout("", "[][grow]", "[][][][][grow]"));

        JLabel lblMitgliedsnummer = new JLabel("Mitgliedsnummer");
        mitgliedsnummer = new JTextField();
        lblMitgliedsnummer.setLabelFor(mitgliedsnummer);
        lblMitgliedsnummer.setDisplayedMnemonic('m');
        panel.add(lblMitgliedsnummer, "");
        panel.add(mitgliedsnummer, "grow,wrap");

        JLabel lblVorname = new JLabel("Vorname");
        vorname = new JTextField();
        lblVorname.setLabelFor(vorname);
        lblVorname.setDisplayedMnemonic('v');
        panel.add(lblVorname, "");
        panel.add(vorname, "grow,wrap");

        JLabel lblNachname = new JLabel("Nachname");
        nachname = new JTextField();
        lblNachname.setLabelFor(nachname);
        lblNachname.setDisplayedMnemonic('n');
        panel.add(lblNachname, "");
        panel.add(nachname, "grow,wrap");

        JButton btnSuchen = new JButton("Suchen");
        btnSuchen.setMnemonic('s');
        btnSuchen.addActionListener(new SearchButtonListener());
        panel.add(btnSuchen, "span,trail,wrap");

        tablePane = new JScrollPane();
        tablePane.setPreferredSize(new Dimension(400, 200));
        panel.add(tablePane, "span,grow");

        JRootPane rootPane = getRootPane();
        rootPane.setDefaultButton(btnSuchen);
        // Handle ESC-key
        Action escListener = new AbstractAction() {
            private static final long serialVersionUID = 8011175609689348329L;

            @Override
            public void actionPerformed(ActionEvent e) {
                dispatchEvent(new WindowEvent(MitgliedSelectDialog.this,
                        WindowEvent.WINDOW_CLOSING));
            }
        };
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "jnami.ESC");
        rootPane.getActionMap().put("jnami.ESC", escListener);

        pack();
    }

    /**
     * Setzt eine MitgliedID als Ergebnis und schließt das Fenster.
     * 
     * @param mglId
     *            MitgliedID, die das Ergebnis darstellt
     */
    private void chooseMglId(int mglId) {
        chosenMglId = mglId;
        MitgliedSelectDialog.this.setVisible(false);
    }

    /**
     * Liefert die gewählte MitgliedID.
     * 
     * @return gewählte MitgliedID
     */
    public int getChosenMglId() {
        return chosenMglId;
    }

    /**
     * Füllt bei Auswahl des Suchbuttons die Tabelle mit den zu den Eingaben
     * passenden Mitgliedern.
     */
    private class SearchButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            SqlSession session = sqlSessionFactory.openSession();
            try {
                BeitragMapper mapper = session.getMapper(BeitragMapper.class);
                Collection<BeitragMitglied> results = mapper.findMitglieder(
                        mitgliedsnummer.getText(), getSearchString(vorname),
                        getSearchString(nachname));

                if (results.size() == 1) {
                    chooseMglId(results.iterator().next().getMitgliedId());
                } else {
                    tableModel = new MitgliedListTableModel(results);
                    table = new JTable(tableModel);
                    table.setRowSelectionInterval(0, 0);
                    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    table.addMouseListener(new TableClickListener());
                    table.addKeyListener(new TableKeyListener());

                    // use TAB to give focus to next component instead of next
                    // table cell
                    table.setFocusTraversalKeys(
                            KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
                    table.setFocusTraversalKeys(
                            KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

                    tablePane.setViewportView(table);
                    table.requestFocusInWindow();
                }

            } finally {
                session.close();
            }
        }

        private String getSearchString(JTextField field) {
            return "%" + field.getText() + "%";
        }
    }

    /**
     * Stellt eine Liste von Mitgliedern als TableModel dar. Die Zellen der
     * Tabelle können nicht bearbeitet werden.
     */
    private class MitgliedListTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 8639929360446497274L;
        private List<BeitragMitglied> mitglieder;

        public MitgliedListTableModel(Collection<BeitragMitglied> mitglieder) {
            this.mitglieder = new ArrayList<>(mitglieder);
        }

        @Override
        public int getRowCount() {
            return mitglieder.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
            case 0:
                return "ID";
            case 1:
                return "Mitgliedsnummer";
            case 2:
                return "Nachname";
            case 3:
                return "Vorname";
            default:
                return "";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            BeitragMitglied mgl = mitglieder.get(rowIndex);
            switch (columnIndex) {
            case 0:
                return mgl.getMitgliedId();
            case 1:
                return mgl.getMitgliedsnummer();
            case 2:
                return mgl.getNachname();
            case 3:
                return mgl.getVorname();
            default:
                return "";
            }
        }

        /**
         * Liefert die ID des Mitglieds in einer bestimmten Zeile.
         * 
         * @param rowIndex
         *            Zeile der Tabelle
         * @return MitgliedID in dieser Zeile
         */
        public int getIdAt(int rowIndex) {
            return mitglieder.get(rowIndex).getMitgliedId();
        }
    }

    /**
     * Erlaubt es Mitglieder durch Doppelklicks in der Tabelle auszuwählen.
     */
    private class TableClickListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (table != e.getComponent()) {
                throw new IllegalArgumentException(
                        "Used Listener on unexpected object");
            }

            if (e.getClickCount() == 2) {
                int row = table.rowAtPoint(e.getPoint());
                chooseMglId(tableModel.getIdAt(row));
            }
        }
    }

    /**
     * Erlaubt es Mitglieder durch Drücken der Eingabetaste in der Tabelle
     * auszuwählen.
     */
    private class TableKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (table != e.getComponent()) {
                throw new IllegalArgumentException(
                        "Used Listener on unexpected object");
            }

            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    chooseMglId(tableModel.getIdAt(row));
                }
            }
        }
    }
}
