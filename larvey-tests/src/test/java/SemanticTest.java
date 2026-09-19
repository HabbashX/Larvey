import com.habbashx.larvey.api.Larvey;
import com.habbashx.larvey.exception.LarveyMappingException;
import com.habbashx.larvey.exception.LarveySemanticException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticTest {
    @Test
    void rejectsDuplicateProperties() {
        assertThrows(LarveySemanticException.class, () -> Larvey.parse("host = \"a\"\nhost = \"b\"").map(Holder.class));
    }

    @Test
    void rejectsDuplicateObjectProperties() {
        assertThrows(LarveySemanticException.class, () -> Larvey.parse("labels = { env = \"a\" env = \"b\" }").map(MapHolder.class));
    }

    @Test
    void rejectsUnknownFunction() {
        assertThrows(LarveyMappingException.class, () -> Larvey.parse("value = nosuchfn(\"x\")").map(Holder.class));
    }

    @Test
    void mappingErrorCarriesPath() {
        try {
            Larvey.parse("server { port = \"abc\" }").map(Nested.class);
        } catch (LarveyMappingException e) {
            assertTrue(e.getPath().contains("server.port"));
            assertTrue(e.getTargetType() != null);
            return;
        }
        throw new AssertionError("Expected LarveyMappingException");
    }

    public static class Holder {
        public String host;
        public String value;
    }

    public static class MapHolder {
        public java.util.Map<String, String> labels;
    }

    public static class Nested {
        public Server server;
    }

    public static class Server {
        public int port;
    }
}
