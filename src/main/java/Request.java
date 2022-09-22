import com.sun.net.httpserver.Headers;
import org.apache.http.*;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class Request {
    public final String GET = "GET";
    public final String POST = "POST";

    public boolean isValid;
    public String path;
    public String method;
    public Map<String, List<String>> mapQuery = new HashMap<>();
    public Map<String, List<String>> mapQueryPost = new HashMap<>();
    public String bodyPost;

    int headersStart;
    int headersEnd;
    final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};

    public Request(BufferedInputStream in, int limit) throws IOException {
        in.mark(limit);
        byte[] buffer = new byte[limit];
        int read = in.read(buffer);

        isValid = true;

        final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png",
                "/resources.html", "/styles.css", "/app.js", "/links.html",
                "/forms.html", "/classic.html", "/events.html", "/events.js");
        final var allowedMethods = List.of(GET, POST);

        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);

        if (requestLineEnd == -1) { // null?
            isValid = false;
        } // impossible to work longer without requestLine
        else {

            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" "); //
            if (requestLine.length != 3) {
                isValid = false;
            } // impossible to work with bad request
            else {
                method = requestLine[0];
                if (!allowedMethods.contains(method)) {
                    isValid = false;
                }

                //System.out.println(method);

                path = requestLine[1];
                if (!path.startsWith("/") & !validPaths.contains(path)) {
                    isValid = false;
                }
                //System.out.println(path);

                headersStart = requestLineEnd + requestLineDelimiter.length;
                headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
                if (headersEnd == -1) {
                    isValid = false;
                }

                if (isValid) {

                    //Fill structure for GET method
                    if (method.equals(GET)) {
                        if (path.contains("?")) {
                            String queryString = getQueryString(path);
                            mapQuery = fillQueryMap(queryString);
                        }
                    }

                    //Fill structure for POST method
                    if (!method.equals(GET)) {
                        in.reset();
                        in.skip(headersStart);

                        final var headersBytes = in.readNBytes(headersEnd - headersStart);
                        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));

                        System.out.println(headers);

                        //System.out.println("Below POST method content from req: ");
                        in.skip(headersDelimiter.length);
                        final var contentLength = extractHeader(headers);
                        if (contentLength.isPresent()) {
                            final var length = Integer.parseInt(contentLength.get());
                            final var bodyBytes = in.readNBytes(length);

                            final var body = new String(bodyBytes);
                            //System.out.println(body);

                            //get boundary value
                            final String boundary = extractHeaderBoundary(headers);

                            final var variables = body.replace("\r\n\r\n", "\r\n").split("\r\n");

                            String namePOST = "";
                            String valuePOST = "";
                            for (int i = 0; i < variables.length; i++){
                                if (variables[i].contains(boundary)){
                                    if (i < variables.length-1){
                                        if (variables[i+1].contains("filename=")) {
                                            namePOST = variables[i+1].substring(variables[i+1].indexOf("\"")+1, variables[i+1].indexOf("; filename=")-1);
                                            valuePOST = variables[i+1].substring(variables[i+1].indexOf("filename=")); // - file name
                                            //valuePOST = variables[i+3]; // - file content
                                        }
                                        else {
                                            namePOST = variables[i+1].substring(variables[i+1].indexOf("\"")+1,variables[i+1].lastIndexOf("\""));
                                            valuePOST = variables[i+2];
                                        }
                                        if (!mapQueryPost.containsKey(namePOST)) mapQueryPost.put(namePOST, new ArrayList<>());
                                        mapQueryPost.get(namePOST).add(valuePOST);
                                    }
                                }
                            }
                            //System.out.println("POSTS: " + mapQueryPost);
                        }

                    }
                }

            }
        }

    }

    private Map<String, List<String>> fillQueryMap(String queryString) {
        Map<String, List<String>> params = new HashMap<>();
        var param = URLEncodedUtils.parse(queryString, StandardCharsets.UTF_8);
        for (NameValuePair nameValuePair : param) {
            String name = nameValuePair.getName();
            String value = nameValuePair.getValue();
            if (!params.containsKey(name)) params.put(name, new ArrayList<>());
            params.get(name).add(value);
        }
        return params;
    }


    public String getQueryParams() {
        String res = "";

        for (Map.Entry<String, List<String>> entry : mapQuery.entrySet()) {
            res += "\r\n" + entry.getKey()  + " : ";
            for (String s : entry.getValue()) {
                res += "\r\n" + s;
            }
        }

        return res;
    }

    public String getQueryParam(String keyName) {
        String res = "";
        for (Map.Entry<String, List<String>> entry : mapQuery.entrySet()) {
            if (entry.getKey() == keyName){
                    res += "\r\n" + keyName + " : " + entry.getValue();
            }
        }
        return res;
    }

    public String getPOSTParams() {
        String res = "";

        for (Map.Entry<String, List<String>> entry : mapQueryPost.entrySet()) {
            res += "\r\n" + entry.getKey()  + " : ";
            for (String s : entry.getValue()) {
                res += "\r\n" + s;
            }
        }

        return res;
    }

    public String getPOSTParam(String keyName) {
        String res = "";
        for (Map.Entry<String, List<String>> entry : mapQueryPost.entrySet()) {
            if (entry.getKey() == keyName){
                res += "\r\n" + keyName + " : " + entry.getValue();
            }
        }
        return res;
    }

    private String getQueryString(String path) {
        int queryParamsStart = path.indexOf('?');
        return path.substring(queryParamsStart + 1);
    }

    private static Optional<String> extractHeader(List<String> headers) {
        return headers.stream()
                .filter(o -> o.startsWith("Content-Length"))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static String extractHeaderBoundary(List<String> headers) {
        return headers.stream()
                .filter(o -> o.startsWith("Content-Type"))
                .map(o -> o.substring(o.indexOf("=")+1))
                .map(String::trim)
                .findFirst()
                .get()
                .replace("-", "");
    }

    private static int indexOf(byte[] buffer, byte[] delimiter, int start, int max) {
        outer:
        for (int i = start; i < max - delimiter.length + 1; i++) {
            for (int j = 0; j < delimiter.length; j++) {
                if (buffer[i + j] != delimiter[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
