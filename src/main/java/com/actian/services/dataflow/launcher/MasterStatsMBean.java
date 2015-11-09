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


public interface MasterStatsMBean {
	public int getTaskCount();
	public int getCompletedTaskCount();
	public int getFailedTaskCount();
	public int getCancelledTaskCount();
	public int getMinimumTime();
	public int getMaximumTime();
	public int getAverageTime();
	public void resetStats();
	public void increaseFailedCount();
	public void increaseCompletedCount(int time);
	public void increaseCancelledCount();
    public String summary();
}
