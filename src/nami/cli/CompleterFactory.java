package nami.cli;

import jline.console.completer.Completer;

/**
 * <p>
 * Interface, das eine Klasse beschreibt, die einen Completer liefert. Diese
 * wird mittels einer Annotation an ein Cli-Kommando angehängt. Diese Klasse ist
 * also ein Workaround, weil Objekte nicht direkt als Attribute einer Annotation
 * verwendet werden können.
 * </p>
 * 
 * <p>
 * Damit die Klasse von NamiCli verwendet werden kann, muss sie einen
 * Public-Konstruktor ohne Parameter haben, also mit class.newInstance()
 * initialisierbar sein. Solange keine eingenen Konstruktoren definiert werden,
 * wird dieser automatisch erzeugt.
 * </p>
 * 
 * <p>
 * Die Klasse besitzt keine Felder. Im Prinzip könnte also die Methode auch
 * static sein, was mit einem Interface (bzw. auch mit einer abstrakten Klasse)
 * aber in Java nicht möglich ist.
 * 
 * @author Fabian Lipp
 * 
 */
public interface CompleterFactory {
    /**
     * Liefert den Completer.
     * 
     * @return Completer, den diese Klasse beschreibt
     */
    Completer getCompleter();
}
