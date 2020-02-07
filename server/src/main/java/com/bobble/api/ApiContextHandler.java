package com.bobble.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.bobble.api.StoreConsumer;
import com.bobble.api.Utility;
import com.bobble.api.resources.BobbleRequest;
import com.bobble.api.resources.StatusRequest;
import com.bobble.api.resources.StatusResponse;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;

public class ApiContextHandler extends ServletContextHandler {
    Properties properties;
    StoreConsumer storeConsumer;

    public ApiContextHandler(Properties config) {
        this.properties = config;
        servletRegistration();
    }

    void servletRegistration() {
        storeConsumer = new StoreConsumer();
        addServlet(new ServletHolder(new Order(storeConsumer)), "/order");
        addServlet(new ServletHolder(new Status(storeConsumer)), "/status");
        addServlet(new ServletHolder(new Config(properties)), "/config.js");
    }

    @SuppressWarnings("serial")
	static final class Order extends HttpServlet {
        StoreConsumer storeConsumer;
        
        public Order(StoreConsumer storeConsumer) {
            this.storeConsumer = storeConsumer;
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
            try (Scope scope = GlobalTracer.get().buildSpan("order_span").startActive(true)) {
                request.setAttribute("span", scope.span());

                BobbleRequest[] bobbleInfo = parseBobbleInfo(request);
                if (bobbleInfo == null) {
                    Utility.writeErrorResponse(response);
                    return;
                }

                String orderId = UUID.randomUUID().toString();
                for (BobbleRequest bobbleRequest : bobbleInfo)
                    for (int i = 0; i < bobbleRequest.getQuantity(); i++)
                        if (!storeConsumer.addBobble(request, orderId)) {
                            Utility.writeErrorResponse(response);
                            return;
                        }
                StatusResponse statusRes = storeConsumer.checkStatus(request, orderId);
                if (statusRes == null) {
                    Utility.writeErrorResponse(response);
                    return;
                }
                Utility.writeJSON(response, statusRes);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        static BobbleRequest[] parseBobbleInfo(HttpServletRequest request)
            throws IOException {
            JsonObject jsonObj = Utility.readJSONObject(request);
            JsonArray bobbles = jsonObj.getAsJsonArray("bobbles");
            if (bobbles == null || bobbles.size() == 0)
                return null;

            Gson gson = new Gson();
            BobbleRequest[] bobblesInfo = new BobbleRequest[bobbles.size()];
            for (int i = 0; i < bobbles.size(); i++) {
                JsonObject bobble = (JsonObject) bobbles.get(i);
                String character = gson.fromJson(bobble.get("character"), String.class);
                int quantity = gson.fromJson(bobble.get("quantity"), int.class);
                bobblesInfo[i] = new BobbleRequest(character, quantity);
            }
            return bobblesInfo;
        }
    }

    @SuppressWarnings("serial")
	static final class Status extends HttpServlet {
        StoreConsumer storeConsumer;

        public Status(StoreConsumer storeConsumer) {
            this.storeConsumer = storeConsumer;
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
            StatusRequest statusRequest = (StatusRequest) Utility.readJSON(request, StatusRequest.class);
            if (statusRequest == null) {
                Utility.writeErrorResponse(response);
                return;
            }
            StatusResponse statusResponse = storeConsumer.checkStatus(request, statusRequest.getOrderId());
            Utility.writeJSON(response, statusResponse);
        }
    }

    @SuppressWarnings("serial")
	static final class Config extends HttpServlet {
        Properties properties;

        public Config(Properties properties) {
            this.properties = properties;
        }

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
            PrintWriter writer = response.getWriter();
            writer.println(createConfigBody());
            writer.close();
        }

        String createConfigBody() {
            String body = ""
                + "var Config = {"
                + "    tracer: \"%s\","
                + "    tracer_host: \"%s\","
                + "    tracer_port: %s,"
                + "    tracer_access_token: \"%s\","
                + "}";

            return String.format(body,
                    properties.getProperty("tracer"),
                    properties.getProperty("tracer_host"),
                    properties.getProperty("tracer_port"),
                    properties.getProperty("tracer_access_token"));
        }
    }
}