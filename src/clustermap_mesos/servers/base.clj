(ns clustermap-mesos.servers.base
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package exec-script* file]]
   [pallet.crate.automated-admin-user :refer [automated-admin-user]]))

(defn  base-server
  "Defines the type of node clustermap-mesos will run on"
  []
  (server-spec
   :phases
   {:bootstrap (plan-fn
                (automated-admin-user)
                (exec-script* "locale-gen en_GB.UTF-8 en_US.UTF-8"))
    :configure (plan-fn
                (package "openntpd")
                (exec-script* "if ! test -e /swapfile1 ; then dd if=/dev/zero of=/swapfile1 bs=1024 count=8388608 ; mkswap /swapfile1 ; fi")
                (file "/swapfile1" :action :touch :mode "0600" :owner "root" :group "root")
                (exec-script* "if ! test -e /etc/fstab.org ; then cp /etc/fstab /etc/fstab.org ; fi ; cat /etc/fstab.org | grep -v /swapfile1 > /etc/fstab")
                (exec-script* "echo \"/swapfile1 swap swap defaults 0 0\" >> /etc/fstab")
                (exec-script* "swapon -a"))}))
