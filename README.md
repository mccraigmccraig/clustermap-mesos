# clustermap-mesos

A pallet project to manage a mesos cluster for clustermap, with mesos, marathon docker and elasticsearch

You get :

* master nodes : zookeeper, mesos, marathon and elasticsearch masters
* data slave nodes : docker, mesos slave and elasticsearch data node
* nodata slave nodes : docker, mesos slave and elasticsearch nodata node

all nodes have an haproxy configured from marathon's api, so any apps running on mesos/marathon are available at the port configured on localhost

all nodes have logstash-forwarder sending syslog entries to port 5043. the logstash app config in apps.sh will run logstash on mesos/marathon to push log entries from all nodes into elasticsearch : to log from apps in docker containers just mount /dev/log into the container and log to syslog

there are some infrastructure apps defined in apps.sh which will run on mesos/marathon :

* chronos : distributed cron
* logstash : indexes log entries from logstash-forwarder in elasticsearch
* kibana : dashboard for querying logstash indexes


Copyright © Trampoline Systems Limited
