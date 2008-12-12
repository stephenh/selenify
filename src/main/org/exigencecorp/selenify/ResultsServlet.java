package org.exigencecorp.selenify;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Saves the results from TestRunner to txt/xml files.
 * 
 * System properties:
 * 
 * - selenium.results.webapp = mywebapp - selenium.results.directory =
 * ./build/selenium-results
 */
public class ResultsServlet implements Servlet {

	public void init(ServletConfig config) {
	}

	public ServletConfig getServletConfig() {
		return null;
	}

	public String getServletInfo() {
		return null;
	}

	public void destroy() {
	}

	public void service(ServletRequest req, ServletResponse res)
			throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		response.setContentType("text/html");
		response.setHeader("Expires", "Sat, 6 May 1995 12:00:00 GMT");
		response
				.setHeader("Cache-Control",
						"no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
		response.setHeader("Pragma", "no-cache");
		this.serviceNotCached(request, response);
	}

	public void serviceNotCached(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		this.saveToXml(request, response);
		this.saveToTxt(request, response);
		this.shutdownTheJvmThatWillBringDownJettyAsWell();
	}

	private void saveToTxt(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		FileWriter fw = new FileWriter(this.getResultsFile("txt"));
		try {
			fw.append("result=" + request.getParameter("result"));
		} finally {
			fw.close();
		}
	}

	@SuppressWarnings("unchecked")
	private void saveToXml(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		FileWriter fw = new FileWriter(this.getResultsFile("xml"));
		try {
			fw.append(new ResultsParser().toXml(this.getWebapp(),
					(Map<String, String[]>) request.getParameterMap()));
		} finally {
			fw.close();
		}
	}

	private File getResultsFile(String extension) {
		return new File(this.getDirectory() + "/SELENIUM-" + this.getWebapp()
				+ "-results." + extension);
	}

	private String getWebapp() {
		return System.getProperty("selenium.results.webapp");
	}

	private String getDirectory() {
		return System.getProperty("selenium.results.directory");
	}

	private void shutdownTheJvmThatWillBringDownJettyAsWell() {
		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					// Ignore
				}
				System.exit(0);
			}
		}).start();
	}

}
