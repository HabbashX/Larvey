import com.habbashx.larvey.annotations.LarveyAlias;
import com.habbashx.larvey.annotations.LarveyCreator;
import com.habbashx.larvey.annotations.LarveyDefault;
import com.habbashx.larvey.annotations.LarveyProperty;
import com.habbashx.larvey.api.Larvey;
import com.habbashx.larvey.ast.ConfigurationNode;
import com.habbashx.larvey.mapper.LarveyMapper;
import com.habbashx.larvey.mapper.MappingStrategy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BytecodeParityTest {
    enum Level { LOW, HIGH }

    public static class PrivateBean {
        private String host;
        private int port;
        public String getHost() {
            return host;
        }
        public int getPort() {
            return port;
        }
    }

    public static class NestedBean {
        public String name;
        public PrivateBean server;
        public List<Integer> ports;
        public Set<String> tags;
        public int[] codes;
        public Map<String, String> labels;
        public Optional<String> note;
        public Level level;
        public UUID id;
    }

    public record AppRecord(String name, int port, List<String> hosts, NestedRecord nested) {
    }

    public record NestedRecord(String host, boolean ssl) {
    }

    public static class CreatorBean {
        private final String host;
        private final int port;
        @LarveyCreator
        public CreatorBean(@LarveyProperty("host") String host, @LarveyProperty("port") @LarveyDefault("8080") int port) {
            this.host = host;
            this.port = port;
        }
    }

    public static class AliasBean {
        private int port;
        @LarveyAlias({"server-port"})
        public void setPort(int port) {
            this.port = port;
        }
        public int getPort() {
            return port;
        }
    }

    private final LarveyMapper reflection = LarveyMapper.builder().strategy(MappingStrategy.REFLECTION).build();
    private final LarveyMapper bytecode = LarveyMapper.builder().strategy(MappingStrategy.BYTECODE).build();

    private <T> void assertParity(String source, Class<T> type, java.util.function.Function<T, Object> view) {
        ConfigurationNode ast = Larvey.parseAst(source);
        assertEquals(view.apply(reflection.map(ast, type)), view.apply(bytecode.map(ast, type)));
    }

    @Test
    void privateFields() {
        assertParity("host = \"h\" port = 1", PrivateBean.class, b -> b.getHost() + b.getPort());
    }

    @Test
    void nestedAndCollections() {
        String source = "name = \"app\" server { host = \"h\" port = 2 } ports = [1, 2] tags = [\"a\"] codes = [7] labels = { env = \"prod\" } note = \"hi\" level = \"HIGH\" id = \"550e8400-e29b-41d4-a716-446655440000\"";
        ConfigurationNode ast = Larvey.parseAst(source);
        NestedBean expected = reflection.map(ast, NestedBean.class);
        NestedBean actual = bytecode.map(ast, NestedBean.class);
        assertEquals(expected.name, actual.name);
        assertEquals(expected.server.getHost(), actual.server.getHost());
        assertEquals(expected.server.getPort(), actual.server.getPort());
        assertEquals(expected.ports, actual.ports);
        assertEquals(expected.tags, actual.tags);
        assertEquals(expected.codes.length, actual.codes.length);
        assertEquals(expected.labels, actual.labels);
        assertEquals(expected.note, actual.note);
        assertEquals(expected.level, actual.level);
        assertEquals(expected.id, actual.id);
    }

    @Test
    void records() {
        String source = "name = \"app\" port = 1 hosts = [\"a\"] nested { host = \"h\" ssl = true }";
        ConfigurationNode ast = Larvey.parseAst(source);
        assertEquals(reflection.map(ast, AppRecord.class), bytecode.map(ast, AppRecord.class));
    }

    @Test
    void creatorWithDefault() {
        assertParity("host = \"h\"", CreatorBean.class, b -> b.host + b.port);
        assertParity("host = \"h\" port = 5", CreatorBean.class, b -> b.host + b.port);
    }

    @Test
    void setterAlias() {
        assertParity("server-port = 9", AliasBean.class, AliasBean::getPort);
    }
}
