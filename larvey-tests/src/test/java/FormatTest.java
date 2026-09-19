import com.habbashx.larvey.annotations.LarveyFormat;
import com.habbashx.larvey.api.Larvey;
import com.habbashx.larvey.exception.LarveyMappingException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormatTest {
    public static class CodeConfig {
        @LarveyFormat("regex:[A-Z]{3}-[0-9]{3}")
        public String code;
    }

    public static class PortConfig {
        @LarveyFormat("[0-9]+")
        public String port;
    }

    @Test
    void acceptsMatchingFormat() {
        CodeConfig config = Larvey.parse("code = \"ABC-123\"").map(CodeConfig.class);
        assertEquals("ABC-123", config.code);
    }

    @Test
    void rejectsMismatchedFormat() {
        assertThrows(LarveyMappingException.class, () -> Larvey.parse("code = \"abc\"").map(CodeConfig.class));
    }

    @Test
    void rejectsInvalidRegex() {
        assertThrows(LarveyMappingException.class, () -> Larvey.parse("port = \"8080\"").map(BrokenConfig.class));
    }

    public static class BrokenConfig {
        @LarveyFormat("([")
        public String port;
    }
}
