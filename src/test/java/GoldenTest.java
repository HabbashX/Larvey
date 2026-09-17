import com.habbashx.larvey.api.Larvey;
import com.habbashx.larvey.ast.AssignmentNode;
import com.habbashx.larvey.ast.BlockNode;
import com.habbashx.larvey.ast.ConfigurationNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoldenTest {
    private ConfigurationNode load(String name) throws Exception {
        String source = Files.readString(Path.of("src/test/resources/configs/" + name));
        return Larvey.parseAst(source);
    }

    @Test
    void basicGolden() throws Exception {
        ConfigurationNode root = load("basic.larvey");
        assertEquals(6, root.members().size());
        assertEquals("name", ((AssignmentNode) root.members().get(0)).name());
    }

    @Test
    void nestedGolden() throws Exception {
        ConfigurationNode root = load("nested.larvey");
        BlockNode app = (BlockNode) root.members().get(0);
        assertEquals("app", app.name());
        assertEquals(4, app.members().size());
    }

    @Test
    void arraysGolden() throws Exception {
        ConfigurationNode root = load("arrays.larvey");
        assertEquals(3, root.members().size());
        assertTrue(((AssignmentNode) root.members().get(2)).value() instanceof com.habbashx.larvey.ast.ObjectNode);
    }
}
