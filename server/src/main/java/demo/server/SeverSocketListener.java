package demo.server;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SeverSocketListener {

    private final Logger log = LoggerFactory.getLogger(SeverSocketListener.class);

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private final ExecutorService executorService = Executors.newWorkStealingPool();
    private AtomicBoolean shutdown = new AtomicBoolean(false);
    private final MessageConsumer toteEventConsumer;

    public SeverSocketListener(MessageConsumer toteEventConsumer) {
        this.toteEventConsumer = toteEventConsumer;
    }

    @PostConstruct
    public void start() {
        try {
            serverSocket = new ServerSocket(4943);
            serverSocket.setSoTimeout(1000);
            executorService.submit(() -> {
                while (!shutdown.get()) {
                    try {
                        clientSocket = serverSocket.accept();
                        executorService.submit(() -> new MessageListener(clientSocket, toteEventConsumer).listen());
                    } catch (SocketTimeoutException _) {
                    } catch (IOException e) {
                        log.warn("IOException in socket accept", e);
                    }
                }
            });
        } catch (IOException e) {
            log.warn("IOException in server socket listener", e);
        }
    }

    @PreDestroy
    public void stop() {
        shutdown.set(true);
        try {
            executorService.shutdownNow();
            if (clientSocket != null) {
                clientSocket.close();
            }
            serverSocket.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
