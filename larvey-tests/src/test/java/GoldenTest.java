import com.habbashx.larvey.api.Larvey;
import com.habbashx.larvey.ast.AssignmentNode;
import com.habbashx.larvey.ast.AstDump;
import com.habbashx.larvey.ast.BlockNode;
import com.habbashx.larvey.ast.ConfigurationNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoldenTest {
    private String resource(String name) throws Exception {
        try (var input = GoldenTest.class.getResourceAsStream("/configs/" + name)) {
            if (input == null) {
                throw new IllegalStateException("Missing resource: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void checkSnapshot(String name) throws Exception {
        ConfigurationNode root = Larvey.parseAst(resource(name + ".larvey"));
        String dump = AstDump.dump(root);
        if (System.getProperty("updateSnapshots") != null) {
            Files.writeString(Path.of("src/test/resources/configs/" + name + ".ast.txt"), dump, StandardCharsets.UTF_8);
            return;
        }
        assertEquals(resource(name + ".ast.txt"), dump);
    }

    @Test
    void basicGolden() throws Exception {
        ConfigurationNode root = Larvey.parseAst(resource("basic.larvey"));
        assertEquals(6, root.members().size());
        assertEquals("name", ((AssignmentNode) root.members().get(0)).name());
        checkSnapshot("basic");
    }

    @Test
    void nestedGolden() throws Exception {
        ConfigurationNode root = Larvey.parseAst(resource("nested.larvey"));
        BlockNode app = (BlockNode) root.members().get(0);
        assertEquals("app", app.name());
        assertEquals(4, app.members().size());
        checkSnapshot("nested");
    }

    @Test
    void arraysGolden() throws Exception {
        ConfigurationNode root = Larvey.parseAst(resource("arrays.larvey"));
        assertEquals(3, root.members().size());
        assertTrue(((AssignmentNode) root.members().get(2)).value() instanceof com.habbashx.larvey.ast.ObjectNode);
        checkSnapshot("arrays");
    }
}
