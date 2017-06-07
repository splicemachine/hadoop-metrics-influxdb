# Overview
Metrics are collections of information about Hadoop daemons, events and measurements; for example, data nodes collect metrics such as the number of blocks replicated, number of read requests from clients, and so on. For that reason, metrics are an invaluable resource for monitoring Apache Hadoop services and an indispensable tool for debugging system problems.

The Metrics2 framework is designed to collect and dispatch per-process metrics to monitor the overall status of the Hadoop system. Producers register the metrics sources with the metrics system, while consumers register the sinks. The framework marshals metrics from sources to sinks based on (per source/sink) configuration options.

For the purposes of the DBaaS project we needed to add a sink that will send the metrics to the InfluxDB.  There was an open source project that had the initial code for a InfluxDB sink (https://github.com/arnobroekhof/hadoop-metrics-influxdb).  We made modifications to that code to build with a different version of hadoop as well as to fix some issues with data.

For more details on metrics collected by the Metrics2 framework see this page - https://hadoop.apache.org/docs/r2.7.2/hadoop-project-dist/hadoop-common/Metrics.html.

# Where is it used in DBAAS
In the dcos-commons project you will see several configuration files with the format of *Metrics2*.properties located at: https://github.com/splicemachine/dcos-commons/tree/master/frameworks/splicemachine/src/main/dist.  Those files reference this custom sink.


## How to Build and Deploy

1. Compile the code.
  ```
mvn clean compile package
  ```

## Example Config File Entries
The following is an example of a config file entry to use the sink
  ```
datanode.sink.file-all.class=org.apache.hadoop.metrics2.sink.influxdb.InfluxdbSink
datanode.sink.file-all.period=10
datanode.sink.file-all.url=http://{{INFLUX_DB_HOST}}:{{INFLUX_DB_PORT}}
datanode.sink.file-all.database={{INFLUX_DB_NAME}}
datanode.sink.file-all.username={{INFLUX_DB_USER}}
datanode.sink.file-all.password={{INFLUX_DB_PASSWORD}}
datanode.sink.file-all.cluster={{FRAMEWORK_NAME}}
datanode.sink.file-all.source=datanode
  ```
