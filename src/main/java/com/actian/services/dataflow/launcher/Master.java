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
import com.actian.services.dataflow.launcher.job.Worker;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import com.pervasive.datarush.graphs.EngineConfig;
import com.pervasive.datarush.graphs.LogicalGraph;
import com.pervasive.datarush.json.JSON;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Master implements LauncherService{

    private static final Logger LOG = Logger.getLogger(Master.class.getName());
    private State state;

    private ExecutorService service;
    private final Type nodeType;
    private String defaultGraph;
    private int port;
    private Set<REPListener> listeners;

    private Master() {
        this(Type.PRIMARY, 20);
    }

    private Master(Type nodeType, int poolSize) {
        this.state = State.INITIALIZING;
        this.nodeType = nodeType;

        if (poolSize <= 0) {
            poolSize = (int) (Runtime.getRuntime().availableProcessors() * 0.75 + 1);
        }
        LOG.log(Level.INFO,"Creating thread pool of size {0}",poolSize);

        service = Executors.newFixedThreadPool(poolSize);
        this.listeners = new HashSet<>();
        this.port = 1000 + 100 * this.nodeType.ordinal();
    }

    @Override
    public FutureTask<JobResult> submitJob(String graph) throws IOException {
        return submitJob(LauncherUtils.loadGraph(graph));
    }
    
    public FutureTask<JobResult> submitJob(LogicalGraph graph) throws IOException {
        return submitJob(graph, getConfig(), "", null);
    }
    
    @Override
    public FutureTask<JobResult> submitJob(String graph, Properties params) throws IOException {
        return submitJob(LauncherUtils.loadGraph(graph), getConfig(), "", params);
    }

    @Override
    public FutureTask<JobResult> submitJob(String file, Properties params, Map<String, Object> overrides) throws IOException {
        return submitJob(LauncherUtils.loadGraph(file,overrides),getConfig(),"",params);
    }

    private FutureTask<JobResult> submitJob(LogicalGraph graph, EngineConfig config, String taskID, Properties params) {
        FutureTask<JobResult> task;
        task = (FutureTask<JobResult>) service
                .submit(new Worker(graph, config, taskID, params));
        return task;
    }


    public static synchronized Master createMaster(Type nodeType, int poolSize) {
        Master m = new Master(nodeType,poolSize);
        m.setState(State.READY);
        return m;
    }

    public void startup() {
        try {

            LOG.log(Level.INFO, new JSON().prettyPrint(getConfig()));
            int numCores = EngineConfig.engine().getAvailableProcessors();
            LOG.log(Level.INFO, "Number of cores={0}", numCores);
            
            if (nodeType == Type.PRIMARY) {
                setState(State.RESPONDER);
            } else {
                setState(State.RESERVE);
            }


        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception", e);
            setState(State.FAILED);
        }

    }

    EngineConfig getConfig() {
        EngineConfig config = EngineConfig.engine().monitored(false);
        // Apply any overrides
        config = config.parallelism(LaunchConfiguration.getInstance().PARALLELISM);
        config = config.ports.batchSize(LaunchConfiguration.getInstance().BATCH_SIZE);
        config = config.ports.writeahead(LaunchConfiguration.getInstance().WRITEAHEAD);
        if (!LaunchConfiguration.getInstance().DUMP_DIRECTORY.isEmpty()) {
            config = config.monitored(true).dumpfilePath(LaunchConfiguration.getInstance().DUMP_DIRECTORY);
        }
        if (LaunchConfiguration.getInstance().SPOOLING == false) {
            config = config.ports.noSpooling();
        }

        return config;
    }

    public void shutdown() {
        LOG.log(Level.INFO,"Shuting down launcher.");
        service.shutdown();
        setState(State.SHUTDOWN);
    }

    public void setDefaultGraph(String filename) throws IOException {
        File f = new File(filename);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(f);
        JsonNode graphNode = root.findPath("logicalGraph");
        graphNode = graphNode.isMissingNode() ? root : graphNode;

        defaultGraph = graphNode.toString();
    }
    
    @Override
    public synchronized State getState() {
        return state;
    }

    @Override
    public synchronized void setState(State state) {
        this.state = state;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getType() {
        return nodeType.name();
    }

    @Override
    public void addCommandListener(REPListener app) {
        this.listeners.add(app);
    }

    @Override
    public void removeCommandListener(REPListener app) {
        this.listeners.remove(app);
    }

    @Override
    public String processCommand(String[] args, Properties props) {
        for(REPListener l : listeners) {
            String response = l.command(args,props);
            if (response != null) {
                return response;
            }
        }
        return null;
    }

}
