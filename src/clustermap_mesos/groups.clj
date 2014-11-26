(ns clustermap-mesos.groups
  (:require
   [pallet.api :refer [group-spec server-spec node-spec plan-fn group-nodes compute-service]]
   [pallet.actions :refer [package]]
   [pallet.configure :refer [pallet-config compute-service-properties]]
   pallet.node
   [com.palletops.awaze.elasticloadbalancing :as elb]
   [clustermap-mesos.servers.base :refer [base-server]]
   [clustermap-mesos.servers.mesos :refer [mesos-master-server mesos-slave-server]]
   [clustermap-mesos.servers.elasticsearch
    :refer [elasticsearch-master-server elasticsearch-data-server elasticsearch-nodata-server]]
   [clustermap-mesos.nodes :refer :all]))

(defn mesos-master-group
  []
  (group-spec "mesos-master"
              :extends [(base-server)
                        (mesos-master-server "clustermap-mesos")
                        (elasticsearch-master-server "clustermap" "256m")]
    :node-spec (eu-west-ubuntu-1404-hvm-ebs-node "t2.small" "eu-west-1c" "subnet-c9ece28f" "sg-8c2a86e9" "mccraigkey" "cmap2-appserver")
    ;; :count 3
    ))

(defn mesos-data-slave-group
  []
  (group-spec "mesos-data-slave"
              :extends [(base-server)
                        (mesos-slave-server)
                        (elasticsearch-data-server "clustermap" "2g")]
    :node-spec (eu-west-ubuntu-1404-pv-ebs-node "m3.large" "eu-west-1c" "subnet-c9ece28f" "sg-8c2a86e9" "mccraigkey" "cmap2-appserver")
    ;; :count 3
    ))

(defn mesos-nodata-slave-group
  []
  (group-spec "mesos-nodata-slave"
              :extends [(base-server)
                        (mesos-slave-server)
                        (elasticsearch-nodata-server "clustermap" "512m")]
    :node-spec (eu-west-ubuntu-1404-pv-ebs-node "m3.large" "eu-west-1c" "subnet-c9ece28f" "sg-8c2a86e9" "mccraigkey" "cmap2-appserver")
    ;; :count 3
    ))

(defn- make-sequential
  [x]
  (cond
   (nil? x) nil
   (sequential? x) x
   true [x]))

(defn- add-nodes-to-aws-elasticloadbalancers
  [compute-service-id group-specs load-balancer-names]
  (let [nodes (group-nodes (compute-service compute-service-id) (make-sequential group-specs))
        instance-ids (->> nodes (map :node) (map pallet.node/id) (map (fn [node-id] {:instance-id node-id})))

        load-balancer-names (make-sequential load-balancer-names)

        {:keys [identity credential endpoint provider]} (compute-service-properties (pallet-config) compute-service-id)
        credentials {:access-key identity :secret-key credential :endpoint endpoint}]

    (doseq [load-balancer-name load-balancer-names]
      (elb/register-instances-with-load-balancer credentials
                                                 {:load-balancer-name load-balancer-name
                                                  :instances instance-ids}))))

(defn add-slaves-to-aws-elasticloadbalancers
  [compute-service-id load-balancer-names]
  (add-nodes-to-aws-elasticloadbalancers compute-service-id [(mesos-data-slave-group) (mesos-nodata-slave-group)] load-balancer-names))

(comment
  (require '[pallet.api :refer :all])
  (require '[clustermap-mesos.groups :refer :all] :reload)
  (require '[pallet.actions :as actions])
  (def mesos-eu-west-1 (compute-service :mesos-eu-west-1))

  (def s (converge {(mesos-master-group) 3
                    (mesos-data-slave-group) 3
                    (mesos-nodata-slave-group) 0}
                   :compute mesos-eu-west-1))
  (add-slaves-to-aws-elasticloadbalancers :mesos-eu-west-1 ["clustermap2-mesos-lb" "ccm-mesos-lb" "tcm-mesos-lb"])

  ;; general lift : upgrade and configure everything
  (do (lift [(mesos-master-group) (mesos-data-slave-group) (mesos-nodata-slave-group)]
            :compute mesos-eu-west-1)
      nil)

  ;; upgrade a package (for shellshock patches in this case)
  (do (lift [(mesos-master-group) (mesos-data-slave-group) (mesos-nodata-slave-group)]
            :compute mesos-eu-west-1
            :phase (plan-fn (actions/package-manager :update)
                            (actions/package "bash" :action :upgrade)))
      nil)

  ;; DON'T DO THIS : lift a single group
  ;; groups needs access to other groups to set master ips etc
  (require '[clustermap-mesos.servers.elasticsearch :as ess])
  (do (lift [(mesos-data-slave-group)]
            :compute mesos-eu-west-1
            :phase  (-> (ess/elasticsearch-data-server "clustermap" "2g") :phases :configure))
      nil)

  )
