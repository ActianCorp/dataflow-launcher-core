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
package com.actian.services.dataflow.launcher;

import com.actian.services.dataflow.launcher.comms.REPListener;
import com.actian.services.dataflow.launcher.job.JobResult;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.FutureTask;

public interface LauncherService {
    public static final String DB_URL = "Global!dbUrl";
    public static final String DB_USER = "Global!dbUser";
    public static final String DB_PASSWORD = "Global!dbPassword";
    public static final String DB_DRIVERNAME = "Global!dbDriverName";
    
    public enum State {
        INITIALIZING,   // In constructor
        RESPONDER,      // Should run graphs
        RESERVE,        // Should not run graphs
        ROLLINGFORWARD,
        READY,          // Afer initialisation
        FAILED,         // Some error has occurred
        SHUTDOWN
    };
   public enum Type { UNASSIGNED, PRIMARY, SECONDARY };
   public FutureTask<JobResult> submitJob(String file) throws IOException; 
   public FutureTask<JobResult> submitJob(String file, Properties params) throws IOException;
   public FutureTask<JobResult> submitJob(String file, Properties params, Map<String,Object> overrides) throws IOException;
   public State getState();
   public void setState(State state);
   public int getPort();
   public void setPort(int port);
   public void addCommandListener(REPListener app);
   public void removeCommandListener(REPListener app);
   public String processCommand(String[] args,Properties props);
   public String getType();
}
