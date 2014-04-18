package com.mcintyret.cache.http;

import com.mcintyret.cache.data.Cache;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * User: tommcintyre
 * Date: 4/18/14
 */
public class HttpServer {

    private static final Logger LOG = LoggerFactory.getLogger(HttpServer.class);

    private static final int PORT = 18080;

    private final Cache cache;

    public HttpServer(Cache cache) {
        this.cache = cache;
    }

    public void start() throws Exception {
        Server server = new Server(PORT);

        HandlerCollection handlers = new HandlerCollection();
        ContextHandlerCollection contexts = new ContextHandlerCollection();

        contexts.addHandler(createContextHandler("/get", new GetHandler()));

        contexts.addHandler(createContextHandler("/put", new PutHandler()));

        handlers.setHandlers(new Handler[]{contexts, new DefaultHandler()});
        server.setHandler(handlers);
        server.start();
    }

    private static ContextHandler createContextHandler(String context, Handler newHandler) {
        ContextHandler contextHandler = new ContextHandler();
        contextHandler.setContextPath(context);
        contextHandler.setAllowNullPathInfo(true);
        contextHandler.setHandler(newHandler);
        return contextHandler;
    }


    private class GetHandler extends AbstractHandler {

        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            baseRequest.setHandled(true);
            LOG.info("Handling get request");

            String key = request.getParameter("key");
            if (key == null) {
                response.setStatus(400);
                writeToResponse(response, "Must supply a key");
                return;
            }

            try {
                byte[] val = cache.get(key);

                response.setStatus(200);
                writeToResponse(response, val == null ? "null" : new String(val));
            } catch (Exception e) {
                response.setStatus(500);
                writeToResponse(response, "Error: " + e.getMessage());
            }
        }
    }

    private class PutHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            baseRequest.setHandled(true);
            LOG.info("Handling put request");

            String key = request.getParameter("key");
            if (key == null) {
                response.setStatus(400);
                writeToResponse(response, "Must supply a key");
                return;
            }

            String value = request.getParameter("val");
            if (value == null) {
                response.setStatus(400);
                writeToResponse(response, "Must supply a value");
                return;
            }

            cache.put(key, value.getBytes());
            response.setStatus(200);
        }
    }

    private static void writeToResponse(HttpServletResponse response, String message) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));
        writer.println(message);
        writer.close();
    }

}
