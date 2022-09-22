import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executors;

public class Server{
    private static final int backlogSize = 50;

    public static void listen(int port){
        final var threadPool = Executors.newFixedThreadPool(64);
        try (final var serverSocket = new ServerSocket(port, backlogSize)) {

            while (!serverSocket.isClosed()) {
                final var socket = serverSocket.accept();
                threadPool.execute(new ThreadServer(socket));
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Server has stopped");
        }
    }
}