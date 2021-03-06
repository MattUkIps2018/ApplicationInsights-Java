package com.microsoft.ajl.simplecalc;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoft.applicationinsights.TelemetryClient;

import ch.qos.logback.classic.Logger;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Servlet implementation class SimpleTestTraceLogBackServlet
 */
@WebServlet(description = "calls logback", urlPatterns = "/traceLogBack")
public class SimpleTestTraceLogBackServlet extends HttpServlet {

    private static final long serialVersionUID = 8803657641175323998L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletFuncs.geRrenderHtml(request, response);

        //jbosseap6 and jbosseap7 have error : org.slf4j.impl.Slf4jLogger cannot be cast to ch.qos.logback.classic.Logger
        try {
            Logger logger = (Logger) LoggerFactory.getLogger("root");
            logger.trace("This is logback trace.");
            logger.debug("This is logback debug.");
            logger.info("This is logback info.");
            MDC.put("MDC key", "MDC value");
            logger.warn("This is logback warn.");
            MDC.remove("MDC key");
            logger.error("This is logback error.");
        } catch (Exception e) {
            TelemetryClient client = new TelemetryClient();
            client.trackException(e);
        }
    }
}