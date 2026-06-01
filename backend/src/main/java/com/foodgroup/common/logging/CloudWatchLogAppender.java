package com.foodgroup.common.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.logstash.logback.encoder.LogstashEncoder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
@ConditionalOnProperty(name = "aws.cloudwatch.enabled", havingValue = "true")
public class CloudWatchLogAppender extends AppenderBase<ILoggingEvent> {

    private static final int BATCH_SIZE = 100;
    private static final long FLUSH_INTERVAL_SECONDS = 5;

    private final CloudWatchLogsClient cloudWatchLogsClient;

    @Value("${aws.cloudwatch.log-group:/foodgroup/backend}")
    private String logGroup;

    @Value("${aws.cloudwatch.log-stream:app}")
    private String logStream;

    private final List<InputLogEvent> buffer = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private LogstashEncoder encoder;
    private ScheduledExecutorService scheduler;

    public CloudWatchLogAppender(CloudWatchLogsClient cloudWatchLogsClient) {
        this.cloudWatchLogsClient = cloudWatchLogsClient;
    }

    @PostConstruct
    public void init() {
        ensureLogGroupExists();
        ensureLogStreamExists();

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        setContext(loggerContext);
        setName("CLOUDWATCH");

        encoder = new LogstashEncoder();
        encoder.setContext(loggerContext);
        encoder.start();

        start();

        // Register this appender on the root logger so all log events flow through it
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(this);

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cloudwatch-log-flusher");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::flush, FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        flush();

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.detachAppender(this);

        stop();
        if (encoder != null) {
            encoder.stop();
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        try {
            byte[] encoded = encoder.encode(event);
            String message = new String(encoded, StandardCharsets.UTF_8).trim();

            InputLogEvent logEvent = InputLogEvent.builder()
                    .timestamp(event.getTimeStamp())
                    .message(message)
                    .build();

            lock.lock();
            try {
                buffer.add(logEvent);
                if (buffer.size() >= BATCH_SIZE) {
                    flushUnderLock();
                }
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            System.err.println("[CloudWatchLogAppender] Failed to append log event: " + e.getMessage());
        }
    }

    private void flush() {
        lock.lock();
        try {
            flushUnderLock();
        } finally {
            lock.unlock();
        }
    }

    private void flushUnderLock() {
        if (buffer.isEmpty()) {
            return;
        }

        List<InputLogEvent> batch = new ArrayList<>(buffer);
        buffer.clear();

        try {
            // CloudWatch Logs API requires events sorted by timestamp
            batch.sort((a, b) -> Long.compare(a.timestamp(), b.timestamp()));

            PutLogEventsRequest request = PutLogEventsRequest.builder()
                    .logGroupName(logGroup)
                    .logStreamName(logStream)
                    .logEvents(batch)
                    .build();

            cloudWatchLogsClient.putLogEvents(request);
        } catch (Exception e) {
            System.err.println("[CloudWatchLogAppender] Failed to send " + batch.size()
                    + " log events to CloudWatch: " + e.getMessage());
        }
    }

    private void ensureLogGroupExists() {
        try {
            cloudWatchLogsClient.createLogGroup(
                    CreateLogGroupRequest.builder()
                            .logGroupName(logGroup)
                            .build()
            );
        } catch (ResourceAlreadyExistsException ignored) {
            // already exists — this is fine
        } catch (Exception e) {
            System.err.println("[CloudWatchLogAppender] Could not ensure log group exists: " + e.getMessage());
        }
    }

    private void ensureLogStreamExists() {
        try {
            cloudWatchLogsClient.createLogStream(
                    CreateLogStreamRequest.builder()
                            .logGroupName(logGroup)
                            .logStreamName(logStream)
                            .build()
            );
        } catch (ResourceAlreadyExistsException ignored) {
            // already exists — this is fine
        } catch (Exception e) {
            System.err.println("[CloudWatchLogAppender] Could not ensure log stream exists: " + e.getMessage());
        }
    }
}
