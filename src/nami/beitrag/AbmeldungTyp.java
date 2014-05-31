package nami.beitrag;

/**
 * Beschreibt die Wege, auf denen Abmeldungen entgegen genommen werden können.
 * 
 * @author Fabian Lipp
 * 
 */
public enum AbmeldungTyp {
    /**
     * Kündigung wurde per E-Mail geschickt.
     */
    EMAIL,

    /**
     * Kündigung wurde per Brief geschickt.
     */
    SCHRIFTLICH,

    /**
     * Kündigung wurde nur mündlich ausgesprochen.
     */
    MUENDLICH
}
