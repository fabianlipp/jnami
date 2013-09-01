package nami.cli;

import java.util.List;
import java.util.Map;

import jline.console.completer.ArgumentCompleter;
import jline.console.completer.ArgumentCompleter.ArgumentDelimiter;
import jline.console.completer.ArgumentCompleter.ArgumentList;
import jline.console.completer.Completer;
import jline.internal.Log;
import jline.internal.Preconditions;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * A {@link Completer} implementation that splits the command buffer into the
 * first word and the remaining words. The first word ist completed using a
 * fixed completer (first level). The completer (second level) for the remainder
 * of the line is chosen dependent of the first word.
 * </p>
 * 
 * <p>
 * Only the remainder (after the first word) is passed to the complete-method of
 * the second level completer.<br />
 * The words are separated by an arbitrary number of whitespace characters.
 * </p>
 * 
 * @author Fabian Lipp
 */
public class TwoLevelCompleter implements Completer {

    private Completer firstLevel;
    private Map<String, Completer> secondLevel;

    private static ArgumentDelimiter delim;

    /**
     * Initialise the TwoLevelCompleter.
     * 
     * @param firstLevel
     *            Completer for the first word in the buffer
     * @param secondLevel
     *            Contains pairs of the keywords for the first word in the
     *            buffer and the corresponding completer to use for the
     *            remaining line.<br />
     *            The keys in the map have to be in lower case as this class
     *            works case-insensitive
     */
    public TwoLevelCompleter(Completer firstLevel,
            Map<String, Completer> secondLevel) {
        this.firstLevel = firstLevel;
        this.secondLevel = secondLevel;

        // Use whitespace as delimiter between words
        delim = new ArgumentCompleter.WhitespaceArgumentDelimiter();
    }

    @Override
    // based on ArgumentCompleter from JLine 2.11
    // original authors: Marc Prud'hommeaux, Jason Dillon
    public int complete(String buffer, int cursor, List<CharSequence> candidates) {

        // buffer can be null
        Preconditions.checkNotNull(candidates);

        ArgumentList list = delim.delimit(buffer, cursor);
        int argpos = list.getArgumentPosition();
        int argIndex = list.getCursorArgumentIndex();
        String[] args = list.getArguments();

        if (argIndex < 0) {
            return -1;
        }

        int ret;
        if (argIndex == 0) {
            // this is the first word
            ret = firstLevel.complete(list.getCursorArgument(), argpos,
                    candidates);
        } else {
            // we're not in the first word
            String firstCommand = args[0].toLowerCase();

            // concatenate remaining words
            // we lose multiple whitespace characters (compared to buffer) with
            // this operation
            String remainingCommand = StringUtils.join(args, " ", 1,
                    args.length);

            // we need to preserve a whitespace character at the end of the
            // buffer, if it was there before splitting the buffer (otherwise no
            // completion suggestions are
            // made with some completers)
            if (!remainingCommand.isEmpty()
                    && buffer.charAt(buffer.length() - 1) == ' ') {
                remainingCommand += " ";
            }

            // look for the second level completer
            if (secondLevel.containsKey(firstCommand)) {
                Completer completer = secondLevel.get(firstCommand);

                // we contract the arguments args[1..n] to args[1]
                // if the ArgumentDelimiter finds the cursor in an argument
                // after args[1] we have to update the argpos accordingly, i.e.
                // adding the lengths of the preceding arguments and the spaces
                // between the arguments
                for (int i = 2; i <= argIndex; i++) {
                    // +1 is for spaces between arguments
                    argpos += list.getArguments()[i - 1].length() + 1;
                }
                ret = completer.complete(remainingCommand, argpos, candidates);
            } else {
                // there is no second level completer for this command
                // => we cannot complete anything
                ret = -1;
            }

        }

        if (ret == -1) {
            return -1;
        }

        int pos = ret + list.getBufferPosition() - argpos;

        // Special case: when completing in the middle of a line, and the area
        // under the cursor is a delimiter,
        // then trim any delimiters from the candidates, since we do not need to
        // have an extra delimiter.
        //
        // E.g., if we have a completion for "foo", and we enter "f bar" into
        // the buffer, and move to after the "f"
        // and hit TAB, we want "foo bar" instead of "foo  bar".

        if ((cursor != buffer.length()) && delim.isDelimiter(buffer, cursor)) {
            for (int i = 0; i < candidates.size(); i++) {
                CharSequence val = candidates.get(i);

                while (val.length() > 0
                        && delim.isDelimiter(val, val.length() - 1)) {
                    val = val.subSequence(0, val.length() - 1);
                }

                candidates.set(i, val);
            }
        }

        Log.trace("Completing ", buffer, " (pos=", cursor, ") with: ",
                candidates, ": offset=", pos);

        return pos;
    }

}
