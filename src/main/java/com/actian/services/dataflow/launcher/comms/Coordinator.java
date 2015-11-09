/*
   Copyright 2015 Actian Corporation
 
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
 
     http://www.apache.org/licenses/LICENSE-2.0
 
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.actian.services.dataflow.launcher.comms;

import com.actian.services.dataflow.launcher.LauncherService;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.Collections;

public class Coordinator implements Runnable, CoordinatorService {
    private static final Logger LOG = Logger.getLogger(Coordinator.class.getName());
    private static final int MAX_CLIENTS = 3;
    private int port;
    private final List<Future<Long>> clients = new ArrayList<>(MAX_CLIENTS);
    private final List<CoordinatorListener> listeners = new ArrayList<>(MAX_CLIENTS);
    private boolean openForConnections = true;
    private Thread coord;
    private LauncherService launcher;
    private CoordinatorClient heartbeat;
    private String primaryURI;
    private int primaryPort;

    private Coordinator() {
    }

    public Coordinator(LauncherService launcher, String primaryURI, int primaryPort) {
        this.launcher = launcher;
        this.port = launcher.getPort();
        this.primaryURI = primaryURI;
        this.primaryPort = primaryPort;
    }

    @Override
    public void run() {
        ExecutorService service = Executors.newFixedThreadPool(MAX_CLIENTS+1);
        ServerSocket serverSocket = null;

        try {
            // Open up a communication channel.
            serverSocket = new ServerSocket(port);

            // Wait for a connection to us.
            while (openForConnections) {
                removeFinished();
                // Await a connection
                Socket clientSocket = serverSocket.accept();
                CoordinatorListener listener = new CoordinatorListener(clientSocket, this);
                if (clients.size() == MAX_CLIENTS) {
                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
                    writer.println("Too many connections. Closing connection.");
                    writer.flush();
                    clientSocket.close();
                } else {
                    clients.add(service.submit(listener));
                    listeners.add(listener);
                }
            }

        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } finally {
            if (null != serverSocket) {
                try {
                    serverSocket.close();
                } catch (IOException ex) {
                    Logger.getLogger(Coordinator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            service.shutdown();
        }
    }

    @Override
    public void shutdown() {
        for (CoordinatorListener client : listeners) {
            client.sendMessage("Shutdown event recieved.");
            client.shutdown();
        }
        removeFinished();
        openForConnections = false;
    }

    String getHeartbeatStatus() {
        return (heartbeat != null) ? "Heartbeat: " + heartbeat.getStatus() :
                                     "No heartbeat running.";
    }

    public void removeFinished() {
        // Clean up any finished threads
        List finished = new ArrayList<>();
        for (Future<Long> c : clients) {
            if (c.isDone()) {
                finished.add(c);
                LOG.log(Level.FINE,"Found a closed client.");
            }
        }
        
        clients.removeAll(finished);
        List closedClients = new ArrayList<>();
        for (CoordinatorListener l : listeners) {
            if (l.finished()) {
                //l.shutdown();
                closedClients.add(l);
            }
        }
        listeners.removeAll(closedClients);
    }

    @Override
    public void broadcast(String msg) {
        for (CoordinatorListener client : listeners) {
            client.sendMessage(msg);
        }
    }

    List<CoordinatorListener> getSessionList() {
        return Collections.unmodifiableList(listeners);
    }

    @Override
    public void start() {
        if (coord == null) { 
            LOG.log(Level.INFO, "Starting coordinator thread for port {0,number,#}", this.port);
            coord = new Thread(this,"Coordinator");
            coord.start();
        }
        // If we are the secondary machine, we need a connection to the primary.
        if (heartbeat == null && !"localhost".equals(primaryURI)) {
            heartbeat = new CoordinatorClient(primaryURI,primaryPort, this);
            Executors.newSingleThreadExecutor().submit(heartbeat);
        }
    }

    public LauncherService getLauncher() {
        return launcher;
    }

}
