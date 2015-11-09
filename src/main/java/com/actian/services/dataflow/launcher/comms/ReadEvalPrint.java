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
import com.actian.services.dataflow.launcher.LauncherService;
import com.actian.services.dataflow.launcher.LauncherService.State;
import java.io.PrintWriter;
import java.sql.Timestamp;

import com.actian.services.dataflow.launcher.MasterStatsMBean;
import com.actian.services.dataflow.launcher.job.JobResult;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public final class ReadEvalPrint {

    public enum Command {

        UNKNOWN, STATUS, QUIT, MODE, STATS, HELP, LOG, BASIC, SESSIONS, EXECUTE, LOAD, CONFIG
    }
    public static final String PROMPT = ">";
    public static final String PROMPT_RESPONSE = "...\t";
    public static final String BROADCAST_PREFIX = "+  ";
    private Command command;
    private String response;
    private String argument;
    private String argument2;

    public ReadEvalPrint(String commandline, PrintWriter writer, LauncherService launcher, Properties clientprops) {
        commandline = processBinary(commandline);

        String[] args = commandline.split(" ", 3);
        argument = args.length > 1 ? args[1] : "";
        argument2 = args.length > 2 ? args[2] : "";

        // Is this overridden?
        this.response = launcher.processCommand(args,clientprops);
        if (this.response != null) {
            writeResponse(writer);
            return;
        }

        switch (args[0]) {
            case "BASIC":
                this.command = Command.BASIC;
                this.response = "";
                break;
            case "LOG":
                this.command = Command.LOG;
                if (argument != null && argument.equals("ON")) {
                    this.response = "Log listening on.";
                } else if (argument != null && argument.equals("OFF")) {
                    this.response = "Log listening off.";
                } else {
                    this.response = "Please state ON or OFF.";
                }
                break;
            case "MODE":
                this.command = Command.MODE;
                try {
                    launcher.setState(State.valueOf(args[1]));
                    this.response = new Timestamp(System.currentTimeMillis()) + "|State set to " + launcher.getState().name();
                } catch (Exception ex) {
                    this.response = "Unknown state";
                }
                break;
            case "QUIT":
                this.command = Command.QUIT;
                this.response = "Closing connection.";
                break;
            case "SESSIONS":
                this.command = Command.SESSIONS;
                this.response = "Listing sessions:";
                break;
            case "STATUS":
                this.command = Command.STATUS;
                this.response = new Timestamp(System.currentTimeMillis())
                        + "|" + launcher.getState().name();
                break;
            case "STATS":
                this.command = Command.STATS;
                try {
                    ObjectName name = new ObjectName("com.actian.dataflow.launcher:name=Stats");
                    MasterStatsMBean stats = JMX.newMBeanProxy(ManagementFactory.getPlatformMBeanServer(), name, MasterStatsMBean.class);
                    this.response = stats.summary();

                } catch (MalformedObjectNameException ex) {
                    this.response = "No stats available.";
                } catch (Exception ex) {
                    this.response = "No JMX service running.[" + ex.getMessage() + "]";
                }
                break;
            case "HELP":
                this.command = Command.HELP;
                this.response = "LOG [ON|OFF] - sends any log messages to the display.\n"
                        + "MODE state   - puts the server into passed state.\n"
                        + "STATS        - display job statistics.\n"
                        + "STATUS       - reports state of service.\n"
                        + "QUIT         - closes this connection.\n";
                break;
            case "EXECUTE":
                this.command = Command.EXECUTE;
                if (argument.length() == 0) {
                    this.response = "Must supply a filename with no spaces";
                    this.command = Command.UNKNOWN;
                } else {
                    Properties props = new Properties();
                    for (String nvp : getArgument2().split(",")) {
                        String[] kv = nvp.split("\\=", 2);
                        if (kv.length > 1) {
                            props.put(kv[0], kv[1]);
                        }
                    }
                    try {
                        JobResult result = launcher.submitJob(getArgument(), props).get();
                        this.response = result.getStatus().name() + " in " + result.getTimeTaken() + "ms\n" +
                                        result.getResult();
                    } catch (Exception ex) {
                        this.response = "Error: " + ex.getMessage();
                    }
                }
                break;
            case "CONFIG":
                this.command = command.CONFIG;
                if ("BATCHSIZE".equals(argument)) {
                    int bsize;
                    try {
                        bsize = Integer.parseInt(argument2);
                        LaunchConfiguration.getInstance().BATCH_SIZE = bsize;
                        this.response = "Batch size set to " + LaunchConfiguration.getInstance().BATCH_SIZE;
                    } catch (NumberFormatException ex) {
                        this.response = "Could not convert " + argument2 + " to a number.";
                    }
                } else if ("PARALLELISM".equals(argument)) {
                    int psize;
                    try {
                        psize = Integer.parseInt(argument2);
                        LaunchConfiguration.getInstance().PARALLELISM = psize;
                        this.response = "Parallelism set to " + LaunchConfiguration.getInstance().PARALLELISM;
                    } catch (NumberFormatException ex) {
                        this.response = "Could not convert " + argument2 + " to a number.";
                    }

                } else if ("WRITEAHEAD".equals(argument)) {
                    int psize;
                    try {
                        psize = Integer.parseInt(argument2);
                        LaunchConfiguration.getInstance().WRITEAHEAD = psize;
                        this.response = "Writeahead set to " + LaunchConfiguration.getInstance().WRITEAHEAD;
                    } catch (NumberFormatException ex) {
                        this.response = "Could not convert " + argument2 + " to a number.";
                    }

                } else if ("SPOOLING".equals(argument)) {
                    LaunchConfiguration.getInstance().SPOOLING = true;
                    this.response = "Spooling on.";
                } else if ("NOSPOOLING".equals(argument)) {
                    LaunchConfiguration.getInstance().SPOOLING = false;
                    this.response = "Spooling off.";
                } else if ("DUMPDIR".equals(argument)) {
                    LaunchConfiguration.getInstance().DUMP_DIRECTORY = argument2;
                    this.response = "Dump directory is now '" + LaunchConfiguration.getInstance().DUMP_DIRECTORY + "'";
                } else {
                    this.response = "Current Configuration:\n" + 
                        "  Parallelism: " + LaunchConfiguration.getInstance().PARALLELISM + '\n' +
                        "  Batch Size : " + LaunchConfiguration.getInstance().BATCH_SIZE + '\n' +
                        "  Spooling   : " + (LaunchConfiguration.getInstance().SPOOLING ? "on" : "off") + '\n' +
                        "  Writeahead : " + LaunchConfiguration.getInstance().WRITEAHEAD + '\n'  +
                        "  Dump Dir   : " + LaunchConfiguration.getInstance().DUMP_DIRECTORY;

                }
                break;
            case "":
                // Allow the user to just press return occasionally.
                this.command = Command.UNKNOWN;
                this.response = "";
                break;
            default:
                this.command = Command.UNKNOWN;
                this.response = "Unknown command '" + args[0] + "'";
                break;
        }

        writeResponse(writer);
        writer.flush();
    }

    public Command getCommandType() {
        return command;
    }

    public String getResponse() {
        return response;
    }

    public String getArgument() {
        return argument;
    }

    public String getArgument2() {
        return argument2;
    }

    public String getArguments() {
        return argument + " " + argument2;
    }

    private String processBinary(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c == '\b') {
                if (sb.length() > 0) {
                    sb.deleteCharAt(sb.length() - 1);
                }
            } else if(c >=32 && c < 127) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void writePrompt(PrintWriter writer) {
        writer.append(PROMPT);
        writer.flush();
    }

    private void writeResponse(PrintWriter writer) {
        if (this.response.equals("")) {
            return;
        }

        String[] lines = this.response.split("\n");
        for (String line : lines) {
            writer.append(PROMPT_RESPONSE);
            writer.println(line);
        }
    }
}
