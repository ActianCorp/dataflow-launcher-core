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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.actian.services.dataflow.launcher.comms.ReadEvalPrint.Command;
import java.util.List;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class CoordinatorListener implements Callable<Long> {

    private static final String VERSION = "0.1";
    private final Socket clientSocket;
    private final Coordinator coordinator;
    private PrintWriter writer;
    private ANSILogHandler logHandler;
    private long lastCommandTime;
    private Properties props;

    public CoordinatorListener(Socket clientSocket, Coordinator coordinator) {
        this.clientSocket = clientSocket;
        this.coordinator = coordinator;
        this.lastCommandTime = System.currentTimeMillis();
        this.props = new Properties();
    }

    @Override
    public Long call() {
        boolean showPrompt = true;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream());
            writer.println("\nDF Launch Service (v" + VERSION + ") - " + (coordinator.getLauncher().getType()));
            writer.flush();
            while (!clientSocket.isClosed()) {
                if (logHandler != null) {
                    logHandler.flush();
                }
                if (showPrompt) {
                    ReadEvalPrint.writePrompt(writer);
                }
                String command = reader.readLine();
                ReadEvalPrint p = new ReadEvalPrint(command,writer,coordinator.getLauncher(),props);
                if (p.getCommandType() == Command.QUIT) {
                    clientSocket.close();
                } else if (p.getCommandType() == Command.LOG) {
                    if ("ON".equals(p.getArgument())) {
                        logHandler = new ANSILogHandler(writer);
                        Logger.getLogger("").addHandler(logHandler);
                    } else {
                        if (logHandler != null ) {
                            Logger.getLogger("").removeHandler(logHandler);
                            logHandler = null;
                        }
                    }
                } else if (p.getCommandType() == Command.BASIC) {
                    // No more command prompts.
                    showPrompt = false;
                } else if (p.getCommandType() == Command.SESSIONS) {
                    List<CoordinatorListener> sessions = this.coordinator.getSessionList();
                    for (CoordinatorListener sess : sessions) {
                        writer.append(ReadEvalPrint.PROMPT_RESPONSE).append(sess.clientSocket.getRemoteSocketAddress().toString());
                        writer.println();
                    }
                    /* Display heartbeat info */
                    writer.append(ReadEvalPrint.PROMPT_RESPONSE).append(coordinator.getHeartbeatStatus());
                    writer.println();
                    writer.flush();
                }
                lastCommandTime = System.currentTimeMillis();
            }
        } catch (IOException e) {
            Logger.getLogger(CoordinatorListener.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            writer.println("Closing connection...");
            writer.flush();
            shutdown();
        }
        return 0L;
    }

    public void sendMessage(String msg) {
        if (writer != null) {
            writer.print(ANSILogHandler.REVERSE);
            writer.print(msg);
            writer.println(ANSILogHandler.RESET);
            writer.flush();
        }
    }

    public boolean finished() {
        return this.clientSocket.isClosed();
    }

    void shutdown() {
        if (logHandler != null) {
            Logger.getLogger("").removeHandler(logHandler);
            logHandler = null;
        }
        try {
            clientSocket.close();
        } catch (IOException ex) {
        }
    }

    static class ANSILogHandler extends Handler {

        PrintWriter pw;
        SimpleFormatter formatter;
        public static final String RED = "\u001B[31m";
        public static final String YELLOW = "\u001B[33m";
        public static final String CYAN = "\u001B[36m";
        public static final String RESET = "\u001B[0m";
        public static final String REVERSE = "\u001B[7m";

        public ANSILogHandler(PrintWriter writer) {
            pw = writer;
            formatter = new SimpleFormatter();
            setLevel(Level.ALL);
        }

        @Override
        public void publish(LogRecord record) {
            pw.append(ReadEvalPrint.BROADCAST_PREFIX);
            switch (record.getLevel().getName()) {
                case "SEVERE": 
                    pw.append(RED);
                    break;
                case "WARNING":
                    pw.append(YELLOW);
                    break;
                case "INFO":
                    pw.append(CYAN);
                    break;
            }

            pw.append(formatter.format(record)).append(RESET);
            pw.flush();
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }

}
