package demo.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class MessageListener {

    private final Logger log = LoggerFactory.getLogger(MessageListener.class);
    private final Socket clientSocket;

    private final ExecutorService messagePollerExecutorService = Executors.newWorkStealingPool();
    private BlockingQueue<String> messageQueue;
    private MessagePoller messagePoller;
    private final HexFormat hexFormat = HexFormat.of();

    public MessageListener(Socket clientSocket, MessageConsumer messageConsumer) {
        this.clientSocket = clientSocket;
        this.messageQueue = new LinkedBlockingQueue<>();
        this.messagePoller = new MessagePoller(messageQueue, messageConsumer, this::handlePollerError);
        messagePollerExecutorService.submit(() -> messagePoller.pollMessageQueue());
    }

//    public void listen() {
//        while (true) {
//            try {
//                // Do not close inputStream, including via try with resources. It will close the socket, which we don't want.
//                InputStream inputStream = new InflaterInputStream(clientSocket.getInputStream());
//                try (ByteArrayOutputStream readOutputStream = new ByteArrayOutputStream()) {
//                    byte[] readBuff = new byte[1024];
//                    int readCount;
//                    int offset = 0;
//                    while ((readCount = inputStream.read(readBuff)) != -1) {
//                        log.debug("writing to stream, offset: {}, readCount: {}", offset, readCount);
//                        readOutputStream.write(readBuff, offset, readCount);
//                        offset += readCount;
//                    }
//                    handleMessage(readOutputStream.toByteArray());
//                }
//            } catch (IOException e) {
//                log.error("SiteReader error", e);
//            }
//        }
//    }

    public void listen() {
        try {
            InputStream inputStream = clientSocket.getInputStream();

            byte[] readBuff = new byte[1024];
            ByteArrayOutputStream readOutputStream = new ByteArrayOutputStream();
            Inflater inflater = new Inflater();
            byte[] inflateBuff = new byte[1024];
            int readCount;
            int inflateCount = 0;
            while ((readCount = inputStream.read(readBuff)) != -1) {
                log.debug("readCount: {}", readCount);
                readOutputStream.write(readBuff, 0, readCount);
                log.debug("readOutputStream byte array: {}", hexFormat.formatHex(readOutputStream.toByteArray()));
                inflater.setInput(readOutputStream.toByteArray());
                while (inflater.getRemaining() > 0 && !inflater.finished() && !inflater.needsInput()) {
                    log.debug("inflater not finished and doesn't needs input, calling inflater.inflate");
                    inflateCount += inflater.inflate(inflateBuff);
                }
                if (inflater.finished()) {
                    log.debug("inflater finished, handling message");
                    handleMessage(Arrays.copyOfRange(inflateBuff, 0, inflateCount));
                    inflater.reset();
                    inflateCount = 0;
                    readOutputStream.close();
                    readOutputStream = new ByteArrayOutputStream();
                }
            }
        } catch (IOException | DataFormatException e) {
            log.error("SiteReader error", e);
            throw new RuntimeException(e);
        }
    }

    private void handleMessage(byte[] messageBytes) {
        String message = new String(messageBytes, StandardCharsets.UTF_8);
        messageQueue.offer(message);
    }

    void handlePollerError(Throwable t) {
        log.info("Event queue poller threw an exception, disconnecting.", t);
        try {
            clientSocket.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
