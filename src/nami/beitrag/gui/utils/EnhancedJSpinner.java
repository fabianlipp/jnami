package nami.beitrag.gui.utils;

import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Date;

import javax.swing.JSpinner;
import javax.swing.SpinnerModel;

/**
 * Erweiterte JSpinner-Variante. Die momentan einzige Änderung ist, dass der
 * Spinner auf das Mausrad reagiert.
 * 
 * @author Fabian Lipp
 * 
 */
public class EnhancedJSpinner extends JSpinner {
    private static final long serialVersionUID = 2543227949570542782L;

    // Documentation from superclass
    /**
     * Constructs a spinner with an <code>Integer SpinnerNumberModel</code> with
     * initial value 0 and no minimum or maximum limits.
     */
    public EnhancedJSpinner() {
        super();
        this.addMouseWheelListener(new MouseWheelSpinnerScroll());
    }

    // Documentation from superclass
    /**
     * Constructs a spinner for the given model. The spinner has a set of
     * previous/next buttons, and an editor appropriate for the model.
     * 
     * @param model .
     */
    public EnhancedJSpinner(SpinnerModel model) {
        super(model);
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
                dispatchEvent(new KeyEvent(EnhancedJSpinner.this,
                        KeyEvent.KEY_PRESSED, new Date().getTime(), 0,
                        KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED));
            } else if (e.getWheelRotation() < 0) {
                // nach oben
                dispatchEvent(new KeyEvent(EnhancedJSpinner.this,
                        KeyEvent.KEY_PRESSED, new Date().getTime(), 0,
                        KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED));
            }

        }
    }

}
