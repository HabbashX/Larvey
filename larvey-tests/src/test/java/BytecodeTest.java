import com.habbashx.larvey.api.Larvey;
import com.habbashx.larvey.bytecode.BytecodeMappers;
import com.habbashx.larvey.mapper.LarveyMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BytecodeTest {
    public static class SimpleBean {
        public String name;
        public int port;
        public long total;
        public double ratio;
        public boolean debug;
    }

    @Test
    void bytecodeMatchesReflection() {
        String source = "name = \"GazaPay\" port = 8080 total = 100 ratio = 1.5 debug = true";
        SimpleBean expected = Larvey.parse(source).map(SimpleBean.class);
        LarveyMapper bytecode = BytecodeMappers.create();
        SimpleBean actual = bytecode.map(Larvey.parseAst(source), SimpleBean.class);
        assertEquals(expected.name, actual.name);
        assertEquals(expected.port, actual.port);
        assertEquals(expected.total, actual.total);
        assertEquals(expected.ratio, actual.ratio);
        assertEquals(expected.debug, actual.debug);
    }

    @Test
    void bytecodeFallsBackForRecords() {
        record Rec(String host, int port) {
        }
        String source = "host = \"h\" port = 1";
        Rec expected = Larvey.parse(source).map(Rec.class);
        LarveyMapper bytecode = BytecodeMappers.create();
        Rec actual = bytecode.map(Larvey.parseAst(source), Rec.class);
        assertEquals(expected, actual);
    }
}
