import com.habbashx.larvey.api.Larvey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundtripTest {
    public static class ServerConfig {
        public String host;
        public int port;
    }

    public static class AppConfig {
        public String name;
        public ServerConfig server;
    }

    @Test
    void serializesAndParsesBack() {
        AppConfig config = new AppConfig();
        config.name = "GazaPay";
        config.server = new ServerConfig();
        config.server.host = "0.0.0.0";
        config.server.port = 8080;
        String source = Larvey.write(config);
        assertTrue(source.contains("GazaPay"));
        AppConfig back = Larvey.parse(source).map(AppConfig.class);
        assertEquals("GazaPay", back.name);
        assertEquals(8080, back.server.port);
    }

    @Test
    void resolvesEnvWithDefaultAndInterpolation() {
        var mapped = Larvey.parse("host = \"localhost\" port = 8080 url = \"${host}:${port}\" user = env(\"LARVEY_MISSING_XYZ\", \"fallback\")").map(Interpolated.class);
        assertEquals("localhost:8080", mapped.url);
        assertEquals("fallback", mapped.user);
    }

    public static class Interpolated {
        public String url;
        public String user;
    }
}
