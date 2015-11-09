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

import java.util.Map;
import java.util.logging.Logger;

public class LaunchConfiguration {
    private static final Logger LOG = Logger.getLogger(LaunchConfiguration.class.getName());

    public int CONNECTION_RETRIES;
    public int CONNECTION_PULSE_MS;
    public int CONNECTION_REATTACH_MS;
    public int BATCH_SIZE;
    public int PARALLELISM;
    public int WRITEAHEAD;
    public boolean SPOOLING;
    public String DUMP_DIRECTORY;
    public LaunchConfiguration() {
        this.CONNECTION_RETRIES = 3;
        this.CONNECTION_PULSE_MS = 1000;
        this.CONNECTION_REATTACH_MS = 5000;
        this.BATCH_SIZE = 1024;
        this.PARALLELISM = 1;
        this.SPOOLING = true;
        this.WRITEAHEAD = 2;
        this.DUMP_DIRECTORY="";
    }

    public static LaunchConfiguration getInstance() {
        return ConfigurationHolder.INSTANCE;
    }

    private static class ConfigurationHolder {
        private static final LaunchConfiguration INSTANCE = new LaunchConfiguration();
    }

    public void register(Master launcher, Map<String,Object> props ) {
        System.out.println("register");
        for (String k : props.keySet()){
            System.out.println("key=" + k);
        }
    }

    public void unregister(Master launcher, Map props) {
        System.out.println("unregister");
    }
}
