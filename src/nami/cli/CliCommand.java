package nami.cli;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mit dieser Annotation werden Methoden markiert, die als Kommando von der
 * Shell aufgerufen werden können. Dafür muss die Methode <tt>public</tt> sein.
 * 
 * @author Fabian Lipp
 * 
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CliCommand {
    /**
     * Befehl, mit dem die Methode von der Shell aufgerufen wird.
     */
    String value();
    // String description() default "";
}
