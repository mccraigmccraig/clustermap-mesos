(ns clustermap-mesos.servers.docker
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package-source package-manager package remote-file]]))


(defn docker-server
  []
  (server-spec
   :phases
   {:install (plan-fn
              (package-source "docker" :aptitude {:url "https://get.docker.io/ubuntu"
                                                  :release "docker"
                                                  :scopes ["main"]
                                                  :key-server "keyserver.ubuntu.com"
                                                  :key-id "36A1D7869245C8950F966E92D8576A8BA88D21E9"})
              (package-manager :update)
              (package "lxc-docker"))
    :configure (plan-fn
                (remote-file "/usr/local/bin/docker-clean" :local-file "resources/files/docker/docker-clean" :mode "755")
                (remote-file "/etc/cron.d/docker-clean" :content "01 03 * * * root /usr/local/bin/docker-clean")
                (remote-file "/home/ubuntu/.dockercfg" :local-file (str (System/getenv "HOME") "/.dockercfg"))
                (remote-file "/root/.dockercfg" :local-file (str (System/getenv "HOME") "/.dockercfg")))}))
