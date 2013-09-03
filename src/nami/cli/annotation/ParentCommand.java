package nami.cli.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Mit dieser Annotation werden Methoden einem übergeordneten Kommando
 * zugewiesen. Beim Erzeugen einer {@link CliParser}-Instanz wird nach dem Wert
 * dieser Annotation gefiltert, d.h. es werden nur Methoden hinzugefügt, die zu
 * einem bestimmten übergeordneten Befehl gehören.
 * </p>
 * 
 * <p>
 * Es kann auch Kommandos geben, die nicht mit dieser Annotation markiert sind.
 * Diese werden dann ausgewählt, wenn beim Erzeugen des <tt>CliParses</tt> kein
 * übergeordnetes Kommando angegeben wird.
 * </p>
 * 
 * @author Fabian Lipp
 * 
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParentCommand {
    /**
     * Übergeordneter Befehl, zu dem diese Methode gehört.
     */
    String value();
}
