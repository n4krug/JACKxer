package space.n4krug.JACKxer.tools;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.parser.ParseException;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigParserTest extends TestCase {

    public void testFormatUsesPlainNumberNotation() throws EvaluationException, ParseException {
        Map<String, Integer> vars = new HashMap<>();
        Map<String, Integer> counters = new HashMap<>();
        counters.put("ch", 12);

        List<String> formatted = ConfigParser.format("Ch{ch}", vars, counters, new HashMap<>());

        assertEquals("counter expansion should produce plain channel names", "Ch10", formatted.get(10));
        assertFalse("formatted value must not use scientific notation", formatted.get(10).contains("E"));
    }
}
