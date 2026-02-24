package org.info.infobaza.config.db;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

@Slf4j
@Configuration
public class SshTunnelConfig {

    @Value("${tunnel.sshHost}")
    private String sshHost;

    @Value("${tunnel.sshUser}")
    private String sshUser;

    @Value("${tunnel.sshPassword.base64}")
    private String sshPasswordBase64;

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
        // Decode password from Base64
        String sshPassword = new String(Base64.getDecoder().decode(sshPasswordBase64));

        log.info("🔧 SSH Configuration:");
        log.info("   Host: {}:{}", sshHost, sshPort);
        log.info("   User: {}", sshUser);
        log.info("   Password decoded successfully: {} chars", sshPassword.length());
        log.info("   Tunnel: localhost:{} -> {}:{}", localPort, remoteHost, remotePort);

        try {
            JSch jsch = new JSch();
            JSch.setLogger(new JSchLogger());

            session = jsch.getSession(sshUser, sshHost, sshPort);
            session.setPassword(sshPassword);

            // SSH configuration
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "password,keyboard-interactive");
            session.setConfig("MaxAuthTries", "3");

            session.setTimeout(30000);
            session.setServerAliveInterval(60000);

            log.info("🔌 Connecting to SSH server...");
            session.connect(30000);

            if (!session.isConnected()) {
                throw new RuntimeException("SSH session not connected");
            }

            log.info("✅ SSH connection established!");

            int assignedPort = session.setPortForwardingL(localPort, remoteHost, remotePort);
            log.info("✅ SSH Tunnel Active: localhost:{} -> {}:{}",
                    assignedPort, remoteHost, remotePort);

        } catch (com.jcraft.jsch.JSchException e) {
            log.error("❌ SSH Authentication Failed!");
            log.error("   Error: {}", e.getMessage());
            log.error("   Host: {}@{}:{}", sshUser, sshHost, sshPort);
            throw new RuntimeException("Failed to establish SSH tunnel", e);
        } catch (Exception e) {
            log.error("❌ Unexpected error", e);
            throw new RuntimeException("Failed to establish SSH tunnel", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (session != null && session.isConnected()) {
            log.info("🔌 Closing SSH tunnel...");
            session.disconnect();
            log.info("✅ SSH tunnel closed");
        }
    }

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