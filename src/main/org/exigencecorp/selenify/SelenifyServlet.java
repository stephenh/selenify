package org.exigencecorp.selenify;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SelenifyServlet implements Servlet {

    private ServletConfig servletConfig;
    private String path;

    public void init(ServletConfig servletConfig) throws ServletException {
        this.servletConfig = servletConfig;
        this.path = System.getProperty("selenium.tests.directory");
        if (this.path == null) {
            this.path = servletConfig.getServletContext().getRealPath(".");
        }
    }

    public ServletConfig getServletConfig() {
        return this.servletConfig;
    }

    public String getServletInfo() {
        return null;
    }

    public void destroy() {
    }

    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        ((HttpServletResponse) response).setHeader("Expires", "Sat, 6 May 1995 12:00:00 GMT");
        ((HttpServletResponse) response).setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        ((HttpServletResponse) response).setHeader("Cache-Control", "post-check=0, pre-check=0");
        ((HttpServletResponse) response).setHeader("Pragma", "no-cache");

        // Could be:
        // webapp/selenify -- serve suite listing
        // webapp/selenify/FooBuilder -- serve suite's test listing
        // webapp/selenify/FooBuilder/Foo.test -- serve test's steps
        String uri = ((HttpServletRequest) request).getRequestURI().substring(1).replaceAll("/$", "");
        String[] parts = uri.split("/");

        String content = "";
        if (parts.length == 2) {
            if (request.getParameter("auto") != null) {
                content = this.generateTests();
            } else {
                content = this.generateTestSuites();
            }
        } else if (parts.length == 3) {
            content = this.generateTestSuite(parts[2]);
        } else if (parts.length == 4) {
            content = this.generateTest(parts[3], parts[2]);
        }

        response.setContentType("text/html");
        response.getWriter().print(content);
    }

    public String generateTestSuites() {
        StringBuffer sb = new StringBuffer();
        this.addHeader(sb);
        sb.append("<tr><td><b>All Suites</b></td></tr>\n");

        File acceptanceDirectory = new File(this.getRootPath());
        if (!acceptanceDirectory.exists()) {
            throw new RuntimeException("The test directory does not exist: " + acceptanceDirectory);
        }
        File[] files = acceptanceDirectory.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File o1, File o2) {
                return o1.getAbsolutePath().compareToIgnoreCase(o2.getAbsolutePath());
            }
        });
        for (File file : files) {
            if (file.isDirectory()) {
                sb.append("<tr><td colspan=3><a target=_top href=\"../selenium/index.html?test=../selenify/" + file.getName() + "\">");
                sb.append(file.getName());
                sb.append("</a></td></tr>\n");
            }
        }

        this.addFooter(sb);
        return sb.toString();
    }

    public String generateTests() {
        StringBuffer sb = new StringBuffer();
        this.addHeader(sb);
        sb.append("<tr><td><b>All Tests</b></td></tr>\n");

        File acceptanceDirectory = new File(this.getRootPath());
        if (!acceptanceDirectory.exists()) {
            throw new RuntimeException("The test directory does not exist: " + acceptanceDirectory);
        }
        File[] files = acceptanceDirectory.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File o1, File o2) {
                return o1.getAbsolutePath().compareToIgnoreCase(o2.getAbsolutePath());
            }
        });
        for (File file : files) {
            if (file.isDirectory()) {
                this.generateTestSuite(file.getName(), sb);
            }
        }

        this.addFooter(sb);
        return sb.toString();
    }

    public String generateTestSuite(String suiteName) throws ServletException, IOException {
        StringBuffer sb = new StringBuffer();
        this.addHeader(sb);
        sb.append("<tr><td><b>").append(suiteName).append(" Suite</b></td></tr>\n");
        this.generateTestSuite(suiteName, sb);
        this.addFooter(sb);
        return sb.toString();
    }

    public void generateTestSuite(String suiteName, StringBuffer sb) {
        File suiteDirectory = new File(this.getRootPath() + "/" + suiteName);
        if (!suiteDirectory.exists()) {
            throw new RuntimeException("The suite directory does not exist: " + suiteDirectory);
        }
        File[] files = suiteDirectory.listFiles();
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File o1, File o2) {
                return o1.getAbsolutePath().compareToIgnoreCase(o2.getAbsolutePath());
            }
        });
        for (File file : files) {
            String testName = file.getName();
            // Protect against display silly things like '.' - wtf are they
            // returned from listFiles is beyond me
            if (this.skipFile(testName)) {
                continue;
            }
            this.addTest(testName, suiteName, sb);
        }
    }

    public String generateTest(String fileName, String group) throws ServletException, IOException {
        String testName = group + "/" + fileName.substring(0, fileName.indexOf("."));
        StringBuffer sb = new StringBuffer();
        this.addHeader(sb);

        sb.append("<tr><td colspan=3><b>").append(testName).append("</b></td></tr>\n");

        List<String> lines = new ArrayList<String>();
        lines.addAll(this.readLines(group + "/" + fileName));

        // Preprocess looking for any includes
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("&")) {
                lines.remove(i);
                lines.addAll(i, this.readLines("_" + line.substring(1)));
                i = -1;
            }
        }

        for (String line : lines) {
            String[] parsed = SelenifyParser.parse(line);
            if (parsed == null) {
                continue;
            }
            sb.append("<tr><td>").append(parsed[0]);
            sb.append("</td><td>").append(parsed[1]);
            sb.append("</td><td>").append(parsed[2]);
            sb.append("</td></tr>\n");
        }

        this.addFooter(sb);
        return sb.toString();
    }

    protected Collection<String> readLines(String groupPlusFileName) throws IOException {
        File txtFile = new File(this.getRootPath() + "/" + groupPlusFileName);
        BufferedReader reader = new BufferedReader(new FileReader(txtFile));
        List<String> lines = new ArrayList<String>();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        reader.close();
        return lines;
    }

    private boolean skipFile(String name) {
        return name.startsWith(".") || (name.length() == 0 || name.startsWith("_"));
    }

    private void addTest(String fileName, String suiteName, StringBuffer sb) {
        if (fileName.indexOf(".txt") == -1) {
            return;
        }
        String testName = fileName.substring(0, fileName.indexOf("."));
        sb.append("<tr><td><a target=\"testFrame\" href=\"");
        sb.append(suiteName);
        sb.append("/");
        sb.append(fileName);
        sb.append("\">");
        sb.append(testName);
        sb.append("</a></td></tr>\n");
    }

    protected String getRootPath() {
        return this.path + "/../acceptance";
    }

    private void addHeader(StringBuffer sb) {
        sb.append("<html>\n");
        sb.append("<head><style>");
        sb.append("td { white-space: nowrap; font-family: Verdana; font-size: 8pt; }");
        sb.append("table { border-collapse: collapse; border: 0px; }");
        sb.append("</style></head>\n");
        sb.append("<body topmargin=2 leftmargin=2>\n");
        sb.append("<table cellpadding=2>\n");
    }

    private void addFooter(StringBuffer sb) {
        sb.append("</table>\n");
        sb.append("</body>\n");
        sb.append("</html>\n");
    }

}
