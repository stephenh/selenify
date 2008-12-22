package org.exigencecorp.selenify;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;

public class SelenifyParserTest extends TestCase {

    public void testCommandOnly() {
        this.assertParsed("goBackAndWait", "goBackAndWait", "", "");
    }

    public void testCommandWithOnlyValue() {
        this.assertParsed("assertTextNotPresent bob", "assertTextNotPresent", "bob", "");
    }

    public void testCommandBothArgsWithSpace() {
        this.assertParsed("assertText link=Foo Bar: text", "assertText", "link=Foo Bar", "text");
    }

    public void testCommandBothArgsWithSpaceAndEscapedColon() {
        this.assertParsed("assertText link=F\\:oo Bar: text", "assertText", "link=F:oo Bar", "text");
    }

    public void testHtml() {
        this.assertParsed("type description: <a>foo</a>", "type", "description", "&lt;a&gt;foo&lt;/a&gt;");
    }

    private void assertParsed(String line, String command, String arg1, String arg2) {
        String[] parsed = SelenifyParser.parse(line);
        Assert.assertEquals(command, parsed[0]);
        Assert.assertEquals(arg1, parsed[1]);
        Assert.assertEquals(arg2, parsed[2]);
    }

    public void testPass() {
        ResultsParser p = new ResultsParser();
        Map<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("result", new String[] { "fail" }); // we ignore this and derive it
        parameters.put("numTestPasses", new String[] { "1" });
        parameters.put("numTestFailures", new String[] { "2" }); // we ignore this and derive it
        parameters.put("totalTime", new String[] { "3" });
        parameters.put("testTable.1", new String[] { "<b>test/123-name</b>" });

        String out = p.toXml("name", parameters);
        Assert.assertEquals(StringUtils.join(new String[] {//
            "<webapp>",//
                "  <name>name</name>",//
                "  <result>passed</result>",//
                "  <passes>1</passes>",//
                "  <failures>0</failures>",//
                "  <ignores>0</ignores>",//
                "  <time>3</time>",//
                "  <tests>",//
                "    <test>",//
                "      <name>test/123-name</name>",//
                "      <commands>",//
                "      </commands>",//
                "    </test>",//
                "  </tests>",//
                "</webapp>",//
                "" },
            "\n"), out);
    }

    public void testIgnore() {
        ResultsParser p = new ResultsParser();
        Map<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put("result", new String[] { "fail" }); // we ignore this and derive it
        parameters.put("numTestPasses", new String[] { "1" });
        parameters.put("numTestFailures", new String[] { "2" }); // we ignore this and derive it
        parameters.put("totalTime", new String[] { "3" });
        parameters.put("testTable.1", new String[] { "<b>test.ignore/123-name.ignore</b>" });

        String out = p.toXml("name", parameters);
        Assert.assertEquals(StringUtils.join(new String[] {//
            "<webapp>",//
                "  <name>name</name>",//
                "  <result>passed</result>",//
                "  <passes>0</passes>",//
                "  <failures>0</failures>",//
                "  <ignores>1</ignores>",//
                "  <time>3</time>",//
                "  <tests>",//
                "    <test>",//
                "      <name>test.ignore/123-name.ignore</name>",//
                "      <commands>",//
                "      </commands>",//
                "    </test>",//
                "  </tests>",//
                "</webapp>",//
                "" },
            "\n"), out);
    }

}
