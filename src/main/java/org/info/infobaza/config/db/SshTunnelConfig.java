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
        log.info("Connecting to SER...");
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(sshUser, sshHost, sshPort);
            session.setPassword(sshPassword);

            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            session.setPortForwardingL(localPort, remoteHost, remotePort);
            System.out.println("SSH tunnel established");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

