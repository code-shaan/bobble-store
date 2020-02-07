package com.bobble.api;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import com.bobble.api.resources.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import okhttp3.Connection;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import io.opentracing.Span;
import io.opentracing.contrib.okhttp3.TagWrapper;
import io.opentracing.contrib.okhttp3.TracingInterceptor;
import io.opentracing.contrib.okhttp3.OkHttpClientSpanDecorator;
import io.opentracing.util.GlobalTracer;

public class StoreConsumer {
    OkHttpClient client;
    MediaType jsonType;

    public StoreConsumer() {
        OkHttpClientSpanDecorator opNameDecorator = new OkHttpClientSpanDecorator() {
                @Override
                public void onRequest(Request request, Span span) {
                    span.setOperationName(request.url().encodedPath());
                }
                
                @Override
                public void onError(Throwable throwable, Span span) {}
                
                @Override
                public void onResponse(Connection connection, Response response, Span span) {}
            };
            
        TracingInterceptor tracingInterceptor = new TracingInterceptor(
                GlobalTracer.get(),
                Arrays.asList(OkHttpClientSpanDecorator.STANDARD_TAGS, opNameDecorator));
        client = new OkHttpClient.Builder()
                .addInterceptor(tracingInterceptor)
                .addNetworkInterceptor(tracingInterceptor)
                .build();

        jsonType = MediaType.parse("application/json");
    }

    public boolean addBobble(HttpServletRequest request, String orderId) {
        BobbleAddRequest bobbleRequest = new BobbleAddRequest(orderId);
        RequestBody body = RequestBody.create(jsonType, Utility.toJSON(bobbleRequest));

        Span parentSpan = (Span) request.getAttribute("span");
        Request req = new Request.Builder()
            .url("http://127.0.0.1:10001/store/add_bobble")
            .post(body)
            .tag(new TagWrapper(parentSpan.context()))
            .build();

        Response res = null;
        try {
            res = client.newCall(req).execute();
        } catch (IOException exc) {
            return false;
        } finally {}
        return res.code() >= 200 && res.code() < 300;
    }

    public Collection<Bobble> getBobbles(HttpServletRequest request)
    {
        Request req = new Request.Builder()
            .url("http://127.0.0.1:10001/store/check_bobbles")
            .build();

        String body = null;
        try {
            Response res = client.newCall(req).execute();
            if (res.code() < 200 || res.code() >= 300)
                return null;

            body = res.body().string();
        } catch (IOException exc) {
            return null;
        }

        Gson gson = new Gson();
        Type collType = new TypeToken<Collection<Bobble>>(){}.getType();
        return gson.fromJson(body, collType);
    }

    public StatusResponse checkStatus(HttpServletRequest request, String orderId) {
        Collection<Bobble> bobbles = getBobbles(request);
        if (bobbles == null)
            return null;

        ArrayList<Bobble> selected = new ArrayList<Bobble>();
        for (Bobble bobble: bobbles)
            if (bobble.getOrderId().equals(orderId))
                selected.add(bobble);

        Status status = Status.READY;
        int estimatedTime = 0;

        for (Bobble bobble: selected) {
            switch (bobble.getStatus()) {
                case NEW_ORDER:
                    estimatedTime += 3;
                    status = Status.NEW_ORDER;
                    break;
                case RECEIVED:
                    estimatedTime += 2;
                    status = Status.RECEIVED;
                    break;
                case PROCESSING:
                    estimatedTime += 1;
                    status = Status.PROCESSING;
                    break;
                default:
                    continue;
            }
        }
        return new StatusResponse(orderId, estimatedTime, status);
    }
}