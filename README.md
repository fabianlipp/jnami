Various tools to work with NaMi (membership management of DPSG) from Java. Further documentation is German-only, because the software is utilizable for members of DPSG (http://www.dpsg.de) only.




Lizenz:
-------
Der Code unterliegt der GNU GPL. Der vollständige Lizenztext befindet sich in LICENSE.

> Copyright (C) 2013-2022 Fabian Lipp
>
> This program is free software: you can redistribute it and/or modify
> it under the terms of the GNU General Public License as published by
> the Free Software Foundation, either version 3 of the License, or
> (at your option) any later version.
>
> This program is distributed in the hope that it will be useful,
> but WITHOUT ANY WARRANTY; without even the implied warranty of
> MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
> GNU General Public License for more details.
>
> You should have received a copy of the GNU General Public License
> along with this program. If not, see <http://www.gnu.org/licenses/>.


Nutzbare Tools:
---------------
* NamiConnector: Bibliothek, die auf NaMi zugreift und die Ergebnisse in Java-Objekte umsetzt. Deckt noch nicht den vollen Funktionsumfang von NaMi ab, ist aber problemlos erweiterbar.
* NamiStatistics: Zählt die Mitglieder (aufgeteilt nach Gruppierung und Stufenzugehörigkeit) in NaMi und speichert die Ergebnisse in eine SQL-Datenbank. Bietet Funktionen, um die Daten aus dieser Datenbank auszuwerten (um beispielsweise eine Statistik für den Stamm, den ganzen Bezirk usw. zu generieren). Insgesamt kann das Tool mindestens die Statistik-Funktion aus NaMi 1 ersetzen, die es in NaMi 2 nicht mehr gibt (ein Bug-Report diesbezüglich blieb unbeantwortet).

  Eine genauere Anleitung für dieses Tool befindet sich in
  [doc/README.namistatistics.md](doc/README.namistatistics.md).
* Beitragstool: Noch nicht wirklich gut getestet, sollte aber schon funktionieren. Funktionsumfang:
  - Mitglieder-Datenbank mit NaMi synchronisieren
  - Beitragskonten aus NaMi abholen (momentan in NaMi nicht verfügbar)
  - Rechnungen/Mahnungen erstellen (mittels LaTeX)
  - SEPA-Mandatsverwaltung
  - SEPA-Lastschriftaufträge mittel XML-RPC an Hibiscus übergeben
  - Prenotifications erzeugen (mittels LaTeX)
  - Kündigungen vormerken


Begonnene Tools:
----------------
* Nami2Mailman: Sucht Personen nach Gruppenzugehörigkeit und generiert daraus Mailverteiler (als Textdateien) die dann beispielsweise mit GNU Mailman synchronisiert werden können
Das Ganze ist mittels XML konfigurierbar und im Stamm, Bezirk und Diözesanverband schon getestet.
* NaMiCli: Ein Kommandozeilen-Interface, mit dem einige NaMi-Funktionen genutzt werden können (z. B. Personen gesucht werden). Außerdem kann auch hieraus das Statistik-Tool genutzt werden.



Bemerkungen zum aktuellen Stand:
--------------------------------
* Die Software ist mittlerweile an den Produktiv-Server von NaMi angepasst. Da
  der Test-Server nicht mehr auf dem aktuellen Software-Stand ist, dürfte die
  Zusammenarbeit damit nicht mehr richtig funktionieren.
* Ich habe das Ganze bisher nur unter Linux (OpenJDK 11) getestet.
  Ich denke es spricht aber auch nichts dagegen, dass es unter Windows läuft.
* Bei den Bibliotheken, die Ivy herunterlädt, hat sich im Laufe der Zeit
  vermutlich einiges angesammelt, was nicht mehr genutzt wird. Da könnte man
  mal aufräumen.



Kompilierung
------------
* Man benötigt dafür ant (http://ant.apache.org/) und Ivy (http://ant.apache.org/ivy/).
* Der Code benötigt mindestens Java 11.
* Dann sollte ein einfacher Aufruf "ant" im Quelltext-Verzeichnis (also dem Verzeichnis, wo die build.xml liegt) reichen, um das Ganze zu kompilieren.
* Die Datei src/nami/connector/credentials/NamiWalletCredentials.java macht vermutlich Probleme, weil man dafür noch eine Library und zugehörige Abhängigkeiten (inkl. JNI) benötigt (https://evolvis.org/projects/libwallet/). Man kann diese Quelltext-Datei aber einfach löschen, wenn man die Passwörter nicht in KWallet speichern möchte.
* jnami verwendet [Project Lombok](http://projectlombok.org/). Bei der Kompilierung auf der Kommandozeile mittels ant wird die notwendige Library automatisch heruntergeladen. Bei Verwendung einer IDE muss ggf. ein Plug-In für Lombok installiert werden.



Ausführung
----------
Die Programme lassen sich – nachdem Ant erfolgreich war – folgendermaßen starten (Bash-Syntax):
```bash
export CLASSPATH=$(< namicli.classpath)  # Diese Datei wird von Ant generiert
java nami.cli.NamiCli
```

```bash
export CLASSPATH=$(< namicli.classpath)  # Diese Datei wird von Ant generiert
java nami.statistics.NamiStatistics
```

```bash
export CLASSPATH=$(< namicli.classpath)  # Diese Datei wird von Ant generiert
java nami.beitrag.NamiBeitragGui
```

<!---
Benutzte Java-Libraries (unvollständig, muss aktualisiert werden):
------------------------------------------------------------------
- Apache HttpComponents
http://hc.apache.org/

- google-gson
http://code.google.com/p/google-gson/

- JDOM 2
http://www.jdom.org/

- MySQL Connector/J
http://dev.mysql.com/downloads/connector/j/
-->



