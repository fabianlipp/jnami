package nami.cli.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Mit dieser Annotation werden Methoden markiert, die als Kommando von der
 * Shell aufgerufen werden können. Dafür muss die Methode <tt>public</tt> sein.
 * </p>
 * 
 * <p>
 * Damit die Methode von NamiCli aufgerufen werden kann, sollte sie die folgende
 * Signatur haben: <br />
 * <code>(String[] args, NamiConnector con, PrintWriter out)</code>
 * </p>
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
}
