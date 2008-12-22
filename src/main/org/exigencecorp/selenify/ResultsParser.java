package org.exigencecorp.selenify;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

public class ResultsParser {

    private final Pattern rowPattern = Pattern.compile("(?s)<tr.*?</tr>", Pattern.CASE_INSENSITIVE);
    private final Pattern testCasePattern = Pattern.compile("(?s)<b>([\\w-_]+/?[\\w-_]+)</b>", Pattern.CASE_INSENSITIVE);
    private final Pattern statusPattern = Pattern.compile("class=\"?(#?\\w+)\"?", Pattern.CASE_INSENSITIVE);
    private final Pattern commandArgsPattern = Pattern.compile("(?s)<td.*?>(.*?)</td>", Pattern.CASE_INSENSITIVE);
    private final Pattern originalHtmlPattern = Pattern.compile("originalhtml=\"(.*?)\"", Pattern.CASE_INSENSITIVE);

    public String toXml(String webappName, Map<String, String[]> parameters) {
        // In case the request didn't really come from selenium, don't NPE out
        if (parameters.get("numTestPasses") == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<webapp>\n");
        this.appendStats(sb, webappName, parameters);
        sb.append("  <tests>\n");
        for (int i = 1; parameters.get("testTable." + i) != null; i++) {
            String table = parameters.get("testTable." + i)[0];
            this.appendTest(sb, table);
        }
        sb.append("  </tests>\n");
        sb.append("</webapp>\n");

        return sb.toString();
    }

    private void appendStats(StringBuilder sb, String webappName, Map<String, String[]> parameters) {
        sb.append("  " + this.createTag("name", webappName));
        sb.append("  " + this.createTag("result", parameters.get("result")[0]));
        sb.append("  " + this.createTag("passes", parameters.get("numTestPasses")[0]));
        sb.append("  " + this.createTag("failures", parameters.get("numTestFailures")[0]));
        sb.append("  " + this.createTag("time", parameters.get("totalTime")[0]));
    }

    private void appendTest(StringBuilder sb, String table) {
        Matcher testCaseMatcher = this.testCasePattern.matcher(table);
        String name = "Unknown";
        if (testCaseMatcher.find()) {
            name = testCaseMatcher.group(1);
        }

        sb.append("    <test>\n");
        sb.append("      <name>" + StringEscapeUtils.escapeXml(name) + "</name>\n");
        sb.append("      <commands>\n");

        Matcher rowMatcher = this.rowPattern.matcher(table);
        while (rowMatcher.find()) {
            String row = rowMatcher.group();
            if (this.testCasePattern.matcher(row).find()) {
                continue;
            }

            String status = this.getStatus(row);
            String[] commandAndArgs = this.getCommandAndArgs(row);
            sb.append("        <command>\n");
            sb.append("          <status>" + status + "</status>\n");
            sb.append("          <name>" + StringEscapeUtils.escapeXml(commandAndArgs[0]) + "</name>\n");
            sb.append("          <arg1>" + StringEscapeUtils.escapeXml(commandAndArgs[1]) + "</arg1>\n");
            sb.append("          <arg2>" + StringEscapeUtils.escapeXml(commandAndArgs[2]) + "</arg2>\n");
            sb.append("          <message>" + StringEscapeUtils.escapeXml(commandAndArgs[3]) + "</message>\n");
            sb.append("        </command>\n");
        }

        sb.append("      </commands>\n");
        sb.append("    </test>\n");
    }

    private String getStatus(String row) {
        Matcher statusMatcher = this.statusPattern.matcher(row);
        if (statusMatcher.find()) {
            String status = statusMatcher.group(1);
            if ("status_failed".equalsIgnoreCase(status)) {
                return "fail";
            } else if ("status_done".equalsIgnoreCase(status)) {
                return "success";
            } else {
                return "none";
            }
        }
        return "none";
    }

    private String[] getCommandAndArgs(String row) {
        String commandName = null;
        String arg1 = null;
        String arg2 = null;
        String message = null;

        Matcher commandArgsMatcher = this.commandArgsPattern.matcher(row);
        if (commandArgsMatcher.find(0)) {
            commandName = commandArgsMatcher.group(1);
            if (commandArgsMatcher.find(commandArgsMatcher.end())) {
                arg1 = commandArgsMatcher.group(1);
                if (commandArgsMatcher.find(commandArgsMatcher.end())) {
                    message = commandArgsMatcher.group(1);
                }
            }
        }

        Matcher originalHtmlMatcher = this.originalHtmlPattern.matcher(row);
        if (originalHtmlMatcher.find()) {
            arg2 = StringEscapeUtils.unescapeHtml(originalHtmlMatcher.group(1));
        }

        if (arg2 != null && arg2.equals(message)) {
            message = "";
        }

        return new String[] { commandName, arg1, arg2, message };
    }

    private String createTag(String tagName, String tagValue) {
        return "<" + tagName + ">" + StringEscapeUtils.escapeXml(tagValue) + "</" + tagName + ">\n";
    }

}
