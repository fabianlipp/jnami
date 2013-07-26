package nami.cli;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Beschreibt die Funktionsweise des Kommandos.
 * 
 * @author Fabian Lipp
 * 
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandDoc {
    /**
     * Kurze Erklärung der Wirkung des Kommandos.
     */
    String value();
}
