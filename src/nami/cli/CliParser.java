package nami.cli;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import jline.console.completer.StringsCompleter;
import nami.cli.annotation.AlternateCommands;
import nami.cli.annotation.CliCommand;
import nami.cli.annotation.CommandDoc;
import nami.cli.annotation.ParamCompleter;
import nami.cli.annotation.ParentCommand;
import nami.connector.NamiConnector;

/**
 * Klasse, die die Kommandozeile parst und abhängig vom ersten Wort in dieser
 * Zeile eine bestimmte Funktion aufruft. Die aufzurufenden Funktionen sind
 * durch die Annotation {@link CliCommand} in vorgegebenen Klassen
 * gekennzeichnet.
 * 
 * @author Fabian Lipp
 * 
 */
public class CliParser {

    private static Logger log = Logger.getLogger(CliParser.class
            .getCanonicalName());

    /**
     * Enthält die Kommandos, die NamiCli versteht, und die zugeordneten
     * Methoden, die dann aufgerufen werden.
     */
    private Map<String, Method> commands = new HashMap<>();
    /**
     * Enthält die primären Namen der Kommandos. Hier werden also nur die
     * Kommandos eingefügt, die als Attribut einer {@link CliCommand}-Annotation
     * angegeben sind, nicht die die nur als Attribut einer
     * {@link AlternateCommands}-Annotation angegeben sind.
     * 
     * Diese Liste wird beispielsweise verwendet, um die Hilfe mit der
     * Kurzbeschreibung der Kommandos anzuzeigen.
     */
    private List<String> commandNames = new LinkedList<>();
    /**
     * Enthält alle Kommandos, also auch die aus {@link AlternateCommands}.
     */
    private List<String> allCommandNames = new LinkedList<>();
    /**
     * Enthält die Second-Level-Completer für jedes der unterstützten Kommandos.
     */
    private Map<String, Completer> completersMap = new HashMap<>();

    /**
     * Initialisiert den Parser.
     * 
     * @param commandClasses
     *            Klassen, in denen die aufzurufenden Methoden enthalten sind.
     * @param parentCommand
     *            der Wert, den die {@link ParentCommand}-Annotation enthalten
     *            muss, damit die Methode berücksichtigt wird. Falls hier
     *            <tt>null</tt> übergeben wird, werden nur Methoden gewählt, bei
     *            denen die Annotation nicht vorhanden ist.
     */
    public CliParser(Class<?>[] commandClasses, String parentCommand) {
        // Read available commands from methods annotated with CliCommand in
        // commandClasses
        for (Class<?> cls : commandClasses) {
            for (Method mtd : cls.getMethods()) {
                if (!checkParentCommand(parentCommand, mtd)) {
                    continue;
                }

                CliCommand anno = mtd.getAnnotation(CliCommand.class);
                if (anno != null) {
                    if (!Modifier.isStatic(mtd.getModifiers())) {
                        log.warning("Cannot use method " + cls.getName() + "."
                                + mtd.getName()
                                + " (annotated with CliCommand)"
                                + " because it is not static.");
                    } else if (!checkSignature(mtd)) {
                        log.warning("Cannot use method " + cls.getName() + "."
                                + mtd.getName()
                                + " (annotated with CliCommand)"
                                + " because its parameters are wrong.");
                    }

                    Completer completer = getCompleterForMethod(mtd);
                    addToCommands(anno.value(), mtd, completer, true);

                    AlternateCommands alt = mtd
                            .getAnnotation(AlternateCommands.class);
                    if (alt != null) {
                        for (String str : alt.value()) {
                            addToCommands(str, mtd, completer, false);
                        }
                    }
                }
            }
        }
        Collections.sort(commandNames);
    }

    /**
     * Initialisiert den Parser.
     * 
     * @param commandClass
     *            Klasse, in der die aufzurufenden Methoden enthalten sind.
     * @param parentCommand
     *            der Wert, den die {@link ParentCommand}-Annotation enthalten
     *            muss, damit die Methode berücksichtigt wird. Falls hier
     *            <tt>null</tt> übergeben wird, werden nur Methoden gewählt, bei
     *            denen die Annotation nicht vorhanden ist.
     */
    public CliParser(Class<?> commandClass, String parentCommand) {
        this(new Class<?>[] { commandClass }, parentCommand);
    }

    /**
     * Initialisiert den Parser. Es werden nur Methoden berücksichtigt, bei
     * denen keine {@link ParentCommand}-Annotation vorhanden ist.
     * 
     * @param commandClasses
     *            Klassen, in denen die aufzurufenden Methoden enthalten sind.
     */
    public CliParser(Class<?>[] commandClasses) {
        this(commandClasses, null);
    }

    /**
     * Initialisiert den Parser. Es werden nur Methoden berücksichtigt, bei
     * denen keine {@link ParentCommand}-Annotation vorhanden ist.
     * 
     * @param commandClass
     *            Klasse, in der die aufzurufenden Methoden enthalten sind.
     */
    public CliParser(Class<?> commandClass) {
        this(commandClass, null);
    }

    /**
     * Erzeugt einen Completer für eine CliCommand-Methode. Dazu wird die
     * entsprechende Annotation ausgelesen und mit der dort angegeben
     * Factory-Klasse der Completer erzeugt.
     * 
     * @param mtd
     *            Methode
     * @return Der Completer, falls die Annotation vorhanden war und dieser
     *         erzeugt werden konnte; andernfalls wird ein
     *         <tt>NullCompleter</tt> zurückgegeben.
     */
    private static Completer getCompleterForMethod(Method mtd) {
        Completer completer = NullCompleter.INSTANCE;
        if (mtd.isAnnotationPresent(ParamCompleter.class)) {
            try {
                ParamCompleter anno = mtd.getAnnotation(ParamCompleter.class);
                Class<? extends CompleterFactory> complFacCls = anno.value();
                CompleterFactory complFac = complFacCls.newInstance();
                completer = complFac.getCompleter();
            } catch (InstantiationException | IllegalAccessException e) {
                log.log(Level.WARNING,
                        "Could not initialise completer for command", e);
            }
        }
        return completer;
    }

    /**
     * Fügt ein Kommado in die entsprechenden Listen dieser Klasse ein.
     * 
     * @param commandName
     *            Kommando
     * @param mtd
     *            aufzurufende Methode
     * @param completer
     *            Completer für die Parameter des Kommandos
     * @param primary
     *            <tt>true</tt>, falls dies ein primäres Kommando ist, also als
     *            Attribut von {@link CliCommand} angegeben ist
     */
    private void addToCommands(String commandName, Method mtd,
            Completer completer, boolean primary) {
        commands.put(commandName.toLowerCase(), mtd);
        allCommandNames.add(commandName);
        if (primary) {
            commandNames.add(commandName);
        }
        completersMap.put(commandName.toLowerCase(), completer);
    }

    /**
     * Tested, ob die Signatur einer Methode (Parameter und deren Datentypen)
     * für den Aufruf als Cli-Kommando geeignet ist.
     * 
     * @param mtd
     *            zu prüfende Methode
     * @return <tt>true</tt>, falls die Methode die passenden Parameter
     *         entgegennimmt
     */
    private static boolean checkSignature(Method mtd) {
        Class<?>[] params = mtd.getParameterTypes();
        if (params.length != 3) {
            return false;
        }

        if (!(new String[0]).getClass().isAssignableFrom(params[0])) {
            return false;
        } else if (!NamiConnector.class.isAssignableFrom(params[1])) {
            return false;
        } else if (!PrintWriter.class.isAssignableFrom(params[2])) {
            return false;
        }

        return true;
    }

    /**
     * Überprüft, ob die {@link ParentCommand}-Annotation mit dem als String
     * übergebenen Kommando übereinstimmt.
     * 
     * @param parentCommand
     *            das Kommando, mit dem verglichen werden soll
     * @param mtd
     *            die Methode, deren Annotation verglichen werden soll
     * @return <tt>true</tt>, falls die Methode die passende Annotation besitzt
     */
    private static boolean checkParentCommand(String parentCommand, Method mtd) {
        ParentCommand parentAnno = mtd.getAnnotation(ParentCommand.class);

        if (parentCommand == null && parentAnno != null) {
            // es ist kein ParentCommand als Parameter übergeben, aber eine
            // Annotation ist vorhanden
            return false;
        } else if (parentCommand != null) {
            if (parentAnno == null) {
                // es ist ein ParentCommand als Parameter übergeben, aber keine
                // Annotation ist vorhanden
                return false;
            } else {
                if (!parentCommand.equals(parentAnno.value())) {
                    // Das ParentCommand im Parameter stimmt nicht mit dem Wert
                    // der Annotation überein
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Liefert einen Completer, der die Kommandos, die der Parser versteht
     * vervollständigt. Falls angegeben, werden auch die Second-Level-Parser der
     * Kommandos (beschrieben durch die Annotation {@link ParamCompleter}) mit
     * berücksichtigt.
     * 
     * @return JLine-Completer
     */
    public Completer getCompleter() {
        return new TwoLevelCompleter(new StringsCompleter(allCommandNames),
                completersMap);
    }

    /**
     * Ruft die einer Kommandozeile entsprechende Funktion auf.
     * 
     * @param line
     *            Kommandozeile
     * @param con
     *            Verbindung zum NaMi-Server
     * @param out
     *            PrintWriter, auf dem die Ausgabe erfolgt
     */
    public void callMethod(String line, NamiConnector con, PrintWriter out) {
        // split line at spaces
        String[] lineSplitted = splitCommandline(line);
        String[] arguments = Arrays.copyOfRange(lineSplitted, 1,
                lineSplitted.length);

        // Call method for command
        Method mtd = commands.get(lineSplitted[0].toLowerCase());
        if (lineSplitted[0].toLowerCase().equals("help")) {
            printHelp(arguments, out);
        } else if (mtd == null) {
            // no correspondig method in map
            out.println("Unknown Command");
        } else {
            try {
                mtd.invoke(null, new Object[] { arguments, con, out });
            } catch (InvocationTargetException e) {
                // called method throws an exception
                e.getTargetException().printStackTrace(out);
            } catch (Exception e) {
                e.printStackTrace(out);
            }
        }
    }

    /**
     * Ruft die einer Kommandozeile entsprechende Funktion auf.
     * 
     * @param args
     *            Kommandozeile, aufgeteilt in einzelne Wörter
     * @param con
     *            Verbindung zum NaMi-Server
     * @param out
     *            PrintWriter, auf dem die Ausgabe erfolgt
     */
    public void callMethod(String[] args, NamiConnector con, PrintWriter out) {
        callMethod(StringUtils.join(args, ' '), con, out);
    }

    /**
     * Teilt den übergebenen String an den Leerzeichen auf. Leerzeichen die
     * innerhalb von Anführungszeichen stehen, werden ignoriert. Das Ergebnis
     * entspricht quasi dem <tt>args</tt>-Array, das der main-Funktion übergeben
     * wird.
     * 
     * @param line
     *            aufzuteilende Zeile
     * @return an den Leerzeichen aufgeteilte Zeile
     */
    private static String[] splitCommandline(String line) {
        // Quelle:
        // http://stackoverflow.com/questions/366202/regex-for-splitting-
        // a-string-using-space-when-not-surrounded-by-single-or-double
        List<String> matchList = new LinkedList<>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(line);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group());
            }
        }
        return matchList.toArray(new String[0]);
    }

    /**
     * Gibt die verfügbaren Befehle und eine kurze Zusammenfassung dieser aus.
     * 
     * @param args
     *            nicht verwendet
     * @param out
     *            Writer, auf dem die Ausgabe erfolgt
     */
    public void printHelp(String[] args, PrintWriter out) {
        // TODO Werte Argumente aus und zeige ggf. Detail zu Befehl an

        int longestCommand = 0;
        for (String command : commandNames) {
            if (command.length() > longestCommand) {
                longestCommand = command.length();
            }
        }

        out.println("Verfügbare Befehle:");
        out.println();
        String formatString = "  %-" + longestCommand + "s  %s";
        for (String command : commandNames) {
            String description = getShortDescription(command);
            out.println(String.format(formatString, command, description));
        }
    }

    /**
     * Liefert die Beschreibung eines Kommandos aus der
     * <tt>{@link CommandDoc}</tt>-Annotation.
     * 
     * @param command
     *            Kommando
     * @return der Beschreibungstext
     */
    private String getShortDescription(String command) {
        Method mtd = commands.get(command.toLowerCase());
        if (mtd != null) {
            CommandDoc anno = mtd.getAnnotation(CommandDoc.class);
            if (anno != null) {
                return anno.value();
            }
        }

        return "";
    }
}
