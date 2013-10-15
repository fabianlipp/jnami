package nami.beitrag.gui;

import java.awt.Color;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.BeitragMitglied;
import net.miginfocom.swing.MigLayout;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * Stellt ein Element bereit, mit dem ein Mitglied ausgewählt werden kann.
 * 
 * @author Fabian Lipp
 * 
 */
public class MitgliedSelectComponent extends JComponent {
    private static final long serialVersionUID = 969876938697755251L;

    private JTextField txtMitgliedid;
    private JButton btnSearchbutton;
    private JLabel lblMitgliedsnummer;
    private JLabel lblVorname;
    private JLabel lblNachname;

    private SqlSessionFactory sessionFactory;
    private int validatedMitgliedId = -1;

    /**
     * Überprüft die eingegebene ID beim Verlassen des Textfeldes und füllt die
     * Labels entsprechend.
     */
    private class MitgliedIdFocusListener extends FocusAdapter {
        @Override
        public void focusLost(FocusEvent e) {
            if (e.getComponent() != txtMitgliedid) {
                throw new IllegalArgumentException(
                        "Focus listener on wrong object");
            }
            searchMitgliedId();
        }
    }

    /**
     * Überprüft die eingegebene ID beim Drücken der Eingabetaste im Textfeld
     * und füllt die Labels entsprechend.
     */
    private class MitgliedIdEnterListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            searchMitgliedId();
        }
    }

    /**
     * Ruft den Suchdialog beim Klick auf den Button auf und trägt die
     * zurückgegebene Mitglieds-ID in das Textfeld ein.
     */
    private class MitgliedSearchListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Window win = SwingUtilities
                    .getWindowAncestor(MitgliedSelectComponent.this);
            MitgliedSelectDialog selectDiag = new MitgliedSelectDialog(win,
                    sessionFactory);
            selectDiag.setVisible(true);
            int mglId = selectDiag.getChosenMglId();
            if (mglId != -1) {
                txtMitgliedid.setText(Integer.toString(mglId));
                searchMitgliedId();
            }
        }
    }

    /**
     * Erzeugt das Eingabefeld.
     * 
     * @param sessionFactory
     *            Verbindung zur Datenbank
     */
    public MitgliedSelectComponent(SqlSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        createPanel();
    }

    private void createPanel() {
        setLayout(new MigLayout("insets 0", "[grow][][][][]", "[shrink]"));

        txtMitgliedid = new JTextField();
        txtMitgliedid.setText("");
        txtMitgliedid.setColumns(10);
        txtMitgliedid.addFocusListener(new MitgliedIdFocusListener());
        txtMitgliedid.addActionListener(new MitgliedIdEnterListener());
        add(txtMitgliedid, "flowx,alignx left");

        btnSearchbutton = new JButton("...");
        btnSearchbutton.setMargin(new Insets(0, 1, 0, 1));
        btnSearchbutton.addActionListener(new MitgliedSearchListener());
        add(btnSearchbutton);

        lblMitgliedsnummer = new JLabel("");
        add(lblMitgliedsnummer);

        lblVorname = new JLabel("");
        add(lblVorname);

        lblNachname = new JLabel("");
        add(lblNachname);
    }

    /**
     * Sucht in der Datenbank nach der Mitglied-ID und füllt die Labels
     * entsprechend.
     */
    private void searchMitgliedId() {
        if (sessionFactory == null) {
            throw new IllegalArgumentException(
                    "Used MitgliedSelectPanel without a SqlSessionFactory");
        }

        int mitgliedId;
        String mitgliedIdStr = txtMitgliedid.getText().trim();
        if (mitgliedIdStr.isEmpty()) {
            lblMitgliedsnummer.setText("");
            lblVorname.setText("");
            lblNachname.setText("");
            if (validatedMitgliedId != -1) {
                validatedMitgliedId = -1;
                fireStateChanged();
            }
            markError(false);
            return;
        }
        try {
            mitgliedId = Integer.parseInt(mitgliedIdStr);
        } catch (NumberFormatException e) {
            markError(true);
            return;
        }

        SqlSession session = sessionFactory.openSession();
        try {
            BeitragMapper mapper = session.getMapper(BeitragMapper.class);

            BeitragMitglied mgl = mapper.getMitglied(mitgliedId);

            if (mgl != null) {
                lblMitgliedsnummer.setText(Integer.toString(mgl
                        .getMitgliedsnummer()));
                lblVorname.setText(mgl.getVorname());
                lblNachname.setText(mgl.getNachname());
                if (validatedMitgliedId != mitgliedId) {
                    validatedMitgliedId = mitgliedId;
                    fireStateChanged();
                }
                markError(false);
            } else {
                markError(true);
            }
        } finally {
            session.close();
        }
    }

    /**
     * (De-)Markiert das Eingabefeld als Fehler.
     * 
     * @param error
     *            falls <tt>true</tt> wird das Feld als Fehler markiert;
     *            andernfalls wird diese Markierung rückgängig gemacht
     */
    private void markError(boolean error) {
        if (error) {
            lblMitgliedsnummer.setText("");
            lblVorname.setText("");
            lblNachname.setText("");
            txtMitgliedid.setBackground(Color.RED);
            if (validatedMitgliedId != -1) {
                validatedMitgliedId = -1;
                fireStateChanged();
            }
        } else {
            txtMitgliedid.setBackground(UIManager
                    .getColor("TextField.background"));
        }
    }

    /**
     * Liefert die MitgliedID, die momentan in diesem Panel eingetragen ist. Die
     * ID wird vorher auf Gültigkeit überprüft, d.h. es wird getestet, ob das
     * Mitglied in der Datenbank vorhanden ist.
     * 
     * @return die MitgliedID; <tt>-1</tt>, falls keine gültige ID eingetragen
     *         ist
     */
    public int getMitgliedId() {
        return validatedMitgliedId;
    }

    /**
     * Setzt die MitgliedID, die in diesem Panel eingetragen ist. Wenn es sich
     * um eine gültige ID handelt, werden die Labels mit dem Namen des Mitglieds
     * gefüllt.
     * 
     * @param mitgliedId
     *            die MitgliedID; wenn hier <tt>-1</tt> übergeben wird, wird der
     *            Inhalt des Textfeldes gelöscht
     */
    public void setMitgliedId(int mitgliedId) {
        if (mitgliedId == -1) {
            txtMitgliedid.setText("");
            searchMitgliedId();
        } else {
            txtMitgliedid.setText(Integer.toString(mitgliedId));
            searchMitgliedId();
        }
    }

    /*
     * Behandlung von ChangeListener/ChangeEvent von JSlider (OpenJDK) kopiert
     * und modifiziert
     */
    /**
     * Only one <code>ChangeEvent</code> is needed per instance since the
     * event's only (read-only) state is the source property. The source of
     * events generated here is always "this". The event is lazily created the
     * first time that an event notification is fired.
     * 
     * @see #fireStateChanged
     */
    private transient ChangeEvent changeEvent = null;

    /**
     * Fügt einen <tt>ChangeListener</tt> hinzu.
     * 
     * @param l
     *            der <tt>ChangeListener</tt>, der hinzugefügt werden soll
     * @see #fireStateChanged
     * @see #removeChangeListener
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * Entfernt einen <tt>ChangeListener</tt>.
     * 
     * @param l
     *            der <tt>ChangeListener</tt>, der entfernt werden soll
     * @see #fireStateChanged
     * @see #addChangeListener
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    /**
     * Liefert alle bei diesem Objekt registrierten <tt>ChangeListener</tt>.
     * 
     * @return alle registrierten <tt>ChangeListener</tt>; ein leeres Array,
     *         falls keine registriert sind
     * @since 1.4
     */
    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /**
     * Sendet ein <tt>ChangeEvent</tt> an alle registrierten
     * <tt>ChangeListener</tt>.
     * 
     * @see #addChangeListener
     */
    protected void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    /**
     * Aktiviert oder deaktiviert die Komponente.
     * 
     * @param b
     *            <tt>true</tt> aktiviert die Komponente, <tt>false</tt>
     *            deaktiviert sie
     */
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        txtMitgliedid.setEnabled(b);
        btnSearchbutton.setEnabled(b);
    }
}
