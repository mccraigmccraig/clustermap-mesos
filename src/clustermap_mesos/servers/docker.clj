(ns clustermap-mesos.servers.docker
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package-source package-manager package]]))


(defn docker-server
  []
  (server-spec
   :phases
   {:configure (plan-fn
                (package-source "docker" :aptitude {:url "https://get.docker.io/ubuntu"
                                                    :release "docker"
                                                    :scopes ["main"]
                                                    :key-server "keyserver.ubuntu.com"
                                                    :key-id "36A1D7869245C8950F966E92D8576A8BA88D21E9"})
                (package-manager :update)
                (package "lxc-docker")

                (remote-file "/usr/local/bin/docker-clean" :local-file "resources/files/docker/docker-clean" :mode "755")
                (remote-file "/etc/cron.d/docker-clean" :content "01 03 * * * root /usr/local/bin/zookeeper-clean")))}))
