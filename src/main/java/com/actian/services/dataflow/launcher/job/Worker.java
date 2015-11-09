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
package com.actian.services.dataflow.launcher.job;

import com.actian.services.dataflow.launcher.MasterStatsMBean;
import com.actian.services.dataflow.launcher.job.JobResult.Status;
import com.actian.services.dataflow.operators.MockableExternalRecordSource;
import com.pervasive.datarush.cal.JobStatus;
import java.util.concurrent.Callable;

import com.pervasive.datarush.graphs.EngineConfig;
import com.pervasive.datarush.graphs.LogicalGraph;
import com.pervasive.datarush.graphs.LogicalGraphInstance;
import com.pervasive.datarush.operators.RecordPipelineOperator;
import com.pervasive.datarush.operators.sink.CollectRecords;
import com.pervasive.datarush.ports.LogicalPort;
import com.pervasive.datarush.sequences.record.RecordTokenList;
import com.pervasive.datarush.tokens.record.RecordSettable;
import com.pervasive.datarush.tokens.scalar.DateValued;
import com.pervasive.datarush.tokens.scalar.NumericValued;
import com.pervasive.datarush.tokens.scalar.ScalarSettable;
import com.pervasive.datarush.tokens.scalar.StringValued;
import com.pervasive.datarush.tokens.scalar.TimestampValued;
import com.pervasive.datarush.types.RecordTokenType;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;


public class Worker implements Callable<JobResult> {

    private final LogicalGraph graph;
    private final EngineConfig config;
    private final String id;
    private MasterStatsMBean stats;
    private CollectRecords collector;
    private final Properties params;
	
	public Worker(LogicalGraph graph, EngineConfig config,String id, Properties params) {
		super();
		this.graph = graph;
		this.config = config;
		this.id = id;
        this.params = params;

        try {
            ObjectName name = new ObjectName("com.actian.dataflow.launcher:name=Stats");
            ObjectInstance oi = ManagementFactory.getPlatformMBeanServer().getObjectInstance(name);
            MasterStatsMBean mbean = JMX.newMBeanProxy(ManagementFactory.getPlatformMBeanServer(), name, MasterStatsMBean.class);
            this.stats = mbean;
        } catch (MalformedObjectNameException | InstanceNotFoundException ex) {
            // Ignore
        }
	}

    @Override
	public JobResult call() throws Exception {
        JobResult result;
        Map<String,Object> resultProperties = new HashMap<>();
        try {
            if (this.params != null) {
                stitchInput();
            }
            long startTime = System.currentTimeMillis();
            LogicalGraphInstance gi = graph.compile(config);
            gi.run();
            long endTime = System.currentTimeMillis();

            if (this.collector != null) {
                RecordTokenList outputData = collector.getOutput();
                RecordSettable setter = outputData.getTokenSetter(0);
                RecordTokenType recordType = outputData.getType();
                for (String name : recordType.getNames()){
                    ScalarSettable value = setter.getField(name);
                    if (value instanceof TimestampValued) {
                        TimestampValued tvalue = (TimestampValued) value;
                        resultProperties.put(name, tvalue.isNull() ? null : tvalue.asTimestamp());
                    } else if (value instanceof StringValued) {
                        StringValued svalue = (StringValued) value;
                        resultProperties.put(name, svalue.isNull() ? null : svalue.asString());
                    } else if (value instanceof NumericValued) {
                        NumericValued nvalue = (NumericValued) value;
                        resultProperties.put(name, nvalue.isNull() ? null : nvalue.asBigDecimal());
                    } else if (value instanceof DateValued) {
                        DateValued dvalue = (DateValued) value;
                        resultProperties.put(name, dvalue.isNull() ? null : dvalue.asCalendarDate());
                    }
                }
            }
            
            result = new JobResult((int) (endTime-startTime), convertStatus(gi.getState().jobStatus()),resultProperties);

            if (stats != null) {
                if (result.getStatus() == Status.COMPLETE_SUCCESS) {
                    stats.increaseCompletedCount(result.getTimeTaken());
                } else if (result.getStatus() == Status.COMPLETE_CANCELLED) {
                    stats.increaseCancelledCount();
                } else if (result.getStatus() == Status.COMPLETE_FAILED) {
                    stats.increaseFailedCount();
                }
            }
        } catch (Exception ex) {
            if (stats != null){
                stats.increaseFailedCount();
            }
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, ex.getMessage());
            resultProperties.put("error", ex.getCause().getMessage());
            result = new JobResult(0,Status.COMPLETE_FAILED, resultProperties);
        }

		return result;
	}

    private void stitchInput() {

        // Find the start node.
        LogicalPort p = (LogicalPort)graph.getProperty("Start Node.output");
        MockableExternalRecordSource opStart = (MockableExternalRecordSource) p.getOwner();

        try {
            // Find the end node.
            LogicalPort q = (LogicalPort)graph.getProperty("Stop Node.output");

            RecordPipelineOperator opFinish = (RecordPipelineOperator) q.getOwner();
            // Add new nodes
            
            collector = new CollectRecords();
            graph.add(collector,"collector");
            graph.connect(opFinish.getOutput(), collector.getInput());

            // Setup custom input
            opStart.getProperties().putAll(params);
        } catch(Exception ex) {
            ; // Ignore if no Stop node.
        }

    }

    private Status convertStatus(JobStatus jobStatus) {
        switch (jobStatus) {
            case COMPLETE_SUCCESS: return Status.COMPLETE_SUCCESS;
            case COMPLETE_CANCELED: return Status.COMPLETE_CANCELLED;
            default: return Status.COMPLETE_FAILED;
        }
    }

}
