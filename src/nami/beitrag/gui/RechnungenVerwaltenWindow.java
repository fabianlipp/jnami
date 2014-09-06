package nami.beitrag.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import nami.beitrag.Buchungstyp;
import nami.beitrag.Rechnungsstatus;
import nami.beitrag.db.BeitragBuchung;
import nami.beitrag.db.BeitragMahnung;
import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.BeitragRechnung;
import nami.beitrag.db.RechnungenMapper;
import nami.beitrag.db.RechnungenMapper.DataFindRechnungen;
import nami.beitrag.db.RechnungenMapper.DataHalbjahrBetraege;
import nami.beitrag.db.RechnungenMapper.DataListPosten;
import nami.beitrag.gui.utils.EnhancedJSpinner;
import nami.beitrag.letters.LetterGenerator;
import nami.beitrag.letters.LetterType;
import net.miginfocom.swing.MigLayout;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import com.toedter.calendar.JDateChooser;

/**
 * Stellt ein Fenster dar, in dem Rechnungen verwaltet werden können. Dazu
 * werden nach bestimmten Kriterien vorhandene Rechnungen aufgelistet.
 * Anschließend kann deren Status verändert werden (ggf. werden dabei auch die
 * passenden Buchungen erzeugt) oder Mahnungen erstellt werden.
 * 
 * @author Fabian Lipp
 * 
 */
public class RechnungenVerwaltenWindow extends JFrame {
    private static final long serialVersionUID = 7409328875312329467L;

    private SqlSessionFactory sqlSessionFactory;
    private LetterGenerator letterGenerator;

    // Komponenten für Filter
    private JCheckBox chckbxErstellungsjahr;
    private JSpinner filterErstellungsjahr;
    private JRadioButton rdbtnStatusUeberfaellig;
    private JRadioButton rdbtnStatusOffen;
    private JRadioButton rdbtnStatusBeglichen;
    private JRadioButton rdbtnStatusAbgeschrieben;
    private JRadioButton rdbtnStatusAlle;
    private MitgliedSelectComponent filterMitglied;

    // Dargestellte Tabellen
    private JTable rechnungenTable;
    private RechnungenModel rechnungenModel;

    private JScrollPane postenScrollPane;
    private JTable postenTable;
    private PostenModel postenModel;

    private JScrollPane mahnungenScrollPane;
    private JTable mahnungenTable;
    private MahnungenModel mahnungenModel;

    // Aktions-Tabs
    private JTabbedPane tabbedPane;
    private static final int BEZAHLT_TAB_INDEX = 0;
    private static final int MAHNUNG_TAB_INDEX = 1;
    private static final int ABSCHREIBEN_TAB_INDEX = 2;
    private static final int OFFEN_TAB_INDEX = 3;

    // Bezahlt-Panel
    private JTable halbjahrBetraegeTable;
    private JScrollPane halbjahrBetraegeScrollPane;
    private HalbjahrBetraegeModel halbjahrBetraegeModel;
    private JCheckBox chckbxBuchungenErstellen;
    private JDateChooser inputBuchungsdatum;
    private JComboBox<Buchungstyp> inputBuchungstyp;

    // Mahnung-Panel
    private JDateChooser inputMahnungDatum;
    private JDateChooser inputFristDatum;
    private int nextMahnungArt;
    private JLabel lblMahnungArt;

    /**
     * Erzeugt ein neues Rechnungs-Fenster.
     * 
     * @param sqlSessionFactory
     *            Zugriff auf die Datenbank
     * @param letterGenerator
     *            Generator für Briefe
     */
    public RechnungenVerwaltenWindow(SqlSessionFactory sqlSessionFactory,
            LetterGenerator letterGenerator) {
        super("Rechnungen verwalten");
        this.sqlSessionFactory = sqlSessionFactory;
        this.letterGenerator = letterGenerator;
        buildFrame();
    }

    private static void setTablePreferredHeightInRows(JTable table, int rowCount) {
        Dimension dim = table.getPreferredScrollableViewportSize();
        int height = table.getRowHeight() * rowCount;
        table.setPreferredScrollableViewportSize(new Dimension(dim.width,
                height));
    }

    private void buildFrame() {
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        MigLayout mainLayout = new MigLayout("", "[grow]",
                "[][][grow][][pref!][][pref!][pref!]");
        contentPane.setLayout(mainLayout);

        /*** Rechnungs-Filter ***/
        contentPane.add(createFilterPanel(), "cell 0 0,grow");

        /*** Tabellen ***/
        // Rechnungen
        JLabel lblRechnungen = new JLabel("Rechnungen:");
        contentPane.add(lblRechnungen, "cell 0 1");

        rechnungenModel = new RechnungenModel();
        rechnungenTable = new JTable();
        rechnungenTable.setModel(rechnungenModel);
        rechnungenTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rechnungenTable.getSelectionModel().addListSelectionListener(
                new RechnungenSelectionListener());

        JScrollPane rechnungenScrollPane = new JScrollPane();
        contentPane.add(rechnungenScrollPane, "cell 0 2,growx");
        rechnungenScrollPane.setViewportView(rechnungenTable);

        // Posten
        JLabel lblPosten = new JLabel("Posten:");
        contentPane.add(lblPosten, "cell 0 3");

        postenModel = new PostenModel();
        postenTable = new JTable();
        postenTable.setModel(postenModel);
        setTablePreferredHeightInRows(postenTable, 5);

        postenScrollPane = new JScrollPane();
        postenScrollPane.setViewportView(postenTable);
        contentPane.add(postenScrollPane, "cell 0 4,growx");

        // Mahnungen
        JLabel lblMahnungen = new JLabel("Mahnungen:");
        contentPane.add(lblMahnungen, "cell 0 5");

        mahnungenModel = new MahnungenModel();
        mahnungenTable = new JTable();
        mahnungenTable.setModel(mahnungenModel);
        setTablePreferredHeightInRows(mahnungenTable, 3);

        mahnungenScrollPane = new JScrollPane();
        mahnungenScrollPane.setViewportView(mahnungenTable);
        contentPane.add(mahnungenScrollPane, "cell 0 6,growx");

        /*** Aktionen ***/
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        contentPane.add(tabbedPane, "cell 0 7,growx");

        tabbedPane.addTab("Bezahlt", null, createBezahltPanel(), null);
        tabbedPane.addTab("Mahnung", null, createMahnungPanel(), null);
        tabbedPane.addTab("Abschreiben", null, createAbschreibenPanel(), null);
        tabbedPane.addTab("Offen", null, createOffenPanel(), null);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        pack();

        // Anfangskonfiguration
        refreshRechnungenModelFromComponents();
        // Diese Elemente werden wieder eingeblendet, wenn eine Rechnung
        // selektiert wird
        postenScrollPane.setVisible(false);
        mahnungenScrollPane.setVisible(false);
        tabbedPane.setVisible(false);
    }

    /**
     * Liefert die Rechnung, die momentan in der Tabelle ausgewählt ist.
     * 
     * @return ausgewählte Rechnung; <tt>null</tt>, falls keine ausgewählt ist
     */
    private BeitragRechnung getSelectedRechnung() {
        int row = rechnungenTable.getSelectedRow();
        if (row != -1) {
            return rechnungenModel.getRechnungAt(row);
        } else {
            return null;
        }
    }

    /**
     * Aktualisiert die Posten- und Mahnungstabelle sowie die Aktions-Tabs bei
     * Auswahl einer Rechnung aus der Liste.
     */
    private class RechnungenSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                // das ist noch nicht das letzte Event der Folge
                // => aktualisiere Tabellen noch nicht
                return;
            }

            BeitragRechnung rechnung = getSelectedRechnung();
            if (rechnung != null) {
                // Alle Steuerelemente anpassen
                int rechnungId = rechnung.getRechnungId();
                postenModel.loadPosten(rechnungId);
                postenScrollPane.setVisible(true);
                mahnungenModel.loadMahnungen(rechnungId);
                mahnungenScrollPane.setVisible(true);
                halbjahrBetraegeModel.loadBetraege(rechnungId);
                nextMahnungArt = rechnungenModel.getMahnungenAt(rechnungenTable
                        .getSelectedRow()) + 1;
                lblMahnungArt.setText(Integer.toString(nextMahnungArt));

                // Aktions-Tabs (de-)aktivieren
                Rechnungsstatus status = rechnung.getStatus();
                tabbedPane.setVisible(true);
                int defaultTabIndex = 0;
                switch (status) {
                case OFFEN:
                    tabbedPane.setEnabledAt(BEZAHLT_TAB_INDEX, true);
                    tabbedPane.setEnabledAt(MAHNUNG_TAB_INDEX, true);
                    tabbedPane.setEnabledAt(ABSCHREIBEN_TAB_INDEX, true);
                    tabbedPane.setEnabledAt(OFFEN_TAB_INDEX, false);
                    defaultTabIndex = BEZAHLT_TAB_INDEX;
                    break;
                case BEGLICHEN:
                    tabbedPane.setEnabledAt(BEZAHLT_TAB_INDEX, false);
                    tabbedPane.setEnabledAt(MAHNUNG_TAB_INDEX, false);
                    tabbedPane.setEnabledAt(ABSCHREIBEN_TAB_INDEX, false);
                    tabbedPane.setEnabledAt(OFFEN_TAB_INDEX, true);
                    defaultTabIndex = OFFEN_TAB_INDEX;
                    break;
                case ABGESCHRIEBEN:
                    tabbedPane.setEnabledAt(BEZAHLT_TAB_INDEX, true);
                    tabbedPane.setEnabledAt(MAHNUNG_TAB_INDEX, false);
                    tabbedPane.setEnabledAt(ABSCHREIBEN_TAB_INDEX, false);
                    tabbedPane.setEnabledAt(OFFEN_TAB_INDEX, true);
                    defaultTabIndex = BEZAHLT_TAB_INDEX;
                    break;
                default:
                }
                if (!tabbedPane.isEnabledAt(tabbedPane.getSelectedIndex())) {
                    // aktuell gewähltes Tab ist disabled -> wähle Standard
                    tabbedPane.setSelectedIndex(defaultTabIndex);
                }
            } else {
                // Leere Tabellen
                postenModel.emptyModel();
                postenScrollPane.setVisible(false);
                mahnungenModel.emptyModel();
                mahnungenScrollPane.setVisible(false);

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

        // Erstellungsjahr
        RechnungenRefreshListener refreshListener = new RechnungenRefreshListener();

        chckbxErstellungsjahr = new JCheckBox("Erstellungsjahr");
        chckbxErstellungsjahr
                .addItemListener(new ErstellungsjahrCheckboxListener());
        chckbxErstellungsjahr.addItemListener(refreshListener);
        filterPanel.add(chckbxErstellungsjahr, "flowx,cell 0 0");

        int year = Calendar.getInstance().get(Calendar.YEAR);
        SpinnerNumberModel numberModel = new SpinnerNumberModel(year, 0,
                year + 1, 1);
        filterErstellungsjahr = new EnhancedJSpinner(numberModel);
        // Zeige keinen Tausenderpunkt an
        filterErstellungsjahr.setEditor(new JSpinner.NumberEditor(
                filterErstellungsjahr, "0"));
        filterErstellungsjahr.setEnabled(chckbxErstellungsjahr.isSelected());
        filterErstellungsjahr.addChangeListener(refreshListener);
        filterPanel.add(filterErstellungsjahr, "cell 0 0");

        // Status
        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(null);
        // statusPanel.setBorder(new TitledBorder(null, "Status",
        // TitledBorder.LEADING, TitledBorder.TOP, null, null));
        filterPanel.add(statusPanel, "cell 0 1,alignx left,aligny top");

        rdbtnStatusUeberfaellig = new JRadioButton("Überfällig");
        rdbtnStatusUeberfaellig.setSelected(true);
        rdbtnStatusOffen = new JRadioButton("Offen");
        rdbtnStatusBeglichen = new JRadioButton("Bezahlt");
        rdbtnStatusAbgeschrieben = new JRadioButton("Abgeschrieben");
        rdbtnStatusAlle = new JRadioButton("Alle");
        statusPanel.setLayout(new MigLayout("insets 0", "[]", "[]"));

        statusPanel.add(rdbtnStatusUeberfaellig, "alignx left,aligny top");
        statusPanel.add(rdbtnStatusOffen, "alignx left,aligny top");
        statusPanel.add(rdbtnStatusBeglichen, "alignx left,aligny top");
        statusPanel.add(rdbtnStatusAbgeschrieben, "alignx left,aligny top");
        statusPanel.add(rdbtnStatusAlle, "alignx left,aligny top");

        ButtonGroup statusGrp = new ButtonGroup();
        statusGrp.add(rdbtnStatusUeberfaellig);
        statusGrp.add(rdbtnStatusOffen);
        statusGrp.add(rdbtnStatusBeglichen);
        statusGrp.add(rdbtnStatusAbgeschrieben);
        statusGrp.add(rdbtnStatusAlle);

        rdbtnStatusUeberfaellig.addItemListener(refreshListener);
        rdbtnStatusOffen.addItemListener(refreshListener);
        rdbtnStatusBeglichen.addItemListener(refreshListener);
        rdbtnStatusAbgeschrieben.addItemListener(refreshListener);
        rdbtnStatusAlle.addItemListener(refreshListener);

        // Mitglied
        JLabel lblMitglied = new JLabel("Mitglied:");
        filterPanel.add(lblMitglied, "flowx,cell 0 2");

        filterMitglied = new MitgliedSelectComponent(sqlSessionFactory);
        filterMitglied.addChangeListener(refreshListener);
        filterPanel.add(filterMitglied, "cell 0 2");

        return filterPanel;
    }

    /**
     * Aktiviert das Eingabefeld für das Erstellungsjahr nur, wenn auch die
     * Checkbox aktiviert ist.
     */
    private class ErstellungsjahrCheckboxListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            filterErstellungsjahr
                    .setEnabled(chckbxErstellungsjahr.isSelected());
        }
    }

    /**
     * Aktualisiert die Rechnungsliste beim Ändern der Filter-Parameter.
     */
    private class RechnungenRefreshListener implements ItemListener,
            ChangeListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            refreshRechnungenModelFromComponents();
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            refreshRechnungenModelFromComponents();
        }
    }

    private JPanel createBezahltPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("", "[][grow]", "[][pref!][][][]"));

        chckbxBuchungenErstellen = new JCheckBox("Buchungen erstellen");
        chckbxBuchungenErstellen.setSelected(true);
        chckbxBuchungenErstellen
                .addItemListener(new BuchungenErstellenCheckboxListener());
        panel.add(chckbxBuchungenErstellen, "cell 0 0,span");

        halbjahrBetraegeScrollPane = new JScrollPane();
        halbjahrBetraegeModel = new HalbjahrBetraegeModel();
        halbjahrBetraegeTable = new JTable();
        halbjahrBetraegeTable.setModel(halbjahrBetraegeModel);
        setTablePreferredHeightInRows(halbjahrBetraegeTable, 2);
        halbjahrBetraegeScrollPane.setViewportView(halbjahrBetraegeTable);
        panel.add(halbjahrBetraegeScrollPane, "cell 0 1,grow,span");

        JLabel lblBuchungsdatum = new JLabel("Buchungsdatum:");
        panel.add(lblBuchungsdatum, "flowx,cell 0 2");
        inputBuchungsdatum = new JDateChooser(new Date());
        panel.add(inputBuchungsdatum, "cell 1 2");

        JLabel lblBuchungstyp = new JLabel("Buchungstyp:");
        panel.add(lblBuchungstyp, "flowx,cell 0 3");
        Buchungstyp[] allowedTypes = new Buchungstyp[] { Buchungstyp.BAR,
                Buchungstyp.LASTSCHRIFT, Buchungstyp.UEBERWEISUNG };
        inputBuchungstyp = new JComboBox<Buchungstyp>(allowedTypes);
        inputBuchungstyp.setSelectedItem(Buchungstyp.UEBERWEISUNG);
        panel.add(inputBuchungstyp, "cell 1 3");

        JButton btnRechnungBezahlt = new JButton("Rechnung bezahlt");
        btnRechnungBezahlt.addActionListener(new BezahltAction());
        panel.add(btnRechnungBezahlt, "cell 0 4,span,alignx right");

        return panel;
    }

    /**
     * Reagiert auf die Buchungs-Erstellen-Checkbox und (de-)aktiviert die
     * anderen Komponenten auf dem Panel dementsprechend.
     */
    private class BuchungenErstellenCheckboxListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent e) {
            boolean state = chckbxBuchungenErstellen.isSelected();
            halbjahrBetraegeTable.setEnabled(state);
            inputBuchungsdatum.setEnabled(state);
        }
    }

    /**
     * Markiert die Rechnung als bezahlt und fügt ggf. die nötigen Buchungen in
     * die Datenbank ein.
     */
    private class BezahltAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            BeitragRechnung rechnung = getSelectedRechnung();

            SqlSession session = sqlSessionFactory.openSession();
            try {
                if (chckbxBuchungenErstellen.isSelected()) {
                    BeitragMapper beitragMapper = session
                            .getMapper(BeitragMapper.class);

                    // Buchung für jedes Halbjahr der Rechnung erstellen
                    ArrayList<DataHalbjahrBetraege> betraege;
                    betraege = halbjahrBetraegeModel.betraege;
                    for (DataHalbjahrBetraege halbjahrBetrag : betraege) {
                        BeitragBuchung buchung = new BeitragBuchung();
                        buchung.setMitgliedId(rechnung.getMitgliedId());
                        buchung.setRechnungsNummer(rechnung
                                .getCompleteRechnungsNummer());
                        buchung.setTyp(inputBuchungstyp
                                .getItemAt(inputBuchungstyp.getSelectedIndex()));
                        buchung.setDatum(inputBuchungsdatum.getDate());
                        buchung.setBetrag(halbjahrBetrag.getBetrag().negate());
                        buchung.setHalbjahr(halbjahrBetrag.getHalbjahr());
                        buchung.setVorausberechnung(false);
                        buchung.setKommentar("Rechnung bezahlt");
                        beitragMapper.insertBuchung(buchung);
                    }
                }

                RechnungenMapper rechnungenMapper = session
                        .getMapper(RechnungenMapper.class);
                rechnung.setStatus(Rechnungsstatus.BEGLICHEN);
                rechnungenMapper.updateRechnung(rechnung);
                session.commit();
            } finally {
                session.close();
            }
            // Rechnungen neu aus DB laden (da eine Rechnung den Status geändert
            // hat und damit ggf. nicht mehr sichtbar ist)
            rechnungenModel.forceUpdate();
        }
    }

    private JPanel createMahnungPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("", "[][]", "[][][][]"));

        JLabel lblDatum = new JLabel("Datum:");
        panel.add(lblDatum, "cell 0 0");
        inputMahnungDatum = new JDateChooser(new Date());
        inputMahnungDatum
                .addPropertyChangeListener(new RechnungsdatumAendernListener());
        panel.add(inputMahnungDatum, "cell 1 0,grow");

        JLabel lblFrist = new JLabel("Frist:");
        panel.add(lblFrist, "cell 0 1");
        inputFristDatum = new JDateChooser(getFristForDatum(new Date()));
        panel.add(inputFristDatum, "cell 1 1,grow");

        JLabel lblTyp = new JLabel("Typ:");
        panel.add(lblTyp, "cell 0 2");
        lblMahnungArt = new JLabel();
        panel.add(lblMahnungArt, "cell 1 2,grow");

        JButton btnMahnungErstellen = new JButton("Mahnung erstellen");
        btnMahnungErstellen.addActionListener(new MahnungAction());
        panel.add(btnMahnungErstellen, "cell 0 3,span,alignx right");

        return panel;
    }

    /**
     * Erstellt eine neue Mahnung als Brief und fügt sie in die Datenbank ein.
     */
    private class MahnungAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            BeitragRechnung rechnung = getSelectedRechnung();
            if (rechnung == null) {
                return;
            }

            BeitragMahnung mahnung = new BeitragMahnung();
            mahnung.setRechnungId(rechnung.getRechnungId());
            mahnung.setDatum(inputMahnungDatum.getDate());
            mahnung.setFrist(inputFristDatum.getDate());
            mahnung.setMahnungArt(nextMahnungArt);

            SqlSession session = sqlSessionFactory.openSession();
            try {
                RechnungenMapper rechnungenMapper = session
                        .getMapper(RechnungenMapper.class);
                rechnungenMapper.insertMahnung(mahnung);
                session.commit();

                String nachname = rechnungenModel.getNachnameAt(rechnungenTable
                        .getSelectedRow());
                String vorname = rechnungenModel.getVornameAt(rechnungenTable
                        .getSelectedRow());
                letterGenerator.generateLetter(LetterType.MAHNUNG,
                        mahnung.getMahnungId(), mahnung.getDatum(), nachname,
                        vorname);
            } finally {
                session.close();
            }
            // Rechnungen neu aus DB laden (da eine Rechnung die Frist geändert
            // hat und damit ggf. nicht mehr sichtbar ist)
            rechnungenModel.forceUpdate();
        }
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
     * Passt die Frist an, wenn das Rechnungsdatum geändert wird.
     */
    private class RechnungsdatumAendernListener implements
            PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent e) {
            if ("date".equals(e.getPropertyName())) {
                inputFristDatum.setDate(getFristForDatum(inputMahnungDatum
                        .getDate()));
            }
        }
    }

    private JPanel createAbschreibenPanel() {
        JPanel panel = new JPanel();

        JButton btnAbschreiben = new JButton("Rechnung abschreiben");
        btnAbschreiben.addActionListener(new ChangeStateAction(
                Rechnungsstatus.ABGESCHRIEBEN));
        panel.add(btnAbschreiben);
        return panel;
    }

    private JPanel createOffenPanel() {
        JPanel panel = new JPanel();

        JButton btnOffen = new JButton("Als offen Markieren");
        btnOffen.addActionListener(new ChangeStateAction(Rechnungsstatus.OFFEN));
        panel.add(btnOffen);
        return panel;
    }

    /**
     * Verändert den Status der markierten Rechnung in der Datenbank ohne
     * weitere Aktionen auszuführen. Der Status, den die Rechnungen bekommen,
     * wird im Konstruktor angegeben.
     */
    private final class ChangeStateAction implements ActionListener {
        // Status den die Rechnungen bekommen sollen
        private Rechnungsstatus state;

        private ChangeStateAction(Rechnungsstatus state) {
            this.state = state;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            BeitragRechnung rechnung = getSelectedRechnung();

            SqlSession session = sqlSessionFactory.openSession();
            try {
                RechnungenMapper rechnungenMapper = session
                        .getMapper(RechnungenMapper.class);
                rechnung.setStatus(state);
                rechnungenMapper.updateRechnung(rechnung);
                session.commit();
            } finally {
                session.close();
            }
            // Rechnungen neu aus DB laden (da eine Rechnung den Status geändert
            // hat und damit ggf. nicht mehr sichtbar ist)
            rechnungenModel.forceUpdate();
        }
    }

    /**
     * Schreibt die Eingaben aus den Filter-Feldern in das TableModel.
     */
    private void refreshRechnungenModelFromComponents() {
        if (rechnungenModel == null) {
            // model ist noch nicht initialisiert
            return;
        }

        // Erstellungsjahr
        if (chckbxErstellungsjahr.isSelected()) {
            rechnungenModel.setErstellungsjahr((Integer) filterErstellungsjahr
                    .getValue());
        } else {
            rechnungenModel.setErstellungsjahr(-1);
        }

        // Status
        if (rdbtnStatusUeberfaellig.isSelected()) {
            rechnungenModel.setUeberfaellig(true);
        } else {
            rechnungenModel.setUeberfaellig(false);
            if (rdbtnStatusOffen.isSelected()) {
                rechnungenModel.setStatus(Rechnungsstatus.OFFEN);
            } else if (rdbtnStatusBeglichen.isSelected()) {
                rechnungenModel.setStatus(Rechnungsstatus.BEGLICHEN);
            } else if (rdbtnStatusAbgeschrieben.isSelected()) {
                rechnungenModel.setStatus(Rechnungsstatus.ABGESCHRIEBEN);
            } else if (rdbtnStatusAlle.isSelected()) {
                rechnungenModel.setStatus(null);
            }
        }

        // Mitglied-Filter
        rechnungenModel.setMitgliedId(filterMitglied.getMitgliedId());

        rechnungenModel.doUpdate();
    }

    /**
     * Model, das die Rechnungen, die den Filterkriterien entsprechen, in einer
     * Tabelle anzeigt.
     */
    private class RechnungenModel extends AbstractTableModel {
        private static final long serialVersionUID = -8344734930779903398L;

        // Aktive Filterkriterien
        private int erstellungsjahr;
        private Rechnungsstatus status;
        private boolean ueberfaellig;
        private int mitgliedId;

        // Gibt an, ob Filterkriterien verändert wurden (das heißt es müssen
        // neue Daten aus der Datenbank geladen werden)
        private boolean needsReload = true;

        // Angezeigte Rechnungen
        private ArrayList<DataFindRechnungen> rechnungen;

        private static final int ID_COLUMN_INDEX = 0;
        private static final int RECHNUNGSNR_COLUMN_INDEX = 1;
        private static final int EMPFAENGER_COLUMN_INDEX = 2;
        private static final int BETRAG_COLUMN_INDEX = 3;
        private static final int DATUM_COLUMN_INDEX = 4;
        private static final int FRIST_COLUMN_INDEX = 5;
        private static final int MAHNUNGEN_COLUMN_INDEX = 6;
        private static final int LETZTE_FRIST_COLUMN_INDEX = 7;
        private static final int STATUS_COLUMN_INDEX = 8;

        // Methoden, die die Filterkriterien ändern. Beim Update werden die
        // Rechnungen nur dann aus der Tabelle geladen, wenn sich eines der
        // Kriterien tatsächlich verändert hat.
        public void setErstellungsjahr(int erstellungsjahr) {
            if (this.erstellungsjahr != erstellungsjahr) {
                this.erstellungsjahr = erstellungsjahr;
                needsReload = true;
            }
        }

        public void setStatus(Rechnungsstatus status) {
            if (this.status != status) {
                this.status = status;
                needsReload = true;
            }
        }

        public void setUeberfaellig(boolean ueberfaellig) {
            if (this.ueberfaellig != ueberfaellig) {
                this.ueberfaellig = ueberfaellig;
                // nur offene Rechnungen können überfällig sein
                if (ueberfaellig) {
                    status = Rechnungsstatus.OFFEN;
                }
                needsReload = true;
            }
        }

        public void setMitgliedId(int mitgliedId) {
            if (this.mitgliedId != mitgliedId) {
                this.mitgliedId = mitgliedId;
                needsReload = true;
            }
        }

        /**
         * Lädt die angezeigten Rechnungen neu aus der Datenbank, falls sich
         * eines der Filterkriterien geändert hat.
         */
        public void doUpdate() {
            if (!needsReload) {
                return;
            }

            SqlSession session = sqlSessionFactory.openSession();
            try {
                RechnungenMapper mapper = session
                        .getMapper(RechnungenMapper.class);
                rechnungen = mapper.findRechnungen(erstellungsjahr, status,
                        ueberfaellig, mitgliedId);
                needsReload = false;
                fireTableDataChanged();
            } finally {
                session.close();
            }
        }

        /**
         * Lädt die Rechnungen neu aus der Datenbank auch wenn sich <i>keine</i>
         * Filterkriterien geändert haben. Sinnvoll, wenn Veränderungen in die
         * Datenbank geschrieben wurden.
         */
        public void forceUpdate() {
            needsReload = true;
            doUpdate();
        }

        @Override
        public int getRowCount() {
            if (rechnungen != null) {
                return rechnungen.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 9;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rechnungen == null || rowIndex >= rechnungen.size()) {
                return null;
            }

            DataFindRechnungen row = rechnungen.get(rowIndex);
            BeitragRechnung rechnung = row.getRechnung();
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
            switch (columnIndex) {
            case ID_COLUMN_INDEX:
                return rechnung.getRechnungId();
            case RECHNUNGSNR_COLUMN_INDEX:
                return rechnung.getCompleteRechnungsNummer();
            case EMPFAENGER_COLUMN_INDEX:
                return row.getVorname() + " " + row.getNachname();
            case BETRAG_COLUMN_INDEX:
                return rechnung.getBetrag().negate();
            case DATUM_COLUMN_INDEX:
                return formatter.format(rechnung.getDatum());
            case FRIST_COLUMN_INDEX:
                return formatter.format(rechnung.getFrist());
            case MAHNUNGEN_COLUMN_INDEX:
                return row.getMahnungen();
            case LETZTE_FRIST_COLUMN_INDEX:
                if (row.getLetzteFrist() != null) {
                    return formatter.format(row.getLetzteFrist());
                } else {
                    return "";
                }
            case STATUS_COLUMN_INDEX:
                return rechnung.getStatus();
            default:
                return null;
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case ID_COLUMN_INDEX:
                return "ID";
            case RECHNUNGSNR_COLUMN_INDEX:
                return "Rechnungsnummer";
            case EMPFAENGER_COLUMN_INDEX:
                return "Empfänger";
            case BETRAG_COLUMN_INDEX:
                return "Betrag";
            case DATUM_COLUMN_INDEX:
                return "Datum";
            case FRIST_COLUMN_INDEX:
                return "Frist";
            case MAHNUNGEN_COLUMN_INDEX:
                return "Mahnungen";
            case LETZTE_FRIST_COLUMN_INDEX:
                return "Letzte Frist";
            case STATUS_COLUMN_INDEX:
                return "Status";
            default:
                return null;
            }
        }

        /**
         * Liefert die Rechnung, die in einer bestimmten Zeile angezeigt wird.
         * 
         * @param rowIndex
         *            gesuchte Zeile
         * @return Rechnung in der Zeile
         */
        public BeitragRechnung getRechnungAt(int rowIndex) {
            if (rechnungen == null || rowIndex >= rechnungen.size()) {
                return null;
            }
            return rechnungen.get(rowIndex).getRechnung();
        }

        /**
         * Liefert die Anzahl erstellter Mahnungen für eine Rechnung.
         * 
         * @param rowIndex
         *            Zeile, in der die Rechnung angezeigt wird
         * @return Anzahl bisher erstellter Mahnungen für die Rechnung in der
         *         vorgegebenen Zeile
         */
        public int getMahnungenAt(int rowIndex) {
            if (rechnungen == null || rowIndex >= rechnungen.size()) {
                return -1;
            }
            return rechnungen.get(rowIndex).getMahnungen();
        }

        /**
         * Liefert den Nachnamen des Empfängers, die in einer bestimmten Zeile
         * angezeigt wird.
         * 
         * @param rowIndex
         *            gesuchte Zeile
         * @return Nachname in der Zeile
         */
        public String getNachnameAt(int rowIndex) {
            if (rechnungen == null || rowIndex >= rechnungen.size()) {
                return null;
            }
            return rechnungen.get(rowIndex).getNachname();
        }

        /**
         * Liefert den Vornamen des Empfängers, die in einer bestimmten Zeile
         * angezeigt wird.
         * 
         * @param rowIndex
         *            gesuchte Zeile
         * @return Vorname in der Zeile
         */
        public String getVornameAt(int rowIndex) {
            if (rechnungen == null || rowIndex >= rechnungen.size()) {
                return null;
            }
            return rechnungen.get(rowIndex).getVorname();
        }
    }

    /**
     * Model, das die Posten anzeigt, die zu einer bestimmten Rechnung gehören.
     */
    private class PostenModel extends AbstractTableModel {
        private static final long serialVersionUID = 5742831859991724534L;

        private static final int ID_COLUMN_INDEX = 0;
        private static final int HALBJAHR_COLUMN_INDEX = 1;
        private static final int DATUM_COLUMN_INDEX = 2;
        private static final int BUCHUNGSTEXT_COLUMN_INDEX = 3;
        private static final int BETRAG_COLUMN_INDEX = 4;

        // Angezeigte Posten
        private ArrayList<DataListPosten> postenList = null;

        /**
         * Löscht alle Posten und lädt keine neuen.
         */
        public void emptyModel() {
            postenList = null;
            fireTableDataChanged();
        }

        /**
         * Ersetzt die angezeigten Posten.
         * 
         * @param rechnungId
         *            ID der Rechnung, deren Posten angezeigt werden sollen
         */
        public void loadPosten(int rechnungId) {
            SqlSession session = sqlSessionFactory.openSession();
            try {
                RechnungenMapper mapper = session
                        .getMapper(RechnungenMapper.class);
                postenList = mapper.getPosten(rechnungId);
                fireTableDataChanged();
            } finally {
                session.close();
            }
        }

        @Override
        public int getRowCount() {
            if (postenList != null) {
                return postenList.size();
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
            if (postenList == null || rowIndex >= postenList.size()) {
                return null;
            }

            DataListPosten posten = postenList.get(rowIndex);
            BeitragBuchung buchung = posten.getBuchung();
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
            switch (columnIndex) {
            case ID_COLUMN_INDEX:
                return buchung.getBuchungId();
            case HALBJAHR_COLUMN_INDEX:
                return buchung.getHalbjahr();
            case DATUM_COLUMN_INDEX:
                return formatter.format(buchung.getDatum());
            case BUCHUNGSTEXT_COLUMN_INDEX:
                return posten.getBuchungstext();
            case BETRAG_COLUMN_INDEX:
                return buchung.getBetrag().negate();
            default:
                return null;
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case ID_COLUMN_INDEX:
                return "ID";
            case HALBJAHR_COLUMN_INDEX:
                return "Halbjahr";
            case DATUM_COLUMN_INDEX:
                return "Datum";
            case BUCHUNGSTEXT_COLUMN_INDEX:
                return "Text";
            case BETRAG_COLUMN_INDEX:
                return "Betrag";
            default:
                return null;
            }
        }
    }

    /**
     * Model, das die Mahnungen anzeigt, die zu einer bestimmten Rechnung
     * gehören.
     */
    private class MahnungenModel extends AbstractTableModel {
        private static final long serialVersionUID = -8477322592006607529L;

        private static final int ID_COLUMN_INDEX = 0;
        private static final int DATUM_COLUMN_INDEX = 1;
        private static final int FRIST_COLUMN_INDEX = 2;
        private static final int ART_COLUMN_INDEX = 3;

        // Angezeigte Mahnungen
        private ArrayList<BeitragMahnung> mahnungen = null;

        /**
         * Löscht alle Mahnungen und lädt keine neuen.
         */
        public void emptyModel() {
            mahnungen = null;
            fireTableDataChanged();
        }

        /**
         * Ersetzt die angezeigten Mahnungen.
         * 
         * @param rechnungId
         *            ID der Rechnung, deren Mahnungen angezeigt werden sollen
         */
        public void loadMahnungen(int rechnungId) {
            SqlSession session = sqlSessionFactory.openSession();
            try {
                RechnungenMapper mapper = session
                        .getMapper(RechnungenMapper.class);
                mahnungen = mapper.getMahnungen(rechnungId);
                fireTableDataChanged();
            } finally {
                session.close();
            }
        }

        @Override
        public int getRowCount() {
            if (mahnungen != null) {
                return mahnungen.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (mahnungen == null || rowIndex >= mahnungen.size()) {
                return null;
            }

            BeitragMahnung mahnung = mahnungen.get(rowIndex);
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");
            switch (columnIndex) {
            case ID_COLUMN_INDEX:
                return mahnung.getMahnungId();
            case DATUM_COLUMN_INDEX:
                return formatter.format(mahnung.getDatum());
            case FRIST_COLUMN_INDEX:
                return formatter.format(mahnung.getFrist());
            case ART_COLUMN_INDEX:
                return mahnung.getMahnungArt();
            default:
                return null;
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case ID_COLUMN_INDEX:
                return "ID";
            case DATUM_COLUMN_INDEX:
                return "Datum";
            case FRIST_COLUMN_INDEX:
                return "Frist";
            case ART_COLUMN_INDEX:
                return "Art";
            default:
                return null;
            }
        }
    }

    /**
     * Model, das die Posten einer Rechnung nach Halbjahren gruppiert und
     * jeweils die Summe der Beträge berechnet.
     */
    private class HalbjahrBetraegeModel extends AbstractTableModel {
        private static final long serialVersionUID = 4192686192102438011L;

        private static final int HALBJAHR_COLUMN_INDEX = 0;
        private static final int BETRAG_COLUMN_INDEX = 1;

        // angezeigte Paare aus Halbjahren und Beträgen
        private ArrayList<DataHalbjahrBetraege> betraege;

        /**
         * Lädt die Daten für eine Rechnung.
         * 
         * @param rechnungId
         *            ID der Rechnung
         */
        public void loadBetraege(int rechnungId) {
            SqlSession session = sqlSessionFactory.openSession();
            try {
                RechnungenMapper mapper = session
                        .getMapper(RechnungenMapper.class);
                betraege = mapper.getHalbjahrBetraege(rechnungId);
                fireTableDataChanged();
            } finally {
                session.close();
            }
        }

        @Override
        public int getRowCount() {
            if (betraege != null) {
                return betraege.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (betraege == null || rowIndex >= betraege.size()) {
                return null;
            }

            DataHalbjahrBetraege betrag = betraege.get(rowIndex);
            switch (columnIndex) {
            case HALBJAHR_COLUMN_INDEX:
                return betrag.getHalbjahr();
            case BETRAG_COLUMN_INDEX:
                return betrag.getBetrag().negate();
            default:
                return null;
            }
        }

        public String getColumnName(int column) {
            switch (column) {
            case HALBJAHR_COLUMN_INDEX:
                return "Halbjahr";
            case BETRAG_COLUMN_INDEX:
                return "Betrag";
            default:
                return null;
            }
        }
    }
}
