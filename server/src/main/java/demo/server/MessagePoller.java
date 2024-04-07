package demo.server;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

public class MessagePoller {

    private final BlockingQueue<String> messageQueue;
    private final MessageConsumer messageConsumer;
    private final Consumer<Throwable> pollErrorHandler;
    private boolean stop = false;

    public MessagePoller(BlockingQueue<String> messageQueue, MessageConsumer messageConsumer, Consumer<Throwable> pollErrorHandler) {
        this.messageQueue = messageQueue;
        this.messageConsumer = messageConsumer;
        this.pollErrorHandler = pollErrorHandler;
    }

    public void shutdown() {
        this.stop = true;
    }

    public void pollMessageQueue() {
        try {
            while (!stop) {
                try {
                    messageConsumer.onMessage(messageQueue.take());
                } catch (InterruptedException _) {
                }
            }
            // we've been stopped, drain queue
            String message;
            while ((message = messageQueue.poll()) != null) {
                messageConsumer.onMessage(message);
            }
        } catch (Throwable t) {
            pollErrorHandler.accept(t);
        }
    }
}
