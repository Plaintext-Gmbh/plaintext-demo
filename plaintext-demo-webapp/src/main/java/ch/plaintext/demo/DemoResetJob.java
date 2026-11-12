package ch.plaintext.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Ends the demo at {@code demo.reset.cron} (off by default, "-") with exit code 75 after leaving
 * the reset marker. Only meaningful where something restarts the process — the container on
 * demo.plaintext.ch ({@code restart: unless-stopped}).
 */
@Component
public class DemoResetJob {

    private static final Logger log = LoggerFactory.getLogger(DemoResetJob.class);

    static final int EXIT_RESTART = 75;

    private final ApplicationContext context;

    public DemoResetJob(ApplicationContext context) {
        this.context = context;
    }

    @Scheduled(cron = "${demo.reset.cron:-}", zone = "Europe/Zurich")
    public void reset() throws IOException {
        DemoReset.request(DemoReset.dataDir());
        log.warn("plaintext-demo: nightly reset - restarting with an empty database");
        System.exit(SpringApplication.exit(context, () -> EXIT_RESTART));
    }
}
