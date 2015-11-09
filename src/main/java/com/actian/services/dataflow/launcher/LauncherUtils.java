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

import com.pervasive.datarush.graphs.LogicalGraph;
import com.pervasive.datarush.json.JSON;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

public class LauncherUtils {

    private static final Logger LOG = Logger.getLogger(LauncherUtils.class.getName());

    static LogicalGraph loadGraph(String filename) throws IOException, JsonProcessingException {
        return loadGraph(filename, Collections.EMPTY_MAP);
    }

    static LogicalGraph loadGraph(String filename, Map<String, Object> overrides) throws IOException, JsonProcessingException {
        File f = new File(filename);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(f);
        JsonNode graphNode = root.findPath("logicalGraph");
        graphNode = graphNode.isMissingNode() ? root : graphNode;
        // Find all nodes which are have @type : "readFromJDBC"
        overrideSettings(graphNode, overrides, mapper);
        String gstring = graphNode.toString();
        JSON parser = new JSON();

        LogicalGraph graph = parser.parse(gstring, LogicalGraph.class);
        // Now apply any standard overrides.
        for (String key : overrides.keySet()) {
            try {
                graph.setProperty(key, overrides.get(key));
            }
            catch (IllegalArgumentException ex) {
                // Unknown property
            }
        }
        return graph;
    }

    ;

    private static void overrideSettings(JsonNode graphNode, Map<String, Object> overrides, ObjectMapper mapper) {
        List<JsonNode> composites = graphNode.findValues("operators");
        for (JsonNode compNode : composites) {
            List<JsonNode> operators = compNode.findValues("operator");
            for (JsonNode operator : operators) {
                if (operator.isObject()) {
                    // Check if its a JDBC node
                    if (operator.path("jdbcConnector").isObject()) {
                        ObjectNode jdbc = (ObjectNode) operator.path("jdbcConnector");
                        if (overrides.containsKey(LauncherService.DB_USER)) {
                            jdbc.put("user", (String) overrides.get(LauncherService.DB_USER));
                            LOG.log(Level.FINE, "Overriding user.");
                        }
                        if (overrides.containsKey(LauncherService.DB_URL)) {
                            jdbc.put("url", (String) overrides.get(LauncherService.DB_URL));
                            LOG.log(Level.FINE, "Overriding url.");
                        }
                        if (overrides.containsKey(LauncherService.DB_DRIVERNAME)) {
                            jdbc.put("driverName", (String) overrides.get(LauncherService.DB_DRIVERNAME));
                            LOG.log(Level.FINE, "Overriding driverName.");
                        }
                        if (overrides.containsKey((LauncherService.DB_PASSWORD))) {
                            ObjectNode passwordNode = mapper.createObjectNode();
                            passwordNode.put("encryptedText", (String) overrides.get(LauncherService.DB_PASSWORD));
                            passwordNode.put("provider", "notencrypted");
                            jdbc.put("password", passwordNode);
                            LOG.log(Level.FINE, "Overriding password.");
                        }
                    }
                }
            }

            // Also apply to any composites.
            overrideSettings(compNode, overrides, mapper);
        }

    }

}
