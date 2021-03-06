package com.bobble.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.Configuration.SenderConfiguration;
import io.jaegertracing.samplers.ConstSampler;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

public class BobbleApp {
    public static void main( String[] args )
        throws Exception {
        
    	Properties properties = loadConfig(args);
        if (!configureGlobalTracer(properties, "Bobbles"))
            throw new Exception("Could not configure the global tracer");

        ResourceHandler filesHandler = new ResourceHandler();
        filesHandler.setWelcomeFiles(new String[]{ "./index.html" });
        filesHandler.setResourceBase(properties.getProperty("public_directory"));

        ContextHandler fileCtxHandler = new ContextHandler();
        fileCtxHandler.setHandler(filesHandler);

        ContextHandlerCollection handlers = new ContextHandlerCollection();
        handlers.setHandlers(new Handler[] {
            fileCtxHandler,
            new ApiContextHandler(properties),
            new StoreContextHandler(properties),
        });
        
        Server server = new Server(10001);
        server.setHandler(handlers);
        server.start();
        server.dumpStdErr ();
        server.join();
    }

    static Properties loadConfig(String [] args)
        throws IOException {
        String file = "config.properties";
        if (args.length > 0)
            file = args[0];

        FileInputStream fs = new FileInputStream(file);
        Properties config = new Properties();
        config.load(fs);
        return config;
    }

    static boolean configureGlobalTracer(Properties config, String componentName)
        throws MalformedURLException {
        String tracerName = config.getProperty("tracer");
        Tracer tracer = null;
        if ("jaeger".equals(tracerName)) {
            SamplerConfiguration samplerConfig = new SamplerConfiguration()
                .withType(ConstSampler.TYPE)
                .withParam(1);
            SenderConfiguration senderConfig = new SenderConfiguration()
                .withAgentHost(config.getProperty("jaeger.reporter_host"))
                .withAgentPort(Integer.decode(config.getProperty("jaeger.reporter_port")));
            ReporterConfiguration reporterConfig = new ReporterConfiguration()
                .withLogSpans(true)
                .withFlushInterval(1000)
                .withMaxQueueSize(10000)
                .withSender(senderConfig);
            tracer = new Configuration(componentName).withSampler(samplerConfig).withReporter(reporterConfig).getTracer();
        } else {
            return false;
        }
        GlobalTracer.register(tracer);
        return true;
    }
}