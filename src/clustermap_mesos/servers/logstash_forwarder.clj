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
   {:install (plan-fn
              (package "unzip")
              (package "golang")
              (exec-script* "gem install fpm")

              ;; download unpack and build lumberjack
              (directory "/opt" :action :create)

              (remote-file "/opt/logstash-forwarder-0.3.1.zip" :url "https://github.com/elasticsearch/logstash-forwarder/archive/v0.3.1.zip")

              (exec-script* "cd /opt ; if ! test -e logstash-forwarder-0.3.1 ; then unzip -n logstash-forwarder-0.3.1.zip ; fi")
              (exec-script* "cd /opt/logstash-forwarder-0.3.1 ; if ! test -e lumberjack_0.3.1_amd64.deb ; then PATH=/usr/local/bin:$PATH make deb ; dpkg -i lumberjack_0.3.1_amd64.deb ;fi"))

    :configure (plan-fn
                ;; configure and start
                (directory "/etc/pki/tls/certs" :action :create)
                (remote-file "/etc/pki/tls/certs/logstash-forwarder.crt" :local-file "resources/files/logstash_forwarder/certs/logstash-forwarder.crt")
                (directory "/etc/pki/tls/private" :action :create)
                (remote-file "/etc/pki/tls/private/logstash-forwarder.key" :local-file "resources/files/logstash_forwarder/certs/logstash-forwarder.key")

                (remote-file "/etc/lumberjack.conf" :local-file "resources/files/logstash_forwarder/lumberjack.conf")
                (remote-file "/etc/init/lumberjack.conf" :local-file "resources/files/logstash_forwarder/upstart_lumberjack.conf")
                (exec-script* "update-rc.d -f lumberjack remove"))

    :restart (plan-fn
              (service "lumberjack" :action :restart :service-impl :upstart))}))
