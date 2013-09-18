package nami.beitrag.gui;

import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractSpinnerModel;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.JSpinner;
import javax.swing.text.DefaultFormatterFactory;

import nami.connector.Halbjahr;

/**
 * Stellt eine Swing-Komponente bereit, mit der ein Halbjahr (inkl. Jahr)
 * ausgewählt werden kann.
 * 
 * @author Fabian Lipp
 * 
 */
public class HalbjahrComponent extends JSpinner {
    private static final long serialVersionUID = -1067992548702882662L;

    /**
     * Erzeugt eine neue Komponente, die mit dem aktuellen Halbjahr
     * initialisiert wird.
     */
    public HalbjahrComponent() {
        this(new Halbjahr(new Date()));
    }

    /**
     * Erzeugt eine neue Komponente, die mit einem vorgegebenen Halbjahr
     * initialisiert ist.
     * 
     * @param halbjahr
     *            Voreinstellung für das Halbjahr
     */
    public HalbjahrComponent(Halbjahr halbjahr) {
        setModel(new HalbjahrModel(halbjahr));
        setEditor(new HalbjahrEditor(this));

        this.addMouseWheelListener(new MouseWheelSpinnerScroll());
    }

    /**
     * Listener, der beim Benutzen des Mausrads das Drücken der DOWN/UP-Taste
     * auf der Tastatur simuliert.
     */
    private class MouseWheelSpinnerScroll implements MouseWheelListener {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getWheelRotation() > 0) {
                // nach unten
                dispatchEvent(new KeyEvent(HalbjahrComponent.this,
                        KeyEvent.KEY_PRESSED, new Date().getTime(), 0,
                        KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED));
            } else if (e.getWheelRotation() < 0) {
                // nach oben
                dispatchEvent(new KeyEvent(HalbjahrComponent.this,
                        KeyEvent.KEY_PRESSED, new Date().getTime(), 0,
                        KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED));
            }

        }
    }

    /**
     * Wandelt ein Halbjahr in einen String um und umgekehrt.
     */
    private static class HalbjahrFormatter extends AbstractFormatter {
        private static final long serialVersionUID = 5476498591061768873L;

        /*
         * Pattern wählt Folge von Ziffern (als Group 1). Davor dürfen
         * Nicht-Ziffern vorkommen. Nach der letzten Ziffer dürfen keine
         * weiteren Zeichen folgen
         */
        private static Pattern jahrPattern = Pattern.compile("[\\D]*([\\d]+)$");

        @Override
        public Object stringToValue(String text) throws ParseException {
            text = text.trim();

            // erster Teil (Halbjahr)
            char firstChar = text.charAt(0);
            int halbjahr;
            switch (firstChar) {
            case '1':
                halbjahr = 1;
                break;
            case '2':
                halbjahr = 2;
                break;
            default:
                throw new ParseException("invalid value for halbjahr", 0);
            }

            // zweiter Teil (Jahr)
            Matcher matcher = jahrPattern.matcher(text);
            // start at char index 1 because first character is halbjahr
            if (!matcher.find(1)) {
                throw new ParseException("cannot find value for jahr", 1);
            }
            int jahr = 0;
            try {
                jahr = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                throw new ParseException("cannot parse year", matcher.start(1));
            }

            return new Halbjahr(halbjahr, jahr);
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            if (value == null) {
                return "";
            }
            if (!(value instanceof Halbjahr)) {
                return value.toString();
            }
            Halbjahr halbjahr = (Halbjahr) value;
            return halbjahr.getHalbjahr() + "/" + halbjahr.getJahr();
        }

    }

    /**
     * Editor, der verwendet wird, um ein HalbjahrModel mit einem JSpinner zu
     * bearbeiten.
     */
    private class HalbjahrEditor extends DefaultEditor {
        private static final long serialVersionUID = 7079551363298819786L;

        public HalbjahrEditor(JSpinner spinner) {
            super(spinner);

            if (!(spinner.getModel() instanceof HalbjahrModel)) {
                throw new IllegalArgumentException("model not a HalbjahrModel");
            }

            AbstractFormatter formatter = new HalbjahrFormatter();
            JFormattedTextField ftf = getTextField();
            ftf.setEditable(true);
            ftf.setFormatterFactory(new DefaultFormatterFactory(formatter));
            ftf.setColumns(6);
        }

    }

    /**
     * Model für einen Spinner, das ein Halbjahr (inkl. Jahr) beschreibt.
     */
    private class HalbjahrModel extends AbstractSpinnerModel {
        private static final long serialVersionUID = 369551205081430944L;
        private Halbjahr value;

        public HalbjahrModel(Halbjahr defaultValue) {
            if (defaultValue == null) {
                throw new NullPointerException("defaultValue is null");
            }
            this.value = defaultValue;
        }

        @Override
        public Halbjahr getValue() {
            return value;
        }

        @Override
        public void setValue(Object value) {
            if ((value == null) || !(value instanceof Halbjahr)) {
                throw new IllegalArgumentException("illegal value");
            }
            if (!value.equals(this.value)) {
                this.value = (Halbjahr) value;
                fireStateChanged();
            }
        }

        @Override
        public Object getNextValue() {
            int halbjahr = value.getHalbjahr();
            int jahr = value.getJahr();
            if (halbjahr == 1) {
                return new Halbjahr(2, jahr);
            } else {
                return new Halbjahr(1, jahr + 1);
            }
        }

        @Override
        public Object getPreviousValue() {
            int halbjahr = value.getHalbjahr();
            int jahr = value.getJahr();
            if (halbjahr == 2) {
                return new Halbjahr(1, jahr);
            } else {
                return new Halbjahr(2, jahr - 1);
            }
        }
    }

    /**
     * Liefert das ausgewählte Halbjahr.
     * 
     * @return Halbjahr
     */
    public Halbjahr getValue() {
        // am Anfang noch falsches Model -> ignorieren (sonst
        // ClassCastException)
        if (!(getModel() instanceof HalbjahrModel)) {
            return null;
        }
        return ((HalbjahrModel) getModel()).getValue();
    }
}
