import com.habbashx.larvey.annotations.LarveyAlias;
import com.habbashx.larvey.annotations.LarveyConfig;
import com.habbashx.larvey.annotations.LarveyConverter;
import com.habbashx.larvey.annotations.LarveyCreator;
import com.habbashx.larvey.annotations.LarveyIgnore;
import com.habbashx.larvey.annotations.LarveyProperty;
import com.habbashx.larvey.annotations.LarveyRequired;
import com.habbashx.larvey.api.Larvey;
import com.habbashx.larvey.exception.LarveyMappingException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapperTest {
    public static class ServerConfig {
        public String host;
        public int port;
        public boolean ssl;
    }

    public static class AppConfig {
        public String name;
        public List<Integer> ports;
        public Set<String> hosts;
        public Map<String, String> labels;
        public int[] numbers;
        public Optional<String> description;
        public ServerConfig server;
    }

    public enum Driver { MYSQL, POSTGRES }

    public static class DbConfig {
        public Driver driver;
    }

    public record ServerRecord(String host, int port) {
    }

    public static class CreatorConfig {
        public final String host;
        public final int port;
        @LarveyCreator
        public CreatorConfig(@LarveyProperty("host") String host, @LarveyProperty("port") int port) {
            this.host = host;
            this.port = port;
        }
    }

    public static class AliasConfig {
        @LarveyAlias({"server-port", "serverPort"})
        public int port;
    }

    public static class IgnoredConfig {
        public String keep;
        @LarveyIgnore
        public String skip = "default";
    }

    public static class RequiredConfig {
        @LarveyRequired
        public String host;
    }

    public static class UpperConverter implements com.habbashx.larvey.convert.LarveyConverter<String, String> {
        @Override
        public String convert(String value) {
            return value == null ? null : value.toUpperCase();
        }
    }

    public static class ConvertedConfig {
        @com.habbashx.larvey.annotations.LarveyConverter(UpperConverter.class)
        public String name;
    }

    @LarveyConfig("app")
    public static class RootedConfig {
        public String name;
    }

    @Test
    void mapsPrimitivesAndNesting() {
        AppConfig config = Larvey.parse("name = \"GazaPay\" ports = [1, 2] server { host = \"h\" port = 8080 ssl = true }").map(AppConfig.class);
        assertEquals("GazaPay", config.name);
        assertEquals(List.of(1, 2), config.ports);
        assertEquals("h", config.server.host);
        assertEquals(8080, config.server.port);
        assertTrue(config.server.ssl);
    }

    @Test
    void mapsArraysSetsMapsOptional() {
        AppConfig config = Larvey.parse("hosts = [\"a\", \"b\"] labels = { env = \"prod\" } numbers = [1, 2, 3] description = \"hi\"").map(AppConfig.class);
        assertEquals(Set.of("a", "b"), config.hosts);
        assertEquals(Map.of("env", "prod"), config.labels);
        assertEquals(3, config.numbers.length);
        assertEquals(Optional.of("hi"), config.description);
    }

    @Test
    void mapsEnum() {
        DbConfig config = Larvey.parse("driver = \"MYSQL\"").map(DbConfig.class);
        assertEquals(Driver.MYSQL, config.driver);
    }

    @Test
    void mapsRecord() {
        ServerRecord record = Larvey.parse("host = \"h\" port = 1").map(ServerRecord.class);
        assertEquals("h", record.host());
    }

    @Test
    void mapsCreator() {
        CreatorConfig config = Larvey.parse("host = \"h\" port = 5").map(CreatorConfig.class);
        assertEquals("h", config.host);
        assertEquals(5, config.port);
    }

    @Test
    void mapsAlias() {
        AliasConfig config = Larvey.parse("server-port = 8080").map(AliasConfig.class);
        assertEquals(8080, config.port);
    }

    @Test
    void ignoresField() {
        IgnoredConfig config = Larvey.parse("keep = \"yes\" skip = \"no\"").map(IgnoredConfig.class);
        assertEquals("yes", config.keep);
        assertEquals("default", config.skip);
    }

    @Test
    void appliesConverter() {
        ConvertedConfig config = Larvey.parse("name = \"gaza\"").map(ConvertedConfig.class);
        assertEquals("GAZA", config.name);
    }

    @Test
    void failsOnMissingRequired() {
        assertThrows(LarveyMappingException.class, () -> Larvey.parse("").map(RequiredConfig.class));
    }

    @Test
    void mapsRootBlock() {
        RootedConfig config = Larvey.parse("app { name = \"GazaPay\" }").map(RootedConfig.class);
        assertEquals("GazaPay", config.name);
    }

    @Test
    void convertsBuiltinTypes() {
        Builtins builtins = Larvey.parse("timeout = \"PT30S\" id = \"550e8400-e29b-41d4-a716-446655440000\"").map(Builtins.class);
        assertEquals(Duration.ofSeconds(30), builtins.timeout);
        assertEquals("550e8400-e29b-41d4-a716-446655440000", builtins.id.toString());
    }

    public static class Builtins {
        public Duration timeout;
        public java.util.UUID id;
    }
}
