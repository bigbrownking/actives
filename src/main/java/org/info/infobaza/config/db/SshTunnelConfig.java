package org.info.infobaza.config.db;


import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SshTunnelConfig {

    private Session session;

    @PostConstruct
    public void init() {
        try {
            String sshHost = "10.10.30.4";
            String sshUser = "r.kazbayev";
            int sshPort = 22;
            String sshPassword = "EE~p(h_4=#=N+[yGb{";
            String remoteHost = "10.10.30.5";
            int remotePort = 5432;
            int localPort = 5433;

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

