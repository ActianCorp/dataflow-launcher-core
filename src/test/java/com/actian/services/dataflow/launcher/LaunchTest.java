package com.actian.services.dataflow.launcher;

import com.actian.services.dataflow.launcher.job.JobResult;
import static com.actian.services.dataflow.launcher.job.JobResult.Status.*;
import com.actian.services.dataflow.launcher.job.Worker;
import com.pervasive.datarush.graphs.LogicalGraph;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import junit.framework.Assert;
import org.junit.Test;

public class LaunchTest {

    @Test
    public void countCharacters() throws IOException, Exception {
        Properties p = new Properties();

        System.out.println("Doing test");
        Master launcher = Master.createMaster(LauncherService.Type.PRIMARY, 0);
        String filename = getClass().getResource("/graphs/CharCount.dr").getFile();
        JobResult result;
        String testData = "monkeys and cheetas";

        LogicalGraph g = LauncherUtils.loadGraph(filename);
        p.put("body", testData);
        Worker w = new Worker(g, launcher.getConfig(), "Test1", p);
        result = w.call();

        System.out.println("Result was:" + result.getResult() + " in time: " + result.getTimeTaken());
        Assert.assertTrue("Job completed successfully.", result.getStatus() == COMPLETE_SUCCESS);

        Assert.assertEquals("Result was correct", testData.length(), Integer.parseInt(result.getResult().get("charCount").toString()));

    }

    @Test
    public void testDBRewrites() throws Exception {

        String filename = getClass().getResource("/graphs/MetaNode.dr").getFile();
        Map<String,Object> overrides = new HashMap<>();
        overrides.put(LauncherService.DB_USER, "testuser");
        LogicalGraph g = LauncherUtils.loadGraph(filename, overrides);
        Assert.assertEquals("testuser",g.getProperty("Node 3.user"));
        Assert.assertEquals("testuser",g.getProperty("Metanode.Database Reader.user"));
        Assert.assertEquals("testuser",g.getProperty("Metanode.Database Reader_2.user"));
        Assert.assertEquals("testuser",g.getProperty("Metanode.Metanode 2.Database Reader.user"));
    }

}
