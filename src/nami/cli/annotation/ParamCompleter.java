package nami.cli.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import nami.cli.CompleterFactory;

/**
 * Beschreibt einen Completer mit dem die Parameter dieses Kommandos
 * vervollständigt werden können.
 * 
 * @author Fabian Lipp
 * 
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParamCompleter {
    /**
     * Factory-Klasse, die den Completer erzeugt. Die genauen Anforderungen an
     * diese Klasse sind im Interface {@link CompleterFactory} beschrieben.
     */
    Class<? extends CompleterFactory> value();
}
