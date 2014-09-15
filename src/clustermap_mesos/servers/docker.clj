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
                (package "lxc-docker"))}))
