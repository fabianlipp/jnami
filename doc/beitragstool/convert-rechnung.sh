#!/bin/bash

# Wandelt eine aus NaMi heruntergeladene PDF-Rechnung so in eine CSV-Datei um,
# dass sie vom Beitragstool eingelesen werden kann.
# Benötigte Tools:
# - POSIX-Tools (sed, grep, tail)
# - pdftotext

if [ $# -lt 1 ]; then
	echo Need filename of Rechnungs-PDF as parameter
fi

INFILE="$1"
TXTFILE="$(basename $INFILE .pdf).txt"
CSVFILE="$(basename $INFILE .pdf).csv"

# PDF in TXT umwandeln
pdftotext -layout "$INFILE" "$TXTFILE"

# Zeilen pro Mitglied auslesen (erste Zeile entfernen, da PLZ aus Adresse erkannt)
DATUMREGEXP='[[:digit:]][[:digit:]]\.[[:digit:]][[:digit:]]\.[[:digit:]][[:digit:]]'
BETRAGREGEXP='-\?[[:digit:]]\?[[:digit:]][,.][[:digit:]][[:digit:]]'
grep "^[[:digit:]]\{4,6\}" "$TXTFILE" \
	| tail -n +2 \
	| sed -e 's/\([[:space:]][[:digit:]]\?[[:digit:]]\)\.\([[:digit:]][[:digit:]][[:space:]]\)/\1,\2/' \
	| sed -e 's/\(^[[:digit:]]\{4,6\}\).*\(\(SB\|VB\|FB\|SBS\|VBS\|FBS\) . Beitrag von \('"$DATUMREGEXP"'\) bis '"$DATUMREGEXP"'\)[[:space:]]*\('"$BETRAGREGEXP"'\)[[:space:]]*\([[:digit:]]*\)$/\1;\2;\3;\4;\5;\6/' > "$CSVFILE"



