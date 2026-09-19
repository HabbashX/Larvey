import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessorTest {
    @Test
    void generatedMetadataExists() throws Exception {
        Class<?> meta = Class.forName("MapperTest_RootedConfigLarveyMeta");
        assertEquals("app", meta.getField("ROOT").get(null));
        Object properties = meta.getField("PROPERTIES").get(null);
        assertTrue(properties.toString().contains("name"));
    }
}
