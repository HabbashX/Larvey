import com.habbashx.larvey.api.Larvey;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FunctionsTest {
    public static class FileHolder {
        public String content;
    }

    public static class AliasHolder {
        public String primary;
        public String copy;
    }

    @Test
    void readsFileFunction() throws Exception {
        Path file = Files.createTempFile("larvey", ".txt");
        Files.writeString(file, "hello-file", StandardCharsets.UTF_8);
        String escaped = file.toString().replace("\\", "\\\\");
        FileHolder holder = Larvey.parse("content = file(\"" + escaped + "\")").map(FileHolder.class);
        assertEquals("hello-file", holder.content);
    }

    @Test
    void readsFileFunctionWithDefault() {
        FileHolder holder = Larvey.parse("content = file(\"/no/such/larvey-file.txt\", \"fallback\")").map(FileHolder.class);
        assertEquals("fallback", holder.content);
    }

    @Test
    void resolvesPropertyFunction() {
        AliasHolder holder = Larvey.parse("primary = \"gaza\"\ncopy = property(\"primary\")").map(AliasHolder.class);
        assertEquals("gaza", holder.copy);
    }

    @Test
    void resolvesNestedPropertyFunction() {
        AliasHolder holder = Larvey.parse("server { host = \"db\" }\ncopy = property(\"server.host\")").map(AliasHolder.class);
        assertEquals("db", holder.copy);
    }

    @Test
    void detectsCyclicPropertyReference() {
        try {
            Larvey.parse("copy = property(\"copy\")").map(AliasHolder.class);
        } catch (RuntimeException e) {
            return;
        }
        throw new AssertionError("Expected cycle detection");
    }

    @Test
    void concatenatesStrings() {
        FileHolder holder = Larvey.parse("content = concat(\"foo\", \"-\", \"bar\")").map(FileHolder.class);
        assertEquals("foo-bar", holder.content);
    }

    @Test
    void changesCase() {
        FileHolder upper = Larvey.parse("content = upper(\"gaza\")").map(FileHolder.class);
        assertEquals("GAZA", upper.content);
        FileHolder lower = Larvey.parse("content = lower(\"GaZa\")").map(FileHolder.class);
        assertEquals("gaza", lower.content);
    }

    @Test
    void trimsWhitespace() {
        FileHolder holder = Larvey.parse("content = trim(\"  padded  \")").map(FileHolder.class);
        assertEquals("padded", holder.content);
    }

    @Test
    void composesFunctions() {
        FileHolder holder = Larvey.parse("content = upper(concat(\"ga\", \"za\"))").map(FileHolder.class);
        assertEquals("GAZA", holder.content);
    }
}
