import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;

import java.net.InetSocketAddress;
import java.nio.file.Path;

/**
 * Static file server for the Library Management System frontend prototype.
 * Serves the frontend/ folder on http://localhost:8000 using only the JDK.
 *
 * Run with:  java Server.java
 *
 * This stands in until the Spring Boot application exists; at that point the
 * pages move into src/main/resources and Spring Boot serves them instead.
 */
public class Server {
    public static void main(String[] args) throws Exception {
        Path root = Path.of("frontend").toAbsolutePath();
        HttpServer server = SimpleFileServer.createFileServer(
                new InetSocketAddress(8000), root, SimpleFileServer.OutputLevel.INFO);
        server.start();
        System.out.println("Library Management System frontend running at http://localhost:8000");
    }
}
