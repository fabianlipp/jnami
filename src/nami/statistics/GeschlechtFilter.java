package nami.statistics;

import nami.connector.Geschlecht;
import nami.connector.namitypes.NamiAbstractMitglied;

/**
 * Filtert Mitglieder nach ihrem Geschlecht.
 * 
 * @author Fabian Lipp
 * 
 */
public class GeschlechtFilter implements Filter {

    private Geschlecht geschlecht;

    /**
     * Erzeugt einen neuen Filter.
     * @param geschlecht das Geschlecht, das akzeptiert wird
     */
    public GeschlechtFilter(Geschlecht geschlecht) {
        this.geschlecht = geschlecht;
    }

    @Override
    public boolean filter(NamiAbstractMitglied mgl) {
        return mgl.getGeschlecht() == geschlecht;
    }

}
