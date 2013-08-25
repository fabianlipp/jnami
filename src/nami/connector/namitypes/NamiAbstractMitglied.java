package nami.connector.namitypes;

import java.io.IOException;

import nami.connector.Geschlecht;
import nami.connector.Mitgliedstyp;
import nami.connector.NamiConnector;
import nami.connector.exception.NamiApiException;

public abstract class NamiAbstractMitglied {
    public abstract int getId();

    public abstract String getVorname();

    public abstract String getNachname();

    public abstract String getEmail();

    public abstract int getMitgliedsnummer();

    public abstract Mitgliedstyp getMitgliedstyp();

    public abstract Geschlecht getGeschlecht();

    public abstract int getGruppierungId();

    public abstract String getGruppierung();

    /**
     * Liefert den vollständigen Mitgliedsdatensatz. Dazu ist evtl. noch eine
     * Anfrage an NaMi notwendig.
     * 
     * @param con
     *            Verbindung zum NaMi-Server
     * @return vollständige Stammdaten des Mitglieds
     * @throws NamiApiException
     *             API-Fehler beim Zugriff auf NaMi
     * @throws IOException
     *             IOException
     */
    public abstract NamiMitglied getFullData(NamiConnector con)
            throws NamiApiException, IOException;

    @Override
    public final String toString() {
        return String.format("%-6d %s %s [%s]", getMitgliedsnummer(),
                getVorname(), getNachname(), getGruppierung());
    }
}
