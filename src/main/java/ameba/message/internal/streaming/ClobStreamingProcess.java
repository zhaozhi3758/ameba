package ameba.message.internal.streaming;

import ameba.message.internal.StreamingProcess;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.message.internal.ReaderWriter;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.sql.Clob;
import java.sql.SQLException;

/**
 * @author icode
 */
@Singleton
public class ClobStreamingProcess implements StreamingProcess<Clob> {

    @Override
    public boolean isSupported(Object entity) {
        return entity instanceof Clob;
    }

    @Override
    public long length(Clob entity) throws IOException {
        try {
            return entity.length();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(Clob entity, OutputStream output, Long pos, Long length) throws IOException {
        Reader reader;
        try {
            reader = entity.getCharacterStream(pos, length);
        } catch (SQLException e) {
            throw new IOException(e);
        }
        try {
            ReaderWriter.writeTo(reader, new OutputStreamWriter(output));
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
}
