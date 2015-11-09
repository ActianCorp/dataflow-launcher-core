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


public class MasterStats implements MasterStatsMBean {

	private int taskCount;
	private int completedTaskCount;
	private int failedTaskCount;
	private int cancelledTaskCount;
	private int minimumTime;
	private int maximumTime;
	private long totalTime;
	
	public int getTaskCount() {
		return taskCount;
	}

	public int getCompletedTaskCount() {
		return completedTaskCount;
	}

	public int getFailedTaskCount() {
		return failedTaskCount;
	}

	public int getCancelledTaskCount() {
		return cancelledTaskCount;
	}

	public int getMinimumTime() {
		return minimumTime;
	}

	public int getMaximumTime() {
		return maximumTime;
	}

	public int getAverageTime() {
		return completedTaskCount > 0 ? (int) (totalTime/completedTaskCount) : 0;
	}

	public void resetStats() {
		taskCount = 0;
		failedTaskCount = 0;
		completedTaskCount = 0;
		cancelledTaskCount = 0;
		minimumTime = 0;
		maximumTime = 0;
		totalTime = 0;
	}

	public void increaseFailedCount() {
		failedTaskCount++;
		taskCount++;
	}

	public synchronized void increaseCompletedCount(int time) {
		completedTaskCount++;
		taskCount++;
		if (time < (minimumTime == 0 ? Integer.MAX_VALUE : minimumTime) ) {
			minimumTime = time;
		}
		if (time > maximumTime) {
			maximumTime = time;
		}
		totalTime = totalTime + time;
	}

	public void increaseCancelledCount() {
		cancelledTaskCount++;
		taskCount++;
	}

    public String summary() {
        String s = "Tasks    : " + this.taskCount + '\n' +
                   "Completed: " + this.completedTaskCount + '\n' +
                   "Mean Time: " + this.getAverageTime() + '\n';

        return s;
    }

    @Override
    public String toString() {
        return this.summary();
    }

}
