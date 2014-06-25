package nami.beitrag.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import nami.beitrag.Buchungstyp;
import nami.beitrag.NamiBeitragConfiguration;
import nami.beitrag.Rechnungsstatus;
import nami.beitrag.db.BeitragBuchung;
import nami.beitrag.db.BeitragLastschrift;
import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.BeitragPrenotification;
import nami.beitrag.db.BeitragRechnung;
import nami.beitrag.db.BeitragSammelLastschrift;
import nami.beitrag.db.BeitragSepaMandat;
import nami.beitrag.db.DataLastschriftMandat;
import nami.beitrag.db.LastschriftenMapper;
import nami.beitrag.db.RechnungenMapper;
import nami.beitrag.db.RechnungenMapper.DataHalbjahrBetraege;
import nami.beitrag.gui.utils.DisabledCellRenderer;
import nami.beitrag.hibiscus.HibiscusExporter;
import nami.beitrag.letters.LetterGenerator;
import nami.beitrag.letters.LetterType;
import net.miginfocom.swing.MigLayout;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import com.toedter.calendar.JDateChooser;

/**
 * Stellt ein Fenster dar, in dem Sammellastschriften verwaltet werden können.
 * Dazu werden nach bestimmten Kriterien vorhandene Lastschriften aufgelistet.
 * Anschließend könenn diese exportiert werden oder sie als ausgeführt markiert
 * werden.
 * 
 * @author Fabian Lipp
 * 
 */
public class LastschriftVerwaltenWindow extends JFrame {
    private static final long serialVersionUID = 7409328875312329467L;

    private SqlSessionFactory sqlSessionFactory;
    private LetterGenerator letterGenerator;
    private NamiBeitragConfiguration conf;

    // Filter-Kriterien
    private JRadioButton rdbtnStatusOffen;
    private JRadioButton rdbtnStatusAusgefuehrt;
    private JRadioButton rdbtnStatusAlle;

    // Dargestellte Tabellen
    private JTable sammellastTable;
    private SammellastModel sammellastModel;
    private JScrollPane einzellastScrollPane;
    private JTable einzellastTable;
    private EinzellastModel einzellastModel;

    // Aktions-Tabs
    private JTabbedPane tabbedPane;
    private JDateChooser notificationdatum;
    private JCheckBox chckbxBuchungenErstellen;

    private static final int PRENOTIFICATION_TAB_INDEX = 0;
    private static final int EXPORT_TAB_INDEX = 1;
    private static final int DRUCKEN_TAB_INDEX = 2;
    private static final int AUSGEFUEHRT_TAB_INDEX = 3;
    private static final int LOESCHEN_TAB_INDEX = 4;

    private static Logger logger = Logger
            .getLogger(LastschriftVerwaltenWindow.class.getName());

    /**
     * Erzeugt ein neues Lastschrift-Verwaltungs-Fenster.
     * 
     * @param sqlSessionFactory
     *            Zugriff auf die Datenbank
     * @param letterGenerator
     *            Brief-Erzeuger
     * @param conf
     *            Konfiguration des Nami-Beitrags-Tools
     */
    public LastschriftVerwaltenWindow(SqlSessionFactory sqlSessionFactory,
            LetterGenerator letterGenerator, NamiBeitragConfiguration conf) {
        super("Sammellastschriften verwalten");
        this.sqlSessionFactory = sqlSessionFactory;
        this.letterGenerator = letterGenerator;
        this.conf = conf;
        buildFrame();
    }

    private void buildFrame() {
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        MigLayout mainLayout = new MigLayout("", "[grow]", "[][][grow][][]");
        contentPane.setLayout(mainLayout);

        /*** Rechnungs-Filter ***/
        contentPane.add(createFilterPanel(), "cell 0 0,grow");

        /*** Tabellen ***/
        // Rechnungen
        JLabel lblSammellastschriften = new JLabel("Sammellastschriften:");
        contentPane.add(lblSammellastschriften, "cell 0 1");

        sammellastModel = new SammellastModel();
        sammellastTable = new JTable();
        sammellastTable.setModel(sammellastModel);
        sammellastTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sammellastTable.getSelectionModel().addListSelectionListener(
                new SammellastSelectionListener());
        // Setzt den Renderer, der dafür sorgt, dass Checkboxen, die nicht
        // bearbeitet werden können, disabled werden
        sammellastTable.getColumnModel()
                .getColumn(SammellastModel.AUSGEFUEHRT_COLUMN_INDEX)
                .setCellRenderer(new DisabledCellRenderer());

        JScrollPane sammellastScrollPane = new JScrollPane();
        contentPane.add(sammellastScrollPane, "cell 0 2,growx");
        sammellastScrollPane.setViewportView(sammellastTable);

        JLabel lblEinzellastschriften = new JLabel(
                "Enthaltene Einzellastschriften:");
        contentPane.add(lblEinzellastschriften, "cell 0 3");

        einzellastModel = new EinzellastModel();
        einzellastTable = new JTable();
        einzellastTable.setModel(einzellastModel);
        einzellastTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        einzellastScrollPane = new JScrollPane();
        contentPane.add(einzellastScrollPane, "cell 0 4,growx");
        einzellastScrollPane.setViewportView(einzellastTable);

        /*** Aktionen ***/
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        contentPane.add(tabbedPane, "cell 0 5,growx");

        tabbedPane.addTab("Prenotification", null,
                createPrenotificationPanel(), null);
        tabbedPane.addTab("Export", null, createExportPanel(), null);
        tabbedPane.addTab("Drucken", null, createDruckenPanel(), null);
        tabbedPane.addTab("Ausgeführt", null, createAusgefuehrtPanel(), null);
        tabbedPane.addTab("Löschen", null, createLoeschenPanel(), null);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        pack();

        // Anfangskonfiguration
        refreshSammellastModelFromComponents();
        einzellastScrollPane.setVisible(false);
        tabbedPane.setVisible(false);
    }

    /**
     * Liefert die Lastschrift, die momentan in der Tabelle ausgewählt ist.
     * 
     * @return ausgewählte Lastschrift; <tt>null</tt>, falls keine ausgewählt
     *         ist
     */
    private BeitragSammelLastschrift getSelectedSammellast() {
        int row = sammellastTable.getSelectedRow();
        if (row != -1) {
            return sammellastModel.getSammellastAt(row);
        } else {
            return null;
        }
    }

    /**
     * Liefert die Einzellastschrift (und das Mandat), das momentan in der
     * unteren Tabelle ausgewählt ist.
     * 
     * @return ausgewählte Lastschrift; <tt>null</tt>, falls keine ausgewählt
     *         ist
     */
    private DataLastschriftMandat getSelectedEinzellast() {
        int row = einzellastTable.getSelectedRow();
        if (row != -1) {
            return einzellastModel.getEinzellastAt(row);
        } else {
            return null;
        }
    }

    /**
     * Aktualisiert die Einzellastschrift-Tabelle sowie die Aktions-Tabs bei
     * Auswahl einer Sammellastschrift aus der Liste.
     */
    private class SammellastSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                // das ist noch nicht das letzte Event der Folge
                // => aktualisiere Tabellen noch nicht
                return;
            }

            BeitragSammelLastschrift sammellast = getSelectedSammellast();
            if (sammellast != null) {
                int sammellastId = sammellast.getSammelLastschriftId();
                einzellastModel.loadPosten(sammellastId);
                einzellastScrollPane.setVisible(true);

                // Aktions-Tabs (de-)aktivieren
                tabbedPane.setVisible(true);
                int defaultTabIndex = 0;
                if (sammellast.isAusgefuehrt()) {
                    tabbedPane.setEnabledAt(PRENOTIFICATION_TAB_INDEX, true);
                    tabbedPane.setEnabledAt(EXPORT_TAB_INDEX, true);
                    tabbedPane.setEnabledAt(DRUCKEN_TAB_INDEX, true);
                    tabbedPane.setEnabledAt(AUSGEFUEHRT_TAB_INDEX, false);
                    tabbedPane.setEnabledAt(LOESCHEN_TAB_INDEX, false);
                    defaultTabIndex = DRUCKEN_TAB_INDEX;
                } else {
                    tabbedPane.setEnabledAt(PRENOTIFICATION_TAB_INDEX, true);
                    tabbedPane.setEnabledAt(EXPORT_TAB_INDEX, true);
                    tabbedPane.setEnabledAt(DRUCKEN_TAB_INDEX, true);
                    tabbedPane.setEnabledAt(AUSGEFUEHRT_TAB_INDEX, true);
                    tabbedPane.setEnabledAt(LOESCHEN_TAB_INDEX, true);
                    defaultTabIndex = PRENOTIFICATION_TAB_INDEX;
                }

                if (!tabbedPane.isEnabledAt(tabbedPane.getSelectedIndex())) {
                    // aktuell gewähltes Tab ist disabled -> wähle Standard
                    tabbedPane.setSelectedIndex(defaultTabIndex);
                }
            } else {
                // Leere Tabellen
                einzellastModel.emptyModel();
                einzellastScrollPane.setVisible(false);

                // Aktions-Tabs ausblenden
                tabbedPane.setVisible(false);
            }
        }
    }

    private JPanel createFilterPanel() {
        JPanel filterPanel = new JPanel();
        filterPanel.setBorder(new TitledBorder(null, "Filter",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        filterPanel.setLayout(new MigLayout("", "[]", "[][][]"));

        SammellastRefreshListener refreshListener = new SammellastRefreshListener();

        // Status
        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(null);
        filterPanel.add(statusPanel, "cell 0 1,alignx left,aligny top");
        rdbtnStatusOffen = new JRadioButton("Offen");
        rdbtnStatusAusgefuehrt = new JRadioButton("Ausgeführt");
        rdbtnStatusAlle = new JRadioButton("Alle");
        statusPanel.setLayout(new MigLayout("insets 0", "[]", "[]"));
        statusPanel.add(rdbtnStatusOffen, "alignx left,aligny top");
        statusPanel.add(rdbtnStatusAusgefuehrt, "alignx left,aligny top");
        statusPanel.add(rdbtnStatusAlle, "alignx left,aligny top");

        ButtonGroup statusGrp = new ButtonGroup();
        statusGrp.add(rdbtnStatusOffen);
        statusGrp.add(rdbtnStatusAusgefuehrt);
        statusGrp.add(rdbtnStatusAlle);
        rdbtnStatusOffen.addItemListener(refreshListener);
        rdbtnStatusAusgefuehrt.addItemListener(refreshListener);
        rdbtnStatusAlle.addItemListener(refreshListener);
        rdbtnStatusOffen.setSelected(true);

        return filterPanel;
    }

    /**
     * Aktualisiert die Lastschriftliste beim Ändern der Filter-Parameter.
     */
    private class SammellastRefreshListener implements ItemListener,
            ChangeListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            refreshSammellastModelFromComponents();
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            refreshSammellastModelFromComponents();
        }
    }

    private JPanel createPrenotificationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("", "[][grow]", "[][][][]"));

        JLabel lblRechnungsdatum = new JLabel("Datum der Prenotification:");
        panel.add(lblRechnungsdatum, "cell 0 0");

        notificationdatum = new JDateChooser();
        notificationdatum.setDate(new Date());
        panel.add(notificationdatum, "cell 1 0,growx");

        JButton btnDauerhaftePrenotification = new JButton(
                "Dauerhafte Prenotification erzeugen für Mandate, "
                        + "wo noch keine existiert");
        btnDauerhaftePrenotification
                .addActionListener(new PrenotificationAction(
                        PrenotificationType.DAUERHAFT));
        panel.add(btnDauerhaftePrenotification, "cell 0 1,span");

        JButton btnEinzelDauerhaftPrenotification = new JButton(
                "Dauerhafte Prenotification für das markierte Mandat erzeugen");
        btnEinzelDauerhaftPrenotification
                .addActionListener(new PrenotificationAction(
                        PrenotificationType.EINZELN_DAUERHAFT));
        panel.add(btnEinzelDauerhaftPrenotification, "cell 0 2,span");

        JButton btnEinmaligePrenotification = new JButton(
                "Einmalige Prenotification für alle Mandate erzeugen");
        btnEinmaligePrenotification
                .addActionListener(new PrenotificationAction(
                        PrenotificationType.EINMALIG));
        panel.add(btnEinmaligePrenotification, "cell 0 3,span");

        JButton btnEinzelPrenotification = new JButton(
                "Einmalige Prenotification für das markierte Mandat erzeugen");
        btnEinzelPrenotification.addActionListener(new PrenotificationAction(
                PrenotificationType.EINZELN_EINMALIG));
        panel.add(btnEinzelPrenotification, "cell 0 4,span");

        return panel;
    }

    /**
     * Die verschiedenen Möglichkeiten eine Prenotification zu erzeugen.
     */
    private enum PrenotificationType {
        /**
         * Erstellt eine dauerhaft gültige Prenotification für alle
         * Lastschriften, für die bisher keine gültige existiert.
         */
        DAUERHAFT,

        /**
         * Erstellt eine einmalig gültige Prenotification für alle
         * Lastschriften.
         */
        EINMALIG,

        /**
         * Erstellt eine dauerhaft gültige Prenotification für die markierte
         * Lastschrift.
         */
        EINZELN_DAUERHAFT,

        /**
         * Erstellt eine einmalig gültige Prenotification für die markierte
         * Lastschrift.
         */
        EINZELN_EINMALIG
    }

    /**
     * Erzeugt eine oder mehrere Prenotifications bei Auswahl der entsprechenden
     * Buttons.
     */
    private final class PrenotificationAction implements ActionListener {
        private PrenotificationType type;

        private PrenotificationAction(PrenotificationType type) {
            this.type = type;
        }

        private BeitragPrenotification createPrenotification(
                BeitragSammelLastschrift sl, DataLastschriftMandat row) {
            BeitragPrenotification pre = new BeitragPrenotification();
            pre.setMandatId(row.getMandat().getMandatId());
            pre.setDatum(notificationdatum.getDate());
            pre.setBetrag(row.getLastschrift().getBetrag());
            pre.setFaelligkeit(sl.getFaelligkeit());
            pre.setRegelmaessig(true);
            return pre;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            BeitragSammelLastschrift sl = getSelectedSammellast();
            BeitragPrenotification pre;

            // IDs der erzeugten Prenotifications (um später die Briefe zu
            // erzeugen)
            LinkedList<Integer> prenotIds = new LinkedList<>();
            // Nimmt jeweils das letzte Mandat auf, für das eine
            // Prenotification erzeugt wurde.
            // Wenn also nur eine Prenotification erzeugt wird, sind hierin
            // die entsprechenden Mandats-Daten gespeichert
            BeitragSepaMandat lastMandat = null;

            SqlSession session = sqlSessionFactory.openSession();
            try {
                LastschriftenMapper mapper = session
                        .getMapper(LastschriftenMapper.class);

                if (type == PrenotificationType.DAUERHAFT) {
                    ArrayList<DataLastschriftMandat> rows = mapper
                            .getLastschriften(sl.getSammelLastschriftId());

                    for (DataLastschriftMandat row : rows) {
                        // Prüfe, ob es bereits eine Prenotification gibt
                        int mandatId = row.getMandat().getMandatId();
                        BigDecimal betrag = row.getLastschrift().getBetrag();
                        if (!mapper
                                .existsValidPrenotification(mandatId, betrag)) {
                            pre = createPrenotification(sl, row);
                            pre.setRegelmaessig(true);
                            mapper.insertPrenotification(pre);

                            prenotIds.add(pre.getPrenotificationId());
                            lastMandat = row.getMandat();
                        }
                    }
                    session.commit();
                } else if (type == PrenotificationType.EINMALIG) {
                    ArrayList<DataLastschriftMandat> rows = mapper
                            .getLastschriften(sl.getSammelLastschriftId());

                    for (DataLastschriftMandat row : rows) {
                        pre = createPrenotification(sl, row);
                        pre.setRegelmaessig(false);
                        mapper.insertPrenotification(pre);

                        prenotIds.add(pre.getPrenotificationId());
                        lastMandat = row.getMandat();
                    }
                    session.commit();
                } else if (type == PrenotificationType.EINZELN_EINMALIG
                        || type == PrenotificationType.EINZELN_DAUERHAFT) {
                    DataLastschriftMandat selected = getSelectedEinzellast();

                    if (selected != null) {
                        pre = createPrenotification(sl, selected);
                        if (type == PrenotificationType.EINZELN_DAUERHAFT) {
                            pre.setRegelmaessig(true);
                        } else {
                            pre.setRegelmaessig(false);
                        }
                        mapper.insertPrenotification(pre);
                        session.commit();

                        prenotIds.add(pre.getPrenotificationId());
                        lastMandat = selected.getMandat();
                    } else {
                        logger.severe("Keine Einzellastschrift für "
                                + "Prenotification ausgewählt.");
                    }
                }
            } finally {
                session.close();
            }

            if (prenotIds.isEmpty()) {
                logger.log(Level.INFO, "Keine Prenotifications erzeugt");
            } else if (prenotIds.size() == 1) {
                letterGenerator.generateLetter(LetterType.PRENOTIFICATION,
                        prenotIds.getFirst(), notificationdatum.getDate(),
                        lastMandat.getKontoinhaber(), null);
            } else {
                letterGenerator.generateLetters(LetterType.PRENOTIFICATION,
                        prenotIds, notificationdatum.getDate());
            }
        }
    }

    private JPanel createExportPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("", "[][grow]", "[][][][]"));

        JLabel lblExportErklaerung = new JLabel(
                "<html>Es wird immer die komplette Sammellastschrift exportiert.<br />"
                        + "Damit der Export funktioniert, muss Hibiscus im Hintergrund "
                        + "laufen und entsprechend konfiguriert sein.");
        panel.add(lblExportErklaerung, "flowx,cell 0 0");

        JButton btnSammellastschriftExport = new JButton(
                "Als Sammellastschrift an Hibiscus senden");
        btnSammellastschriftExport.addActionListener(new ExportAction(
                HibiscusExportType.SAMMEL));
        panel.add(btnSammellastschriftExport, "cell 0 1,span");

        JButton btnEinzellastschriftExport = new JButton(
                "Alle als Einzellastschriften an Hibiscus senden");
        btnEinzellastschriftExport.addActionListener(new ExportAction(
                HibiscusExportType.ALLE_EINZEL));
        panel.add(btnEinzellastschriftExport, "cell 0 2,span");

        JButton btnEinzellastschriftSingleExport = new JButton(
                "Markierte als Einzellastschrift an Hibiscus senden");
        btnEinzellastschriftSingleExport.addActionListener(new ExportAction(
                HibiscusExportType.MARKIERT_EINZEL));
        panel.add(btnEinzellastschriftSingleExport, "cell 0 3,span");

        return panel;
    }

    /**
     * Beschreibt die verschiedenen Möglichkeiten eine Sammellastschrift an
     * Hibiscus zu senden.
     */
    private enum HibiscusExportType {
        /**
         * Sendet eine Sammellastschrift gesammelt an Hibiscus.
         */
        SAMMEL,

        /**
         * Sendet alle enthaltenen Lastschriften als Einzellastschriften an
         * Hibiscus.
         */
        ALLE_EINZEL,

        /**
         * Sendet nur die markierte Einzellastschrift einzeln an Hibiscus.
         */
        MARKIERT_EINZEL
    }

    /**
     * Sendet die Sammellastschrift (ggf. als einzelne Lastschriften) an
     * Hibiscus.
     */
    private final class ExportAction implements ActionListener {
        private HibiscusExportType type;

        private ExportAction(HibiscusExportType type) {
            this.type = type;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            BeitragSammelLastschrift sl = getSelectedSammellast();
            HibiscusExporter exporter = new HibiscusExporter(conf,
                    sqlSessionFactory);

            if (type == HibiscusExportType.ALLE_EINZEL) {
                SqlSession session = sqlSessionFactory.openSession();
                try {
                    LastschriftenMapper mapper = session
                            .getMapper(LastschriftenMapper.class);
                    ArrayList<DataLastschriftMandat> rows = mapper
                            .getLastschriften(sl.getSammelLastschriftId());

                    for (DataLastschriftMandat row : rows) {
                        exporter.exportLastschrift(row, sl.getFaelligkeit());
                    }
                } finally {
                    session.close();
                }
            } else if (type == HibiscusExportType.MARKIERT_EINZEL) {
                DataLastschriftMandat selected = getSelectedEinzellast();
                if (selected != null) {
                    exporter.exportLastschrift(selected, sl.getFaelligkeit());
                } else {
                    logger.severe("Keine Einzellastschrift für Export ausgewählt.");
                }
            } else if (type == HibiscusExportType.SAMMEL) {
                SqlSession session = sqlSessionFactory.openSession();
                try {
                    LastschriftenMapper mapper = session
                            .getMapper(LastschriftenMapper.class);
                    ArrayList<DataLastschriftMandat> rows = mapper
                            .getLastschriften(sl.getSammelLastschriftId());

                    exporter.exportSammellastschrift(rows, sl);
                } finally {
                    session.close();
                }
            }

        }
    }

    private JPanel createDruckenPanel() {
        JPanel panel = new JPanel();

        JButton btnPdfExport = new JButton("Als PDF exportieren");
        btnPdfExport.addActionListener(new DruckenAction());
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel.add(btnPdfExport);

        return panel;
    }

    /**
     * Exportiert eine Lastschrift als Ausdruck.
     */
    private class DruckenAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO
            logger.severe("Not implemented");
        }
    }

    private JPanel createAusgefuehrtPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("", "[grow]", "[][][]"));

        JLabel lblmarkiertDieSammellastschrift = new JLabel(
                "<html>Markiert die Sammellastschrift als ausgeführt. "
                        + "Dabei werden entsprechende Buchungen in die "
                        + "Datenbank eingefügt.<br />"
                        + "Dieser Schritt kann nicht rückgängig gemacht werden.");
        panel.add(lblmarkiertDieSammellastschrift, "cell 0 0");

        chckbxBuchungenErstellen = new JCheckBox(
                "Buchungen erstellen und Rechnungen als bezahlt markieren");
        chckbxBuchungenErstellen.setSelected(false);
        panel.add(chckbxBuchungenErstellen, "cell 0 1,span");

        JButton btnAusgefuehrt = new JButton("Als ausgeführt markieren");
        btnAusgefuehrt.addActionListener(new AusgefuehrtAction());
        panel.add(btnAusgefuehrt, "cell 0 2");
        return panel;
    }

    /**
     * Markiert die Sammellastschrift als ausgeführt. Damit werden die
     * zugehörigen Rechnungen als "bezahlt" markiert und die entsprechenden
     * Buchungen zu den jeweiligen Mitgliedern in die Datenbank eingefügt.
     */
    private class AusgefuehrtAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            BeitragSammelLastschrift sl = getSelectedSammellast();

            SqlSession session = sqlSessionFactory.openSession();
            try {
                LastschriftenMapper lMapper = session
                        .getMapper(LastschriftenMapper.class);

                // ggf. Buchungen für Rechnungen erstellen
                if (chckbxBuchungenErstellen.isSelected()) {
                    BeitragMapper beitragMapper = session
                            .getMapper(BeitragMapper.class);
                    RechnungenMapper rMapper = session
                            .getMapper(RechnungenMapper.class);
                    List<BeitragRechnung> rechnungen = lMapper
                            .getRechnungenInSammelLastschrift(sl
                                    .getSammelLastschriftId());

                    for (BeitragRechnung rechnung : rechnungen) {
                        // Buchung für jedes Halbjahr der Rechnung erstellen
                        ArrayList<DataHalbjahrBetraege> betraege = rMapper
                                .getHalbjahrBetraege(rechnung.getRechnungId());
                        for (DataHalbjahrBetraege halbjahrBetrag : betraege) {
                            BeitragBuchung buchung = new BeitragBuchung();
                            buchung.setMitgliedId(rechnung.getMitgliedId());
                            buchung.setRechnungsNummer(rechnung
                                    .getCompleteRechnungsNummer());
                            buchung.setTyp(Buchungstyp.LASTSCHRIFT);
                            buchung.setDatum(sl.getFaelligkeit());
                            buchung.setBetrag(halbjahrBetrag.getBetrag()
                                    .negate());
                            buchung.setHalbjahr(halbjahrBetrag.getHalbjahr());
                            buchung.setVorausberechnung(false);
                            buchung.setKommentar("Lastschrift eingezogen");
                            beitragMapper.insertBuchung(buchung);
                        }

                        // Rechnung als bezahlt markieren
                        rechnung.setStatus(Rechnungsstatus.BEGLICHEN);
                        rMapper.updateRechnung(rechnung);
                    }
                }

                sl.setAusgefuehrt(true);
                lMapper.updateSammelLastschrift(sl);

                session.commit();

                // Sammellastschriften neu aus DB laden (da eine den Status
                // geändert hat und damit ggf. nicht mehr sichtbar ist)
                sammellastModel.forceUpdate();
            } finally {
                session.close();
            }
        }
    }

    private JPanel createLoeschenPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("", "[grow]", "[][][]"));

        JButton btnAusgefuehrt = new JButton("Einzellastschrift löschen");
        btnAusgefuehrt.addActionListener(new EinzelLoeschenAction());
        panel.add(btnAusgefuehrt, "cell 0 0");

        JButton btnLoeschen = new JButton("Sammellastschrift löschen");
        btnLoeschen.addActionListener(new SammelLoeschenAction());
        panel.add(btnLoeschen, "cell 0 1");

        return panel;
    }

    /**
     * Löscht eine Einzellastschrift aus der Datenbank.
     */
    private final class EinzelLoeschenAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            BeitragSammelLastschrift sl = getSelectedSammellast();
            DataLastschriftMandat ls = getSelectedEinzellast();

            if (sl == null || ls == null) {
                return;
            }

            int slId = sl.getSammelLastschriftId();
            int lsId = ls.getLastschrift().getLastschriftId();

            if (sl.isAusgefuehrt()) {
                logger.warning("Ausgeführte Lastschriften können nicht gelöscht werden.");
                return;
            }

            SqlSession session = sqlSessionFactory.openSession();
            try {
                LastschriftenMapper mapper = session
                        .getMapper(LastschriftenMapper.class);

                // Lastschrift und Verbindungen zu Rechnungen löschen
                mapper.deleteAllRechnungenFromLastschrift(lsId);
                mapper.deleteLastschrift(lsId);

                session.commit();
            } finally {
                session.close();
            }
            // Angezeigte Lastschriften neu aus DB laden
            einzellastModel.loadPosten(slId);
        }
    }

    /**
     * Löscht eine Sammellastschrift (inkl. der dazugehörigen
     * Einzellastschriften) aus der Datenbank.
     */
    private final class SammelLoeschenAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            BeitragSammelLastschrift sl = getSelectedSammellast();
            int slId = sl.getSammelLastschriftId();

            if (sl.isAusgefuehrt()) {
                logger.warning("Ausgeführte Lastschriften können nicht gelöscht werden.");
                return;
            }

            int dialogResult = JOptionPane.showConfirmDialog(
                    LastschriftVerwaltenWindow.this,
                    "Sicher, dass die Sammellastschrift gelöscht werden soll?",
                    "Warning", JOptionPane.YES_NO_OPTION);
            if (dialogResult != JOptionPane.YES_OPTION) {
                return;
            }

            SqlSession session = sqlSessionFactory.openSession();
            try {
                LastschriftenMapper mapper = session
                        .getMapper(LastschriftenMapper.class);

                // enthaltene Lastschriften und Verbindungen zu Rechnungen
                // löschen
                List<DataLastschriftMandat> lastschriften = mapper
                        .getLastschriften(slId);
                for (DataLastschriftMandat ls : lastschriften) {
                    int lsId = ls.getLastschrift().getLastschriftId();
                    mapper.deleteAllRechnungenFromLastschrift(lsId);
                    mapper.deleteLastschrift(lsId);
                }
                mapper.deleteSammelLastschrift(slId);

                session.commit();
            } finally {
                session.close();
            }
            // Angezeigte Lastschriften neu aus DB laden
            sammellastModel.forceUpdate();
        }
    }

    /**
     * Schreibt die Eingaben aus den Filter-Feldern in das TableModel.
     */
    private void refreshSammellastModelFromComponents() {
        if (sammellastModel == null) {
            // model ist noch nicht initialisiert
            return;
        }

        // Status
        if (rdbtnStatusOffen.isSelected()) {
            sammellastModel.setAusgefuehrt(false);
        } else if (rdbtnStatusAusgefuehrt.isSelected()) {
            sammellastModel.setAusgefuehrt(true);
        } else if (rdbtnStatusAlle.isSelected()) {
            sammellastModel.setAusgefuehrt(null);
        }

        sammellastModel.doUpdate();
    }

    /**
     * Model, das die Sammellastschriften, die den Filterkriterien entsprechen,
     * in einer Tabelle anzeigt.
     */
    private class SammellastModel extends AbstractTableModel {
        private static final long serialVersionUID = -8344734930779903398L;

        // Aktive Filterkriterien
        private Boolean ausgefuehrt = null;

        // Gibt an, ob Filterkriterien verändert wurden (das heißt es müssen
        // neue Daten aus der Datenbank geladen werden)
        private boolean needsReload = true;

        // Angezeigte Rechnungen
        private ArrayList<BeitragSammelLastschrift> lastschriften;

        private static final int ID_COLUMN_INDEX = 0;
        private static final int FAELLIGKEIT_COLUMN_INDEX = 1;
        private static final int BEZEICHNUNG_COLUMN_INDEX = 2;
        private static final int ANZAHL_COLUMN_INDEX = 3;
        private static final int BETRAG_COLUMN_INDEX = 4;
        private static final int AUSGEFUEHRT_COLUMN_INDEX = 5;

        // Methoden, die die Filterkriterien ändern. Beim Update werden die
        // Lastschriften nur dann aus der Tabelle geladen, wenn sich eines der
        // Kriterien tatsächlich verändert hat.
        public void setAusgefuehrt(Boolean ausgefuehrt) {
            if (this.ausgefuehrt != ausgefuehrt) {
                this.ausgefuehrt = ausgefuehrt;
                needsReload = true;
            }
        }

        /**
         * Lädt die angezeigten Sammellastschriften neu aus der Datenbank, falls
         * sich eines der Filterkriterien geändert hat.
         */
        public void doUpdate() {
            if (!needsReload) {
                return;
            }

            SqlSession session = sqlSessionFactory.openSession();
            try {
                LastschriftenMapper mapper = session
                        .getMapper(LastschriftenMapper.class);
                lastschriften = mapper.findSammelLastschriften(ausgefuehrt);
                needsReload = false;
                fireTableDataChanged();
            } finally {
                session.close();
            }
        }

        /**
         * Lädt die Sammellastschriften neu aus der Datenbank auch wenn sich
         * <i>keine</i> Filterkriterien geändert haben. Sinnvoll, wenn
         * Veränderungen in die Datenbank geschrieben wurden.
         */
        public void forceUpdate() {
            needsReload = true;
            doUpdate();
        }

        @Override
        public int getRowCount() {
            if (lastschriften != null) {
                return lastschriften.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (lastschriften == null || rowIndex >= lastschriften.size()) {
                return null;
            }

            BeitragSammelLastschrift sl = lastschriften.get(rowIndex);
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
            switch (columnIndex) {
            case ID_COLUMN_INDEX:
                return sl.getSammelLastschriftId();
            case FAELLIGKEIT_COLUMN_INDEX:
                return formatter.format(sl.getFaelligkeit());
            case BEZEICHNUNG_COLUMN_INDEX:
                return sl.getBezeichnung();
            case ANZAHL_COLUMN_INDEX:
                return sl.getAnzahlLastschriften();
            case BETRAG_COLUMN_INDEX:
                if (sl.getBetrag() != null) {
                    return sl.getBetrag().negate();
                } else {
                    return 0;
                }
            case AUSGEFUEHRT_COLUMN_INDEX:
                return sl.isAusgefuehrt();
            default:
                return null;
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case ID_COLUMN_INDEX:
                return "ID";
            case FAELLIGKEIT_COLUMN_INDEX:
                return "Fälligkeit";
            case BEZEICHNUNG_COLUMN_INDEX:
                return "Bezeichnung";
            case ANZAHL_COLUMN_INDEX:
                return "Anzahl Lastschriften";
            case BETRAG_COLUMN_INDEX:
                return "Betrag";
            case AUSGEFUEHRT_COLUMN_INDEX:
                return "Ausgeführt";
            default:
                return null;
            }
        }

        /**
         * Liefert die Sammellastschrift, die in einer bestimmten Zeile
         * angezeigt wird.
         * 
         * @param rowIndex
         *            gesuchte Zeile
         * @return Sammellastschrift in der Zeile
         */
        public BeitragSammelLastschrift getSammellastAt(int rowIndex) {
            if (lastschriften == null || rowIndex >= lastschriften.size()) {
                return null;
            }
            return lastschriften.get(rowIndex);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == AUSGEFUEHRT_COLUMN_INDEX) {
                return Boolean.class;
            }
            return super.getColumnClass(columnIndex);
        }
    }

    /**
     * Model, das die Einzellastschriften anzeigt, die zu einer bestimmten
     * Sammellastschrift gehören.
     */
    private class EinzellastModel extends AbstractTableModel {
        private static final long serialVersionUID = 5742831859991724534L;

        private static final int MANDATID_COLUMN_INDEX = 0;
        private static final int KONTOINHABER_COLUMN_INDEX = 1;
        private static final int IBAN_COLUMN_INDEX = 2;
        private static final int BIC_COLUMN_INDEX = 3;
        private static final int VERWENDUNGSZWECK_COLUMN_INDEX = 4;
        private static final int BETRAG_COLUMN_INDEX = 5;

        // Angezeigte Lastschriften
        private ArrayList<DataLastschriftMandat> lastschriftList = null;

        /**
         * Löscht alle Lastschriften und lädt keine neuen.
         */
        public void emptyModel() {
            lastschriftList = null;
            fireTableDataChanged();
        }

        /**
         * Ersetzt die angezeigten Lastschriften.
         * 
         * @param sammelLastschriftId
         *            ID der Sammellastschrift, deren Posten angezeigt werden
         *            sollen
         */
        public void loadPosten(int sammelLastschriftId) {
            SqlSession session = sqlSessionFactory.openSession();
            try {
                LastschriftenMapper mapper = session
                        .getMapper(LastschriftenMapper.class);
                lastschriftList = mapper.getLastschriften(sammelLastschriftId);
                fireTableDataChanged();
            } finally {
                session.close();
            }
        }

        @Override
        public int getRowCount() {
            if (lastschriftList != null) {
                return lastschriftList.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (lastschriftList == null || rowIndex >= lastschriftList.size()) {
                return null;
            }

            DataLastschriftMandat row = lastschriftList.get(rowIndex);
            BeitragLastschrift l = row.getLastschrift();
            BeitragSepaMandat m = row.getMandat();
            switch (columnIndex) {
            case MANDATID_COLUMN_INDEX:
                return l.getMandatId();
            case KONTOINHABER_COLUMN_INDEX:
                return m.getKontoinhaber();
            case IBAN_COLUMN_INDEX:
                return m.getIban();
            case BIC_COLUMN_INDEX:
                return m.getBic();
            case VERWENDUNGSZWECK_COLUMN_INDEX:
                return l.getVerwendungszweck();
            case BETRAG_COLUMN_INDEX:
                return l.getBetrag().negate();
            default:
                return null;
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case MANDATID_COLUMN_INDEX:
                return "Mandat-ID";
            case KONTOINHABER_COLUMN_INDEX:
                return "Kontoinhaber";
            case IBAN_COLUMN_INDEX:
                return "IBAN";
            case BIC_COLUMN_INDEX:
                return "BIC";
            case VERWENDUNGSZWECK_COLUMN_INDEX:
                return "Verwendungszweck";
            case BETRAG_COLUMN_INDEX:
                return "Betrag";
            default:
                return null;
            }
        }

        /**
         * Liefert die Lastschrift, die in einer bestimmten Zeile angezeigt
         * wird.
         * 
         * @param rowIndex
         *            gesuchte Zeile
         * @return Lastschrift in der Zeile
         */
        public DataLastschriftMandat getEinzellastAt(int rowIndex) {
            if (lastschriftList == null || rowIndex >= lastschriftList.size()) {
                return null;
            }
            return lastschriftList.get(rowIndex);
        }
    }

}
