package nami.beitrag.db;

import lombok.Getter;

/**
 * Ergebnis-Datentyp, der Kombinationen aus einer Lastschrift und dem
 * zugehörigen Mandat aufnimmt.
 */
@Getter
public class DataLastschriftMandat {
    private BeitragLastschrift lastschrift;
    private BeitragSepaMandat mandat;
}
