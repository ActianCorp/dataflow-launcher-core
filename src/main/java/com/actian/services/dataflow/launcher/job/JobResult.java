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

import java.util.Map;

public final class JobResult {

    public static final String NULL_STRING = "@NULL@";
    public enum Status { COMPLETE_SUCCESS, COMPLETE_FAILED, COMPLETE_CANCELLED }
    private final int timeTaken;
    private final Status status;
    private final Map<String,Object> result;
    private String stats;

    public JobResult(int timeTaken, Status status, Map<String,Object> result) {
        this.timeTaken = timeTaken;
        this.status = status;
        this.result = result;
    }

    public int getTimeTaken() {
        return timeTaken;
    }

    public Status getStatus() {
        return status;
    }

    public String getStats() {
        return stats;
    }

    public void setStats(String stats) {
        this.stats = stats;
    }

    public Map<String,Object> getResult() {
        return this.result;
    }

}
