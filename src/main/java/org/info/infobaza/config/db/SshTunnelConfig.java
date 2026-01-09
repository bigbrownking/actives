package org.info.infobaza.config.db;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SshTunnelConfig {

    @Value("${tunnel.sshHost}")
    private String sshHost;

    @Value("${tunnel.sshUser}")
    private String sshUser;

    @Value("${tunnel.sshPassword}")
    private String sshPassword;

    @Value("${ser.host}")
    private String remoteHost;

    @Value("${tunnel.port}")
    private int sshPort;

    @Value("${remote.port}")
    private int remotePort;

    @Value("${local.port}")
    private int localPort;

    private Session session;

    @PostConstruct
    public void init() {
        log.info("Attempting SSH connection to {}@{}:{}", sshUser, sshHost, sshPort);
        log.info("Tunnel will forward localhost:{} -> {}:{}", localPort, remoteHost, remotePort);

        try {
            JSch jsch = new JSch();

            // Enable JSch logging for debugging
            JSch.setLogger(new JSchLogger());

            session = jsch.getSession(sshUser, sshHost, sshPort);
            session.setPassword(sshPassword);

            // SSH configuration
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "password"); // Try password first

            // Set timeout
            session.setTimeout(10000); // 10 seconds

            log.info("Connecting to SSH server...");
            session.connect();

            log.info("SSH connection established, setting up port forwarding...");
            session.setPortForwardingL(localPort, remoteHost, remotePort);

            log.info("✅ SSH tunnel established successfully: localhost:{} -> {}:{}",
                    localPort, remoteHost, remotePort);

        } catch (com.jcraft.jsch.JSchException e) {
            log.error("❌ SSH connection failed: {}", e.getMessage());
            log.error("Host: {}@{}:{}", sshUser, sshHost, sshPort);
            log.error("Check: 1) Credentials are correct, 2) SSH server allows password auth, 3) Network connectivity");
            throw new RuntimeException("Failed to establish SSH tunnel", e);
        } catch (Exception e) {
            log.error("❌ Unexpected error during SSH setup", e);
            throw new RuntimeException("Failed to establish SSH tunnel", e);
        }
    }

    // Custom JSch logger
    private static class JSchLogger implements com.jcraft.jsch.Logger {
        @Override
        public boolean isEnabled(int level) {
            return true;
        }

        @Override
        public void log(int level, String message) {
            switch (level) {
                case DEBUG:
                    log.debug("JSch: {}", message);
                    break;
                case INFO:
                    log.info("JSch: {}", message);
                    break;
                case WARN:
                    log.warn("JSch: {}", message);
                    break;
                case ERROR:
                case FATAL:
                    log.error("JSch: {}", message);
                    break;
            }
        }
    }
}