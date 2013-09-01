package nami.cli.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mit dieser Annotation können zusätzliche Befehle angegeben werden, mit denen
 * die Methode aufggerufen wird.
 * 
 * @author Fabian Lipp
 * 
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AlternateCommands {
    /**
     * Zusätzliche Befehle (neben dem Argument von {@link CliCommand}), mit
     * denen die Methode von der Shell aufgerufen werden kann.
     */
    String[] value();
}
