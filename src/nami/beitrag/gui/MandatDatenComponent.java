package nami.beitrag.gui;

import com.toedter.calendar.JDateChooser;
import nami.beitrag.db.BeitragMitglied;
import nami.beitrag.db.BeitragSepaMandat;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.util.Date;

public class MandatDatenComponent extends JComponent {
    private JTextField txtKontoinhaber;
    private JTextField txtStrasse;
    private JTextField txtPlz;
    private JTextField txtOrt;
    private JTextField txtEmail;
    private JTextField txtIban;
    private JTextField txtBic;
    private JDateChooser dateChooser;

    public MandatDatenComponent() {
        createPanel(true);
    }

    public MandatDatenComponent(BeitragSepaMandat mandat) {
        createPanel(false);
        txtKontoinhaber.setText(mandat.getKontoinhaber());
        txtStrasse.setText(mandat.getStrasse());
        txtPlz.setText(mandat.getPlz());
        txtOrt.setText(mandat.getOrt());
        txtEmail.setText(mandat.getEmail());
        txtIban.setText(mandat.getIban());
        txtBic.setText(mandat.getBic());
        dateChooser.setDate(mandat.getDatum());
    }

    private void createPanel(boolean editable) {
        setLayout(new MigLayout("", "[][grow]", "[][][][][][][][]"));

        JLabel lblKontoinhaber = new JLabel("Kontoinhaber:");
        add(lblKontoinhaber, "cell 0 0");
        txtKontoinhaber = new JTextField();
        txtKontoinhaber.setEditable(editable);
        txtKontoinhaber.setColumns(50);
        add(txtKontoinhaber, "cell 1 0");

        JLabel lblStrasse = new JLabel("Straße:");
        add(lblStrasse, "cell 0 1");
        txtStrasse = new JTextField();
        txtStrasse.setColumns(50);
        add(txtStrasse, "cell 1 1");

        JLabel lblPlz = new JLabel("PLZ:");
        add(lblPlz, "cell 0 2");
        txtPlz = new JTextField();
        txtPlz.setColumns(5);
        add(txtPlz, "cell 1 2");

        JLabel lblOrt = new JLabel("Ort:");
        add(lblOrt, "cell 0 3");
        txtOrt = new JTextField();
        txtOrt.setColumns(50);
        add(txtOrt, "cell 1 3");

        JLabel lblEmail = new JLabel("E-Mail:");
        add(lblEmail, "cell 0 4");
        txtEmail = new JTextField();
        txtEmail.setColumns(50);
        add(txtEmail, "cell 1 4");

        JLabel lblIban = new JLabel("IBAN:");
        add(lblIban, "cell 0 5");
        txtIban = new JTextField();
        txtIban.setEditable(editable);
        txtIban.setColumns(34);
        add(txtIban, "cell 1 5");

        JLabel lblBic = new JLabel("BIC:");
        add(lblBic, "cell 0 6");
        txtBic = new JTextField();
        txtBic.setEditable(editable);
        txtBic.setColumns(11);
        add(txtBic, "cell 1 6");

        JLabel lblDatum = new JLabel("Datum des Mandats:");
        add(lblDatum, "cell 0 7");
        dateChooser = new JDateChooser(new Date());
        dateChooser.setEnabled(editable);
        add(dateChooser, "cell 1 7");
    }

    public void writeInputsToMandat(BeitragSepaMandat mandat)
    {
        mandat.setIban(txtIban.getText());
        mandat.setBic(txtBic.getText());
        mandat.setDatum(dateChooser.getDate());
        mandat.setKontoinhaber(txtKontoinhaber.getText());
        mandat.setStrasse(txtStrasse.getText());
        mandat.setPlz(txtPlz.getText());
        mandat.setOrt(txtOrt.getText());
        mandat.setEmail(txtEmail.getText());
    }

    public void fillFromMitglied(BeitragMitglied mgl) {
        if (mgl != null) {
            txtKontoinhaber.setText(mgl.getVorname() + " "
                    + mgl.getNachname());
            txtStrasse.setText(mgl.getStrasse());
            txtPlz.setText(mgl.getPlz());
            txtOrt.setText(mgl.getOrt());
            if (!mgl.getEmail().isBlank()) {
                txtEmail.setText(mgl.getEmail());
            } else {
                txtEmail.setText(mgl.getEmailVertretungsber());
            }
        }
    }
}
