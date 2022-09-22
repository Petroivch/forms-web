import com.sun.net.httpserver.Headers;
import org.apache.http.*;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ThreadServer implements Runnable {
    private static Socket socket;

    public ThreadServer(Socket client) {
        ThreadServer.socket = client;
    }

    @Override
    public void run() {

        try (final var in = new BufferedInputStream(socket.getInputStream());
             final var out = new BufferedOutputStream(socket.getOutputStream())) {
            while (!socket.isClosed()) {
                final var limit = 4096;

                Request req = new Request(in, limit);

                if (req.isValid)
                {
                    System.out.println("Method: " + req.method);
                    System.out.println("Path: " + req.path);

                    System.out.println("All params in QueryString");
                    System.out.println(req.getQueryParams());

                    System.out.println("Get param from QueryString by name");
                    System.out.println("title");
                    System.out.println(req.getQueryParam("title"));
                    System.out.println("value");
                    System.out.println(req.getQueryParam("value"));

                    System.out.println("All params in POST");
                    System.out.println(req.getPOSTParams());

                    System.out.println("Get param from POST by name");
                    System.out.println("title");
                    System.out.println(req.getPOSTParam("title"));
                    System.out.println("value");
                    System.out.println(req.getPOSTParam("value"));


                    out.write(("""
                         HTTP/1.1 200 OK\r
                        Content-Length: 0\r
                        Connection: close\r
                        \r
                        """).getBytes());
                    out.flush();
                }
                /*else
                {
                    out.write((
                            """
                                    HTTP/1.1 400 Bad Request\r
                                    Content-Length: 0\r
                                    Connection: close\r
                                    \r
                                    """
                    ).getBytes());
                    out.flush();
                }*/

            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}