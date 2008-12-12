package org.exigencecorp.selenify;

import org.apache.commons.lang.StringEscapeUtils;

public class SelenifyParser {

    private static final String defaultValueCommands = "assertTextPresent,assertTextNotPresent,verifyTextPresent,verifyTextNotPresent";
    private static final String customValueCommands = System.getProperty("selenium.value.commands", "");

    /** @return a String[] of command, arg1, arg2 */
    public static String[] parse(String line) {
        // Parse this line into an HTML table row, Ignore comments
        if (line.trim().length() == 0 || line.startsWith("#")) {
            return null;
        }

        String command;
        String arg1 = "";
        String arg2 = "";

        int firstSpace = line.indexOf(" ");
        if (firstSpace == -1) {
            command = line;
        } else {
            command = line.substring(0, firstSpace);
            // Hack to do command/arg1 (not arg2) in one line
            String args = arg1 = line.substring(firstSpace + 1);
            String[] splitOnColon = args.split("(?<!\\\\): ");
            if (splitOnColon.length > 1) {
                arg1 = splitOnColon[0];
                arg2 = splitOnColon[1];
                for (int j = 2; j < splitOnColon.length; j++) {
                    arg2 += ": " + splitOnColon[j];
                }
            } else if (SelenifyParser.defaultValueCommands.contains(command) || SelenifyParser.customValueCommands.contains(command)) {
                arg2 = arg1;
                arg1 = "";
            }
        }
        arg1 = StringEscapeUtils.escapeHtml(arg1.replaceAll("\\\\:", ":"));
        arg2 = StringEscapeUtils.escapeHtml(arg2.replaceAll("\\\\:", ":"));
        return new String[] { command, arg1, arg2 };
    }

}
