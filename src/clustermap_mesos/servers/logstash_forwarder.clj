(ns clustermap-mesos.servers.logstash-forwarder
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package package-source directory remote-file exec-script* symbolic-link service]]
   [clustermap-mesos.servers.ruby :refer [ruby-server]]))


(defn logstash-forwarder-server
  []
  (server-spec
   :extends [(ruby-server)]
   :phases
   {:configure (plan-fn
                (package "unzip")
                (package "golang")
                (exec-script* "gem install fpm")

                (directory "/opt" :action :create)
                (remote-file "/etc/lumberjack.conf" :local-file "resources/files/logstash_forwarder/lumberjack.conf")

                (remote-file "/opt/logstash-forwarder-0.3.1.zip" :url "https://github.com/elasticsearch/logstash-forwarder/archive/v0.3.1.zip")
                (exec-script* "cd /opt ; unzip -n logstash-forwarder-0.3.1.zip")

                (exec-script* "cd /opt/logstash-forwarder-0.3.1 ; PATH=/usr/local/bin:$PATH make deb")
                (exec-script* "cd /opt/logstash-forwarder-0.3.1 ; dpkg -i lumberjack_0.3.1_amd64.deb")
                (service "lumberjack" :action :restart))}))
