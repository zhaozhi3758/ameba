package ameba.exception;

import java.net.URL;
import java.util.List;

/**
 * <p>SourceAttachment interface.</p>
 *
 * @author icode
 * @since 0.1.6e
 */
public interface SourceAttachment {

    /**
     * <p>getSourceUrl.</p>
     *
     * @return an array of {@link java.io.File} objects.
     */
    URL getSourceUrl();

    List<String> getSource();

    /**
     * <p>getLineNumber.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getLineNumber();

    /**
     * <p>getLineIndex.</p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    Integer getLineIndex();
}
