package nami.beitrag.gui;

import java.awt.Color;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import nami.beitrag.db.BeitragMapper;
import nami.beitrag.db.BeitragMitglied;
import net.miginfocom.swing.MigLayout;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * Stellt ein Panel bereit, mit dem ein Mitglied ausgewählt werden kann.
 * 
 * @author Fabian Lipp
 * 
 */
public class MitgliedSelectPanel extends JPanel {
    private static final long serialVersionUID = 969876938697755251L;

    private JTextField txtMitgliedid;
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
     * Ruft den Suchdialog beim Klick auf den Button auf und trägt die
     * zurückgegebene Mitglieds-ID in das Textfeld ein.
     */
    private class MitgliedSearchListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Window win = SwingUtilities
                    .getWindowAncestor(MitgliedSelectPanel.this);
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
    public MitgliedSelectPanel(SqlSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        createPanel();
    }

    private void createPanel() {
        setLayout(new MigLayout("", "[grow]", "[shrink]"));

        txtMitgliedid = new JTextField();
        txtMitgliedid.setText("");
        txtMitgliedid.setColumns(10);
        txtMitgliedid.addFocusListener(new MitgliedIdFocusListener());
        add(txtMitgliedid, "flowx,alignx left");

        JButton btnSearchbutton = new JButton("...");
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
                validatedMitgliedId = mitgliedId;
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
            validatedMitgliedId = -1;
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
}
