package mock;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class MyHttpServer {

    private InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), 8080);
    private HttpServer server;

    public MyHttpServer() throws IOException {        
        server = HttpServer.create(address,0);
        
        addOtherService();

        //server.createContext("/specific", new GetParamHandler(this));

        addDefaultService();

        server.start();        
    }

    public String toString() {
        return this.getClass().getSimpleName() + " " + address;
    }

    // SERVICES

    private void addDefaultService() {
        server.createContext("/", httpExchange -> {
            sendResponse(httpExchange, 403, "Not found");
        });
    }

    private void addOtherService() {
        HttpHandler handler = httpExchange -> {
            sendResponse(httpExchange, 200, printRequest(httpExchange));
        };
        server.createContext("/sms", handler);
    }

    // UTILS

    protected String printRequest(HttpExchange httpExchange) {

        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder
            .append("<html><body><b>Request</b><br/><pre>")                
            .append(httpExchange.getRequestMethod()).append(" ")
            .append(httpExchange.getRequestURI()).append(" ")
            .append(httpExchange.getProtocol()).append("\n");

        //httpExchange.getRequestHeaders().entrySet().forEach(entry -> {
        httpExchange.getRequestHeaders().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            htmlBuilder.append(entry.getKey()).append(": ");
            for(String tmp : entry.getValue()) {
                htmlBuilder.append(tmp);
            }
            htmlBuilder.append("\n");
        });

        List<String> ct = httpExchange.getRequestHeaders().get("Content-type");
        if(ct != null) {
            htmlBuilder.append(getRequestBody(httpExchange)).append("\n").append("\n");
        }

        htmlBuilder.append(getUrlEncodedVars(httpExchange.getRequestURI().getQuery()));

        htmlBuilder.append("</pre></body></html>");

        return htmlBuilder.toString();        
    }

    // TODO: verify non-escaping for splits

    protected static HashMap<String, String> getUrlEncodedVars(String query) {
        HashMap<String, String> qvars = new HashMap<>();

        if(query != null) {
            for(String var : query.split("&")) {
                String[] kv = var.split("=", 2);
                if(kv.length > 1) {
                    qvars.put(kv[0], kv[1]);
                } else  {
                    qvars.put(kv[0], "");
                }
            }
        }

        return qvars;
    }

    /*
        application/x-www-form-urlencoded
        multipart/form-data (boundary)
        text/plain
    */
    protected String getRequestBody(HttpExchange httpExchange) {
        String requestBody = "";
        try {
            InputStream inputStream = httpExchange.getRequestBody();
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            requestBody = result.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return requestBody;
    }

    protected void sendResponse(HttpExchange httpExchange, int code, String output) throws IOException {

        byte[] response = output.getBytes();
        httpExchange.sendResponseHeaders(code, response.length);

        OutputStream outputStream = httpExchange.getResponseBody();        
        outputStream.write(response);
        outputStream.flush();
        outputStream.close();
    }


    public static class GetParamHandler implements HttpHandler {
    
        private MyHttpServer server;
    
        public GetParamHandler(MyHttpServer server) {
            this.server = server;
        }
    
        @Override    
        public void handle(HttpExchange httpExchange) throws IOException {
            if("GET".equals(httpExchange.getRequestMethod())) {
                server.sendResponse(httpExchange, 200, "GET Operation");
            }
            server.sendResponse(httpExchange, 403, "Operation not found");
        }
    }

    // LAUNCHER

    public static void main(String args[]) {  
        try {
            MyHttpServer server = new MyHttpServer();
            System.out.printf("Serveur started : %s%n", server.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    } 

    /** DOCs
     * 
     * https://dzone.com/articles/simple-http-server-in-java
     * https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpServer.html
     * https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/POST
     * 
     */
}
