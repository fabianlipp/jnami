package nami.statistics;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;

import nami.connector.Geschlecht;
import nami.connector.Mitgliedstyp;
import nami.connector.NamiApiException;
import nami.connector.NamiConnector;
import nami.connector.namitypes.NamiMitgliedListElement;
import nami.connector.namitypes.NamiSearchedValues;

public class NamiStatisticsGruppe {
    private int id;
    private String bezeichnung;
    private Collection<NamiSearchedValues> searches;
    public static final Collection<NamiStatisticsGruppe> GRUPPEN = new LinkedList<>();

    static {
        NamiSearchedValues search;
        Collection<NamiSearchedValues> searches;

        search = new NamiSearchedValues();
        search.setTaetigkeitId(1); // Mitglied
        search.setUntergliederungId(1); // Wölfling
        GRUPPEN.add(new NamiStatisticsGruppe(0, "Wölflinge", search));

        search = new NamiSearchedValues();
        search.setTaetigkeitId(1); // Mitglied
        search.setUntergliederungId(2); // Jungpfadfinder
        GRUPPEN.add(new NamiStatisticsGruppe(1, "Jungpfadfinder", search));

        search = new NamiSearchedValues();
        search.setTaetigkeitId(1); // Mitglied
        search.setUntergliederungId(3); // Pfadfinder
        GRUPPEN.add(new NamiStatisticsGruppe(2, "Pfadfinder", search));

        search = new NamiSearchedValues();
        search.setTaetigkeitId(1); // Mitglied
        search.setUntergliederungId(4); // Rover
        GRUPPEN.add(new NamiStatisticsGruppe(3, "Rover", search));

        searches = new LinkedList<>();
        search = new NamiSearchedValues();
        search.setTaetigkeitId(6); // Leiter
        searches.add(search);
        search = new NamiSearchedValues();
        search.setTaetigkeitId(13); // Vorsitzender
        searches.add(search);
        search = new NamiSearchedValues();
        search.setTaetigkeitId(11); // Kurat
        searches.add(search);
        GRUPPEN.add(new NamiStatisticsGruppe(4, "Leiter", searches));

        searches = new LinkedList<>();
        search = new NamiSearchedValues();
        search.setTaetigkeitId(41); // Mitarbeiter (ohne Versicherung)
        searches.add(search);
        search = new NamiSearchedValues();
        search.setTaetigkeitId(14); // Admin
        searches.add(search);
        search = new NamiSearchedValues();
        search.setTaetigkeitId(20); // Kassierer
        searches.add(search);
        search = new NamiSearchedValues();
        search.setTaetigkeitId(23); // Materialwart
        searches.add(search);
        search = new NamiSearchedValues();
        search.setTaetigkeitId(40); // Mitarbeiter (mit Versicherung)
        searches.add(search);
        GRUPPEN.add(new NamiStatisticsGruppe(5, "Mitarbeiter", searches));

        // Filtere nach Personen, die Mitglieder sind und die passende
        // Stammgruppierung haben
        search = new NamiSearchedValues();
        NamiStatisticsGruppe stats = new NamiStatisticsGruppe(6,
                "beitragszahlende Mitglieder", search) {
            @Override
            public boolean filter(NamiMitgliedListElement mgl, int gruppierungId) {
                if (!super.filter(mgl, gruppierungId)) {
                    return false;
                }
                if (mgl.getMitgliedstyp() != Mitgliedstyp.MITGLIED) {
                    return false;
                }
                if (mgl.getGruppierungId() != gruppierungId) {
                    return false;
                }

                return true;
            }
        };
        GRUPPEN.add(stats);
    }

    public NamiStatisticsGruppe(int id, String bezeichnung,
            Collection<NamiSearchedValues> searches) {
        this.id = id;
        this.bezeichnung = bezeichnung;
        this.searches = searches;
    }

    public NamiStatisticsGruppe(int id, String bezeichnung,
            NamiSearchedValues search) {
        this.id = id;
        this.bezeichnung = bezeichnung;
        this.searches = new LinkedList<>();
        this.searches.add(search);
    }

    public int getId() {
        return id;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public boolean filter(NamiMitgliedListElement mgl, int gruppierungId) {
        return true;
    }

    public int[] getAnzahl(NamiConnector con, int gruppierungId)
            throws ClientProtocolException, NamiApiException,
            URISyntaxException, IOException {
        // Das HashSet vergleicht die Mitglieder anhand der Mitgliedsnummer,
        // d.h. jedes Mitglied wird auch bei mehreren Tätigkeiten nur einmal
        // gezählt
        Set<NamiMitgliedListElement> searchResults = new HashSet<>();

        // Führe Suchen aus
        for (NamiSearchedValues search : searches) {
            search.setGruppierungId(gruppierungId);
            searchResults.addAll(search.getAllResults(con));
        }

        // Filtere Ergebnis
        int[] count = new int[2];
        count[0] = 0;
        count[1] = 0;
        for (NamiMitgliedListElement mgl : searchResults) {
            if (filter(mgl, gruppierungId)) {
                switch (mgl.getGeschlecht()) {
                case MAENNLICH:
                    count[0]++;
                    break;
                case WEIBLICH:
                    count[1]++;
                    break;
                }
            }
        }

        return count;
    }
}
