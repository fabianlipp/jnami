package nami.beitrag.letters;

import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Ruft Latex zum Kompilieren einer Datei im Hintergrund auf und zeigt die
 * Ausgaben des Compilers in einem eigenen Fenster an.
 * 
 * @author Fabian Lipp
 * 
 */
public class LatexRunner {
    private LetterDirectory dir;
    private JFrame logWindow;

    private JTextArea outputTextArea;

    private static Logger logger = Logger
            .getLogger(LatexRunner.class.getName());

    /**
     * Initialisiert den LatexRunner. Dazu muss das Arbeitsverzeichnis übergeben
     * werden. Dieses wird auch beim LaTeX-Aufruf als Arbeitsverzeichnis
     * verwendet.
     * 
     * @param dir
     *            Arbeitsverzeichnis
     */
    public LatexRunner(LetterDirectory dir) {
        this.dir = dir;
        prepareLogWindow();
    }

    private void prepareLogWindow() {
        logWindow = new JFrame("LaTeX-Ausgabe");

        outputTextArea = new JTextArea();
        outputTextArea.setEditable(false);
        outputTextArea.setColumns(90);
        outputTextArea.setRows(30);
        outputTextArea.setFont(new Font("monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(outputTextArea);
        logWindow.setContentPane(scrollPane);

        logWindow.pack();
    }

    /**
     * Kompiliert eine Datei.
     * 
     * @param filename
     *            Dateiname (ohne Verzeichnis). Die Datei wird im
     *            Arbeitsverzeichnis gesucht.
     * @return <tt>true</tt> genau dann, wenn die Datei erfolgreich kompiliert
     *         wurde
     */
    public boolean compile(String filename) {
        File f = dir.getFile(filename);
        if (!f.exists()) {
            throw new IllegalArgumentException("File does not exist");
        }

        logWindow.setVisible(true);

        try {
            // String[] cmd2 = { "bash", "-c",
            // "for i in 1 2 3 4 5; do sleep 1; echo fertig; done; echo; echo"
            // };

            String[] cmd = { "latexmk", filename };
            if (runExternalCommand(cmd).exitValue() == 0) {
                // Temporäre Dateien löschen
                cmd = new String[] { "latexmk", "-c", filename };
                runExternalCommand(cmd);
                return true;
            } else {
                // delete generated files
                cmd = new String[] { "latexmk", "-C", filename };
                runExternalCommand(cmd);
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "Konnte Brief nicht kompilieren", e);
        }
        return false;
    }

    /**
     * Ruft den Prozess auf, sorgt dafür, dass alle Ausgaben ins Fenster
     * geschrieben werden und wartet bis der Prozess beendet ist.
     */
    private Process runExternalCommand(String[] cmd) throws IOException,
            InterruptedException {
        Runtime rt = Runtime.getRuntime();
        Process p = rt.exec(cmd, null, dir.getWorkdir());
        new Thread(new UpdateOutput(p.getInputStream())).start();
        new Thread(new UpdateOutput(p.getErrorStream())).start();
        p.waitFor();
        return p;
    }

    /**
     * Thread, der einen InputStream überwacht und alles, was hereinkommt
     * (zeilenweise) in das Ausgabefenster schreibt.
     */
    private final class UpdateOutput implements Runnable {
        private BufferedReader reader;

        private UpdateOutput(InputStream is) {
            reader = new BufferedReader(new InputStreamReader(is));
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputTextArea.append(line + "\n");
                    outputTextArea.setCaretPosition(outputTextArea.getText()
                            .length());
                }
                reader.close();
            } catch (IOException e) {
                outputTextArea.append("ERROR reading stream:\n");
                outputTextArea.append(e.toString() + "\n");
                outputTextArea.setCaretPosition(outputTextArea.getText()
                        .length());
            }
        }
    }
}
