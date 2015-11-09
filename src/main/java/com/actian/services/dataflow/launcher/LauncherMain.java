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

import com.actian.services.dataflow.launcher.LauncherService.Type;
import com.actian.services.dataflow.launcher.comms.Coordinator;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

public class LauncherMain {
    private static final Logger LOG = Logger.getLogger(LauncherMain.class.getName());

    public static void main(String[] args) {
        Master self;
        MBeanServer mbs;
        MasterStatsMBean stats;

        Type nodeType = Type.PRIMARY;
        int poolSize = 20;
        String primaryUrl = "localhost";
        int primaryPort = 1100;

        if (args.length >= 1) {
            nodeType = Type.valueOf(args[0]);
            if (nodeType == Type.PRIMARY) {
                primaryPort = 0;
            }
        }
        if (args.length >= 2) {
            poolSize = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            primaryUrl = args[2];
        }
        if (args.length >= 4) {
            primaryPort = Integer.parseInt(args[3]);
        }

        mbs = ManagementFactory.getPlatformMBeanServer();
        stats = new MasterStats();

        try {
            ObjectName statsName = new ObjectName("com.actian.dataflow.launcher:name=Stats");
            mbs.registerMBean(stats, statsName);
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            LOG.log(Level.SEVERE, "MBean registration", e);
        }

        self = Master.createMaster(nodeType, poolSize);

        LOG.log(Level.INFO,"Starting up coordinator.");
        // Launch coordinator
        new Coordinator(self, primaryUrl, primaryPort).start();
        self.startup();
        
        boolean flag = true;
        while (flag) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(LauncherMain.class.getName()).log(Level.SEVERE, null, ex);
                flag = false;
            }
        }

    }
    
}
