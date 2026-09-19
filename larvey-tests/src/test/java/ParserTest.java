import com.habbashx.larvey.ast.AssignmentNode;
import com.habbashx.larvey.ast.BlockNode;
import com.habbashx.larvey.ast.ConfigurationNode;
import com.habbashx.larvey.exception.LarveyParseException;
import com.habbashx.larvey.lexer.Lexer;
import com.habbashx.larvey.parser.Parser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserTest {
    private ConfigurationNode parse(String source) {
        return new Parser(new Lexer(source).tokenize()).parse();
    }

    @Test
    void parsesSimpleAssignments() {
        ConfigurationNode root = parse("name = \"HabbashX\"\nage = 21\nenabled = true\nnothing = null");
        assertEquals(4, root.members().size());
        AssignmentNode name = (AssignmentNode) root.members().get(0);
        assertEquals("name", name.name());
    }

    @Test
    void parsesNestedBlocks() {
        ConfigurationNode root = parse("server { host = \"localhost\" ssl { enabled = true port = 8443 } }");
        BlockNode server = (BlockNode) root.members().get(0);
        assertEquals("server", server.name());
        assertEquals(2, server.members().size());
        assertTrue(server.members().get(1) instanceof BlockNode);
    }

    @Test
    void parsesArrays() {
        ConfigurationNode root = parse("ports = [8080, 8081, 8082]");
        assertEquals(1, root.members().size());
    }

    @Test
    void parsesInlineObject() {
        ConfigurationNode root = parse("metadata = { author = \"HabbashX\" language = \"Java\" }");
        AssignmentNode assignment = (AssignmentNode) root.members().get(0);
        assertTrue(assignment.value() instanceof com.habbashx.larvey.ast.ObjectNode);
    }

    @Test
    void parsesFunctionCall() {
        ConfigurationNode root = parse("username = env(\"DB_USERNAME\")");
        AssignmentNode assignment = (AssignmentNode) root.members().get(0);
        assertTrue(assignment.value() instanceof com.habbashx.larvey.ast.FunctionCallNode);
    }

    @Test
    void parsesEmptyBlock() {
        ConfigurationNode root = parse("server { }");
        BlockNode server = (BlockNode) root.members().get(0);
        assertEquals(0, server.members().size());
    }

    @Test
    void throwsOnMissingEquals() {
        assertThrows(LarveyParseException.class, () -> parse("name \"value\""));
    }

    @Test
    void throwsOnUnexpectedEof() {
        assertThrows(LarveyParseException.class, () -> parse("server { host = \"x\""));
    }
}
