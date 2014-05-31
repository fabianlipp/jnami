package nami.beitrag.gui;

import jas.util.CheckBoxBorderPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import nami.beitrag.NamiBeitragConfiguration;
import nami.beitrag.db.BeitragBrief;
import nami.beitrag.db.BriefeMapper;
import nami.beitrag.db.BriefeMapper.FilterSettings;
import nami.beitrag.letters.LatexRunner;
import nami.beitrag.letters.LetterDirectory;
import nami.beitrag.letters.LetterType;
import net.miginfocom.swing.MigLayout;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import com.toedter.calendar.JDateChooser;

/**
 * Stellt ein Fenster dar, in dem Briefe kompiliert und geöffnet werden können.
 * 
 * @author Fabian Lipp
 * 
 */
public class BriefeWindow extends JFrame {
    private static final long serialVersionUID = 7409328875312329467L;

    private SqlSessionFactory sqlSessionFactory;
    private LetterDirectory letterDirectory;
    private LatexRunner latex;

    // Komponenten für Suche
    private JCheckBox chckbxDatumFilter;
    private JDateChooser inputDatumVon;
    private JDateChooser inputDatumBis;
    private JRadioButton rdbtnAlle;
    private JRadioButton rdbtnRechnung;
    private JRadioButton rdbtnMahnung;
    private JRadioButton rdbtnPrenotification;

    // Komponenten für Tabelle
    private JTable table;
    // Model der Tabelle
    private BriefeModel tableModel;

    // aus Konfigurationsdatei eingelesen
    private String pdfViewer;

    private static Logger logger = Logger.getLogger(BriefeWindow.class
            .getName());

    /**
     * Erzeugt ein neues Rechnungs-Fenster.
     * 
     * @param sqlSessionFactory
     *            Zugriff auf die Datenbank
     * @param letterDirectory
     *            Verzeichnis für Briefe
     * @param conf
     *            Konfiguration des Nami-Beitrags-Tools
     */
    public BriefeWindow(SqlSessionFactory sqlSessionFactory,
            LetterDirectory letterDirectory, NamiBeitragConfiguration conf) {
        super("Rechnungen erstellen");
        this.sqlSessionFactory = sqlSessionFactory;
        this.letterDirectory = letterDirectory;
        this.latex = new LatexRunner(letterDirectory);

        pdfViewer = conf.getPdfViewer();

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

        tableModel = new BriefeModel();
        table = new JTable(tableModel);
        // Verhindert, dass die Spalten neu initialisiert werden, wenn sich das
        // Model verändert (dabei gehen der TableCellRenderer und die
        // Spaltenbreiten verloren)
        table.setAutoCreateColumnsFromModel(false);
        // Reagiert auf die Auswahl einer Zeile durch den Benutzer
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scrollPane.setViewportView(table);

        /*** Rechnungs-Erstellungs-Einstellungen und -Button ***/
        JPanel aktionenPanel = new JPanel();
        aktionenPanel.setBorder(new TitledBorder(null, "Aktionen",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        aktionenPanel.setLayout(new MigLayout("", "[][]", "[]"));
        contentPane.add(aktionenPanel, "cell 0 3,growx");

        JButton btnKompilieren = new JButton("Kompilieren");
        aktionenPanel.add(btnKompilieren, "cell 0 0,aligny top");
        btnKompilieren.addActionListener(new BriefKompilierenListener());

        JButton btnOeffnen = new JButton("Öffnen");
        aktionenPanel.add(btnOeffnen, "cell 1 0,aligny top");
        btnOeffnen.addActionListener(new BriefOeffnenListener());

        refreshTable();

        pack();
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel();
        searchPanel.setBorder(new TitledBorder(null, "Briefe suchen",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        searchPanel.setLayout(new MigLayout("", "[][]", "[]"));

        BriefeSucheListener filterChangeListener = new BriefeSucheListener();

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
        chckbxDatumFilter = zeitraumPanel.getCheckBox();
        layout.setComponentConstraints(chckbxDatumFilter, "span");
        chckbxDatumFilter.setSelected(true);
        searchPanel
                .add(zeitraumPanel, "cell 0 0,shrink,alignx left,aligny top");

        JLabel lblVon = new JLabel("Von:");
        zeitraumPanel.add(lblVon, "cell 1 0,flowx,alignx left");
        inputDatumVon = new JDateChooser(new Date());
        zeitraumPanel.add(inputDatumVon, "cell 2 0");

        JLabel lblBis = new JLabel("Bis:");
        zeitraumPanel.add(lblBis, "cell 1 1,alignx left");
        inputDatumBis = new JDateChooser(new Date());
        zeitraumPanel.add(inputDatumBis, "cell 2 1");

        chckbxDatumFilter.addActionListener(filterChangeListener);
        inputDatumVon.addPropertyChangeListener(filterChangeListener);
        inputDatumBis.addPropertyChangeListener(filterChangeListener);

        /*** Brief-Typ ***/
        JPanel typPanel = new JPanel();
        typPanel.setBorder(new TitledBorder(null, "Brief-Typ",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        typPanel.setLayout(new BoxLayout(typPanel, BoxLayout.Y_AXIS));
        searchPanel.add(typPanel, "cell 1 0,alignx left,aligny top");

        rdbtnAlle = new JRadioButton("Alle");
        rdbtnRechnung = new JRadioButton("Rechnung");
        rdbtnMahnung = new JRadioButton("Mahnung");
        rdbtnPrenotification = new JRadioButton("Prenotification");

        typPanel.add(rdbtnAlle);
        typPanel.add(rdbtnRechnung);
        typPanel.add(rdbtnMahnung);
        typPanel.add(rdbtnPrenotification);

        ButtonGroup vorausberechnungGrp = new ButtonGroup();
        vorausberechnungGrp.add(rdbtnAlle);
        vorausberechnungGrp.add(rdbtnRechnung);
        vorausberechnungGrp.add(rdbtnMahnung);
        vorausberechnungGrp.add(rdbtnPrenotification);
        rdbtnAlle.setSelected(true);

        rdbtnAlle.addActionListener(filterChangeListener);
        rdbtnRechnung.addActionListener(filterChangeListener);
        rdbtnMahnung.addActionListener(filterChangeListener);
        rdbtnPrenotification.addActionListener(filterChangeListener);

        return searchPanel;
    }

    /**
     * Liefert den Brief, der momentan in der Tabelle ausgewählt ist.
     * 
     * @return ausgewählter Brief; <tt>null</tt>, falls keiner ausgewählt ist
     */
    private BeitragBrief getSelectedBrief() {
        int row = table.getSelectedRow();
        if (row != -1) {
            return tableModel.getBriefAt(row);
        } else {
            return null;
        }
    }

    /**
     * Listet bei einer Änderung der Filterkriterien alle Briefe in der Tabelle
     * auf, die die eingegeben Kriterien erfüllen.
     */
    private class BriefeSucheListener implements ActionListener,
            PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent e) {
            if ("date".equals(e.getPropertyName())) {
                refreshTable();
            }
        }

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
        FilterSettings filterSettings = new FilterSettings();

        if (chckbxDatumFilter.isSelected()) {
            filterSettings.setDatumVon(inputDatumVon.getDate());
            filterSettings.setDatumBis(inputDatumBis.getDate());
        }

        if (rdbtnRechnung.isSelected()) {
            filterSettings.setTyp(LetterType.RECHNUNG);
        } else if (rdbtnMahnung.isSelected()) {
            filterSettings.setTyp(LetterType.MAHNUNG);
        } else if (rdbtnPrenotification.isSelected()) {
            filterSettings.setTyp(LetterType.PRENOTIFICATION);
        }

        tableModel.reloadBriefe(filterSettings);
    }

    /**
     * Kompiliert den ausgewählten Brief.
     */
    private class BriefKompilierenListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            final BeitragBrief selected = getSelectedBrief();
            if (selected != null) {
                new Thread(new CompileThread(selected)).start();
            }
        }
    }

    /**
     * Thread der den Kompilier-Vorgang anstößt und im Erfolgsfall die
     * Kompilierungszeit in die Datenbank schreibt.
     */
    private class CompileThread implements Runnable {
        private BeitragBrief brief;

        CompileThread(BeitragBrief brief) {
            this.brief = brief;
        }

        @Override
        public void run() {
            boolean retVal = latex.compile(brief.getDateiname());
            if (retVal) {
                SqlSession session = sqlSessionFactory.openSession();
                try {
                    BriefeMapper mapper = session.getMapper(BriefeMapper.class);
                    brief.setKompiliert(new Date());
                    mapper.updateBrief(brief);
                    session.commit();
                    refreshTable();
                } finally {
                    session.close();
                }
            }
        }
    }

    /**
     * Öffnet den ausgewählten Brief mit dem eingestellten PDF-Betrachter.
     */
    private class BriefOeffnenListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            BeitragBrief selected = getSelectedBrief();
            if (selected == null || selected.getKompiliert() == null) {
                logger.warning("Kein Brief ausgewählt bzw. Brief nicht kompiliert");
            }
            File brief = letterDirectory.getGeneratedFile(selected
                    .getDateiname());
            String[] cmd = { pdfViewer, brief.getAbsolutePath() };
            Runtime rt = Runtime.getRuntime();
            try {
                rt.exec(cmd);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * Stellt eine Liste von Briefen, die nach bestimmten Filterkriterien aus
     * der Datenbank geholt werden, in einer Tabelle dar.
     */
    private class BriefeModel extends AbstractTableModel {
        private static final long serialVersionUID = -8344734930779903398L;

        // Angezeigte Briefe
        private ArrayList<BeitragBrief> briefe;

        private static final int ID_COLUMN_INDEX = 0;
        private static final int DATEINAME_COLUMN_INDEX = 1;
        private static final int DATUM_COLUMN_INDEX = 2;
        private static final int TYP_COLUMN_INDEX = 3;
        private static final int KOMPILIERUNG_COLUMN_INDEX = 4;

        /**
         * Ersetzt die momentan angezeigten Briefe durch eine neue Liste.
         * 
         * @param filterSettings
         *            Kriterien, nach denen gesucht wird
         */
        private void reloadBriefe(FilterSettings filterSettings) {
            SqlSession session = sqlSessionFactory.openSession();
            try {
                BriefeMapper mapper = session.getMapper(BriefeMapper.class);
                briefe = mapper.findBriefe(filterSettings);
                fireTableDataChanged();
            } finally {
                session.close();
            }
        }

        @Override
        public int getRowCount() {
            if (briefe != null) {
                return briefe.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (briefe == null || rowIndex >= briefe.size()) {
                return null;
            }

            BeitragBrief row = briefe.get(rowIndex);
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
            SimpleDateFormat formatterWithTime = new SimpleDateFormat(
                    "dd.MM.yyyy HH:mm:ss");
            switch (columnIndex) {
            case ID_COLUMN_INDEX:
                return row.getBriefId();
            case DATEINAME_COLUMN_INDEX:
                return row.getDateiname();
            case DATUM_COLUMN_INDEX:
                if (row.getDatum() == null) {
                    return "";
                } else {
                    return formatter.format(row.getDatum());
                }
            case TYP_COLUMN_INDEX:
                return row.getTyp();
            case KOMPILIERUNG_COLUMN_INDEX:
                if (row.getKompiliert() == null) {
                    return "";
                } else {
                    return formatterWithTime.format(row.getKompiliert());
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
            case DATEINAME_COLUMN_INDEX:
                return "Dateiname";
            case DATUM_COLUMN_INDEX:
                return "Datum";
            case TYP_COLUMN_INDEX:
                return "Typ";
            case KOMPILIERUNG_COLUMN_INDEX:
                return "Kompiliert";
            default:
                return null;
            }
        }

        /**
         * Liefert den Brief, der in einer bestimmten Zeile angezeigt wird.
         * 
         * @param rowIndex
         *            gesuchte Zeile
         * @return Rechnung in der Zeile
         */
        public BeitragBrief getBriefAt(int rowIndex) {
            if (briefe == null || rowIndex >= briefe.size()) {
                return null;
            }
            return briefe.get(rowIndex);
        }
    }
}
