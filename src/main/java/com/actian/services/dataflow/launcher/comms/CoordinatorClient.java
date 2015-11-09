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

import com.actian.services.dataflow.launcher.LaunchConfiguration;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.SocketFactory;

import com.actian.services.dataflow.launcher.LauncherService.State;

public class CoordinatorClient implements Runnable {

    private static final Logger LOG = Logger.getLogger(CoordinatorClient.class.getName());
    private Socket clientSocket;
    private int failureCount = 0;
    private final int port;
    private final String uri;
    private PrintWriter writer;
    private final Coordinator coordinator;
    private String lastResponse;

    public CoordinatorClient(String uri, int port, Coordinator coordinator) {
        this.uri = uri;
        this.port = port;
        this.coordinator = coordinator;
    }

    @Override
    public void run() {
        // Attempt to connect to primary.
        boolean guard = true;

        while (guard) {
            while (failureCount < LaunchConfiguration.getInstance().CONNECTION_RETRIES
                    && this.clientSocket == null) {
                try {
                    this.clientSocket = SocketFactory.getDefault()
                            .createSocket(uri, port);
                } catch (Exception e) {
                    // Could not connect
                    failureCount++;
                    LOG.log(Level.WARNING, "Could not connect to primary. Failures={0}", failureCount);
                }
                sleepUntilNextConnectionAttempt();
            }
            if (this.clientSocket == null) {
                if (coordinator.getLauncher().getState() == State.RESERVE) {
                    LOG.log(Level.INFO, "Service moving to responder mode.");
                    coordinator.getLauncher().setState(State.RESPONDER);
                    coordinator.broadcast("Service moving to responder mode.");
                    failureCount = 0;
                } else {
                    // Sleep until we try again.
                    failureCount = 0;
                    sleepUntilNextConnectionAttempt();
                }

            } else {

                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream()));
                    writer = new PrintWriter(
                            clientSocket.getOutputStream());
                    // We dont want verbose messages.
                    writer.println("BASIC");
                    while (clientSocket != null && clientSocket.isConnected()) {
                        // Send a status request
                        writer.println("STATUS");
                        writer.flush();
                        long msgTime = System.currentTimeMillis();

                        while (!reader.ready() && (System.currentTimeMillis() - msgTime) < 2000) {
                            // We haven't received a response......
                        }

                        // If we still have no data, then there is something
                        // wrong with the connection.
                        if (!reader.ready()) {
                            // Assume that we have lost connection. Attempt to reopen.
                            LOG.log(Level.WARNING, "Lost connection to primary.");
                            resetConnection();
                            failureCount = LaunchConfiguration.getInstance().CONNECTION_RETRIES;
                            continue;
                        }

                        while (reader.ready()) {
                            String resp = reader.readLine();
                            lastResponse = resp;
                            LOG.log(Level.FINE, "RESPONSE:{0}", resp);
                            // We are only interested in lines with results.
                            if (resp.contains(ReadEvalPrint.PROMPT_RESPONSE)) {
                                // Format of STATUS response is time|mode
                                resp = resp.split("\\|")[1];
                                if ("RESPONDER".equals(resp)) {
                                    // Primary has taken up responsibility.
                                    if (coordinator.getLauncher().getState() == State.RESPONDER) {
                                        LOG.log(Level.INFO, "Primary is responding. Reverting to failover mode.");
                                        coordinator.getLauncher().setState(State.RESERVE);
                                        coordinator.broadcast("Service now in reserve mode.");
                                    }

                                } else {
                                    if (coordinator.getLauncher().getState() != State.RESPONDER) {
                                        // Take over responsibility.
                                        LOG.log(Level.INFO, "Primary is not in state RESPONDER. Will takeover.");
                                        coordinator.getLauncher().setState(State.RESPONDER);
                                        coordinator.broadcast("Service now in responder mode."); }
                                }
                                break;
                            }
                        }
                        // Sleep
                        Thread.sleep(LaunchConfiguration.getInstance().CONNECTION_PULSE_MS);
                    }
                    resetConnection();
                } catch (IOException | InterruptedException e) {
                    LOG.log(Level.WARNING, "Lost connection.", e);
                    resetConnection();
                    failureCount = LaunchConfiguration.getInstance().CONNECTION_RETRIES;
                }
            }
        }
    }

    String getStatus() {
        return (clientSocket != null && clientSocket.isConnected())
                ? "Good:<" + lastResponse + ">" : "Bad";

    }

    private void resetConnection() {
        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Reset connection failure.", e);
            } finally {
                clientSocket = null;
            }
        }
    }

    private void sleepUntilNextConnectionAttempt() {
        try {
            Thread.sleep(LaunchConfiguration.getInstance().CONNECTION_REATTACH_MS);
        } catch (InterruptedException e) {
            LOG.log(Level.WARNING, "Interrupted.", e);
        }
    }

}
