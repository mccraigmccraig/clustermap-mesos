(ns clustermap-mesos.servers.logstash-forwarder
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package package-source directory remote-file exec-script* symbolic-link service with-service-restart]]
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

                (exec-script* "cd /opt/logstash-forwarder-0.3.1 ; if ! test -e lumberjack_0.3.1_amd64.deb ; then PATH=/usr/local/bin:$PATH make deb ; dpkg -i lumberjack_0.3.1_amd64.deb ;fi")
                (with-service-restart "lumberjack"
                  (remote-file "/etc/lumberjack.conf" :local-file "resources/files/logstash_forwarder/lumberjack.conf")))}))
