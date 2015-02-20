package nami.connector.credentials;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;

/**
 * Stellt einen Frame dar, der die NaMi-Zugangsdaten abfragt.
 * 
 * @author Fabian Lipp
 * 
 */
public class CredentialsFrame extends JFrame {
    private static final long serialVersionUID = 2645528642747807801L;
    private JTextField txtUsername;
    private JPasswordField txtPassword;

    private boolean completed = false;
    private String username = null;
    private char[] password = null;

    /**
     * Zeigt einen Frame an, der zur Eingabe von Benutzername und Passwort
     * auffordert.
     * 
     * @param username
     *            Voreingestellter Benutzername; wenn <tt>null</tt> übergeben
     *            wird, ist das Feld leer. Wenn ein Benutzername übergeben wird,
     *            dann hat beim Öffnen des Formulars das Passwort-Feld den
     *            Fokus.
     */
    public CredentialsFrame(String username) {
        super("NaMi-Zugangsdaten eingeben");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new MigLayout("", "[][grow]", "[][][]"));

        JLabel lblUsername = new JLabel("Benutzername:");
        getContentPane().add(lblUsername, "cell 0 0,alignx trailing");

        txtUsername = new JTextField();
        getContentPane().add(txtUsername, "cell 1 0,growx");
        txtUsername.setColumns(10);

        JLabel lblPassword = new JLabel("Passwort:");
        getContentPane().add(lblPassword, "cell 0 1,alignx trailing");

        txtPassword = new JPasswordField();
        getContentPane().add(txtPassword, "cell 1 1,growx");
        txtPassword.setColumns(10);

        JButton btnAbort = new JButton("Abbrechen");
        getContentPane().add(btnAbort, "flowx,cell 1 2,alignx trailing");
        btnAbort.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                completed = false;
                dispose();
            }
        });

        JButton btnOk = new JButton("OK");
        getContentPane().add(btnOk, "cell 1 2,alignx trailing");
        btnOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                completed = true;
                CredentialsFrame.this.username = txtUsername.getText();
                CredentialsFrame.this.password = txtPassword.getPassword();
                dispose();
            }
        });

        JRootPane rootPane = getRootPane();
        rootPane.setDefaultButton(btnOk);
        // Handle ESC-key
        Action escListener = new AbstractAction() {
            private static final long serialVersionUID = 8011175609689348329L;

            @Override
            public void actionPerformed(ActionEvent e) {
                dispatchEvent(new WindowEvent(CredentialsFrame.this,
                        WindowEvent.WINDOW_CLOSING));
            }
        };
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "jnami.ESC");
        rootPane.getActionMap().put("jnami.ESC", escListener);

        pack();

        if (username != null) {
            txtUsername.setText(username);
            txtPassword.requestFocusInWindow();
        }
    }

    /**
     * Gibt an, ob das Fenster mit OK/Eingabetaste geschlossen wurde.
     * 
     * @return <tt>true</tt>, falls die Eingaben bestätigt wurden
     */
    public boolean isComplete() {
        return completed;
    }

    /**
     * Liefert den eingegebenen Benutzernamen.
     * 
     * @return Username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Liefert das eingegebene Passwort.
     * 
     * @return Passwort
     */
    public char[] getPassword() {
        return password;
    }
}
