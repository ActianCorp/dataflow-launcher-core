# dataflow-launcher-core
This project provides a simple launch interface to run JSON dataflow graphs from
a file in pseudo-distributed mode.
The launcher uses a fixed size pool and provides the following:

    * Ability to set the parallelism of the jobs.
    * Override all JDBC connections (database user/password/jdbc url and driverclass)
      for all Database Nodes.
    * Pass data into/out-of the workflow which use the Start/Stop Nodes provided by the 
      [dataflow core library](https://github.com/ActianCorp/core-library)
    * Built as an OSGI container with the idea to run inside Karaf.