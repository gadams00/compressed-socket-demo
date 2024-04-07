package demo.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingMessageConsumer implements MessageConsumer {

    private final Logger log = LoggerFactory.getLogger(LoggingMessageConsumer.class);

    @Override
    public void onMessage(String message) {
        log.info("message: {}", message);
    }
}
