# Kompilierung/Installation
siehe README im Hauptverzeichnis des Repositories




# Konfiguration
Die ausgewerteten Gruppen für die Statistik müssen vor der ersten Ausführung
konfiguriert werden.
Als Vorlage für diese Konfiguration kann die Datei
[doc/namistatistics.xml](namistatistics.xml) verwendet werden.
Das genaue Format für die Datei ist in
[src/nami/statistics/namistatistics.xsd](../src/nami/statistics/namistatistics.xsd)
beschrieben.

Zur Benutzung ist eine SQL-Datenbank erforderlich.
Momentan werden MySQL und HSQLDB unterstützt.
HSQLDB bietet den Vorteil, dass kein entsprechender Server installiert werden muss.
Die Konfiguration erfolgt ebenfalls in der XML-Datei und ist dort erklärt.
Die Tabellen werden in der Datenbank automatisch beim ersten Aufruf angelegt.

Standardmäßig wird die Konfigurationsdatei unter `~/.jnami/namistatistics.xml`
gesucht (kann mit einem Parameter beim Aufruf überschrieben werden).




# Benutzung
Zunächst muss der Java-Classpath entsprechend gesetzt werden.
Die nötigen JAR-Archive werden bei der Kompilierung in eine Datei geschrieben.
In der Bash kann diese beispielsweise so eingelesen werden:
```bash
export CLASSPATH=$(< namicli.classpath)
```

Bei allen Aufrufen kann der Parameter `-c` (oder `--configfile`) verwendet
werden, um einen abweichenden Pfad zur Konfigurationsdatei anzugeben.

* Verfügbare Befehle auflisten
```bash
java nami.statistics.NamiStatistics help
```

* Aktuelle Mitgliederzahlen aus NaMi herunterladen und in die lokale Datenbank
  speichern.

  Dabei wird (falls nötig) auch die Datenbank initialisiert, die verfügbaren
  Gruppierungen abgefragt usw.
```bash
java nami.statistics.NamiStatistics collectData
```

* Vorhandene Daten anzeigen: Listet Index und Datum aller ausgeführten
  Datensammlungen auf
```bash
java nami.statistics.NamiStatistics listRuns
```

* Statistik aller Gruppierungen zu einem bestimmten Zeitpunkt ausgeben.
  Die Ausgabe erfolgt im CSV-Format.

  Mit dem ersten Parameter wird der Name der Ausgabe-Datei festgelegt.
  Wird er ausgelassen oder `-` übergeben, dann erfolgt die Ausgabe auf STDOUT.
  Mit dem zweiten Parameter kann festgelegt werden, zu welchem Zeitpunkt die
  Daten ausgegeben werden sollen (verfügbare IDs können mit `listRuns`
  abgefragt werden).
  Standardmäßig wird die zuletzt ausgeführte Datensammlung verwendet.
```bash
java nami.statistics.NamiStatistics statsAsCsv [out.csv] [runId]
```

* Kummulierte Statistik aller Gruppierungen zu einem bestimmten Zeitpunkt ausgeben.
  Dieser Befehl entspricht dem vorherigen, allerdings werden zu den
  Mitgliederzahlen der Bezirke und Diözesen die Summe aller untergeordneten
  Gruppierungen addiert.
```bash
java nami.statistics.NamiStatistics statsAsCsvCum [out.csv] [runId]
```

* Statistik einer Gruppierungen im zeitlichen Verlauf ausgeben.
  Die Ausgabe erfolgt im CSV-Format.

  Mit dem ersten Parameter wird der Name der Ausgabe-Datei festgelegt.
  Wird `-` übergeben, dann erfolgt die Ausgabe auf STDOUT.
  Mit dem zweiten Parameter wird die Gruppierungsnummer festgelegt,
  deren Daten ausgegeben werden sollen.
```bash
java nami.statistics.NamiStatistics historyAsCsv out.csv 123456
```

* Kummulierte Statistik einer Gruppierungen im zeitlichen Verlauf ausgeben.
  Dieser Befehl entspricht dem vorherigen, allerdings werden zu den
  Mitgliederzahlen der Bezirke und Diözesen die Summe aller untergeordneten
  Gruppierungen addiert.
```bash
java nami.statistics.NamiStatistics historyAsCsvCum out.csv 123456
```




# Hinweise
Das Programm schickt sehr viele Such-Anfragen an den NaMi-Server (pro Paar aus
Gruppe und Gruppierung mindestens eine).
Mir ist keine andere Möglichkeit bekannt diese Daten abzufragen.

Es sind noch nicht alle Filtermöglichkeiten, die NaMi bei der Suche bietet,
verfügbar.
Die anderen können aber leicht ergänzt werden.


