package demo.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterOutputStream;

@Component
public class ClientRunner implements ApplicationRunner {

    private final Logger log = LoggerFactory.getLogger(ClientRunner.class);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Socket socket = new Socket("localhost", 4943)) {
            OutputStream socketOutputStream = socket.getOutputStream();

            for (int i = 0; i < 1000; i++) {
                StringBuilder message = new StringBuilder("Hello ").append(i);
                writeMessage(socketOutputStream, message.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private void writeMessage(OutputStream socketOutputStream, byte[] message) {
        log.debug("Writing {}", message);
        try {
            DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(socketOutputStream);
            deflaterOutputStream.write(message);
            deflaterOutputStream.finish();
            deflaterOutputStream.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
