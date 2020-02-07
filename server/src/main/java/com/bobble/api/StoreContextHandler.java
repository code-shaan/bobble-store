package com.bobble.api;

import io.opentracing.Span;
import io.opentracing.contrib.web.servlet.filter.ServletFilterSpanDecorator;
import java.io.IOException;
import java.util.*;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.bobble.api.resources.*;

import io.opentracing.util.GlobalTracer;
import io.opentracing.contrib.web.servlet.filter.TracingFilter;

public class StoreContextHandler extends ServletContextHandler {
    StoreService storeService;

    public StoreContextHandler(Properties config) {
        ServletFilterSpanDecorator renameSpanDecorator =
            new ServletFilterSpanDecorator() {
                @Override
                public void onRequest(HttpServletRequest httpServletRequest, Span span) {
                    span.setOperationName(httpServletRequest.getRequestURI());
                }

                @Override
                public void onResponse(HttpServletRequest httpServletRequest,
                    HttpServletResponse httpServletResponse, Span span) {}

                @Override
                public void onError(HttpServletRequest httpServletRequest,
                    HttpServletResponse httpServletResponse, Throwable throwable, Span span) {}

                @Override
                public void onTimeout(HttpServletRequest httpServletRequest,
                    HttpServletResponse httpServletResponse, long l, Span span) {}
            };
        
            TracingFilter tracingFilter = new TracingFilter(
            GlobalTracer.get(), Arrays.asList(renameSpanDecorator), null);
        addFilter(new FilterHolder(tracingFilter), "/*", EnumSet.allOf(DispatcherType.class));
        setContextPath("/store");
        servletRegistration();
    }

    void servletRegistration() {
        storeService = new StoreService();
        storeService.start();
        addServlet(new ServletHolder(new AddBobble(storeService)), "/add_bobble");
        addServlet(new ServletHolder(new CheckBobbles(storeService)), "/check_bobbles");
    }

    @SuppressWarnings("serial")
	static final class AddBobble extends HttpServlet {
        StoreService storeService;

        public AddBobble(StoreService storeService) {
            this.storeService = storeService;
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
            BobbleAddRequest addBobble = (BobbleAddRequest) Utility.readJSON(request, BobbleAddRequest.class);
            if (addBobble == null || addBobble.getOrderId() == null) {
                Utility.writeErrorResponse(response);
                return;
            }
            storeService.bobbleAddRequest(addBobble);
            response.setStatus(200);
        }
    }

    @SuppressWarnings("serial")
	static final class CheckBobbles extends HttpServlet {
        StoreService storeService;

        public CheckBobbles(StoreService storeService) {
            this.storeService = storeService;
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
            try {
                Utility.writeJSON(response, storeService.getBobbles());
            } catch (InterruptedException exc) {
            }
        }
    }
}