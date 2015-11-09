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

import com.actian.services.dataflow.launcher.comms.Coordinator;
import com.actian.services.dataflow.launcher.comms.CoordinatorService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator  {

    private BundleContext bc;
    private Master primary;
    private CoordinatorService coordService;

    @Override
    public void start(BundleContext bc) throws Exception {
        System.out.println("Starting DF Launcher.");
        this.bc = bc;
        primary = Master.createMaster(LauncherService.Type.PRIMARY, 0);
        coordService = new Coordinator(primary, "localhost", 0);
        coordService.start();
        bc.registerService(LauncherService.class.getName(), primary, null);
    }

    @Override
    public void stop(BundleContext bc) throws Exception {
        System.out.println("Stopping DF Launcher.");
        primary.shutdown();
        coordService.shutdown();
    }

}
