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
  [{:keys [cluster-name location subnet-id security-group-id key-name iam-instance-profile-name]}]
  (group-spec (str cluster-name "-master")
              :extends [(base-server)
                        (mesos-master-server cluster-name)
                        (elasticsearch-master-server cluster-name "512m")]
              :node-spec (eu-west-ubuntu-1404-hvm-ebs-node
                          {:hardware "t2.small"
                           :location location
                           :subnet-id subnet-id
                           :security-group-id security-group-id
                           :key-name key-name
                           :iam-instance-profile-name iam-instance-profile-name})
    ;; :count 3
    ))

(defn mesos-data-slave-group
  [{:keys [cluster-name location subnet-id security-group-id key-name iam-instance-profile-name]}]
  (group-spec (str cluster-name "-data-slave")
              :extends [(base-server)
                        (mesos-slave-server)
                        (elasticsearch-data-server cluster-name "2g")]
              :node-spec (eu-west-ubuntu-1404-pv-ebs-node
                          {:hardware "m3.large"
                           :location location
                           :subnet-id subnet-id
                           :security-group-id security-group-id
                           :key-name key-name
                           :iam-instance-profile-name iam-instance-profile-name})
    ;; :count 3
    ))

(defn mesos-nodata-slave-group
  [{:keys [cluster-name location subnet-id security-group-id key-name iam-instance-profile-name]}]
  (group-spec (str cluster-name "-nodata-slave")
              :extends [(base-server)
                        (mesos-slave-server)
                        (elasticsearch-nodata-server cluster-name "512m")]
              :node-spec (eu-west-ubuntu-1404-pv-ebs-node
                          {:hardware "m3.large"
                           :location location
                           :subnet-id subnet-id
                           :security-group-id security-group-id
                           :key-name key-name
                           :iam-instance-profile-name iam-instance-profile-name})
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
  [cluster-params compute-service-id load-balancer-names]
  (add-nodes-to-aws-elasticloadbalancers compute-service-id [(mesos-data-slave-group cluster-params)
                                                             (mesos-nodata-slave-group cluster-params)] load-balancer-names))

(comment
  (require '[pallet.api :refer :all])
  (require '[clustermap-mesos.groups :refer :all] :reload)
  (require '[pallet.actions :as actions])
  (def mesos-eu-west-1 (compute-service :mesos-eu-west-1))

  (def cluster-params {:cluster-name "clustermap"
                       :location "eu-west-1c"
                       :subnet-id "subnet-c9ece28f"
                       :security-group-id "sg-8c2a86e9"
                       :key-name "mccraigkey"
                       :iam-instance-profile-name "cmap2-appserver"})

  (def cluster-params {:cluster-name "test"
                       :location "eu-west-1c"
                       :subnet-id "subnet-c9ece28f"
                       :security-group-id "sg-8c2a86e9"
                       :key-name "mccraigkey"
                       :iam-instance-profile-name "cmap2-appserver"})

  (def s (converge {(mesos-master-group cluster-params) 3
                    (mesos-data-slave-group cluster-params) 3
                    (mesos-nodata-slave-group cluster-params) 0}
                   :compute mesos-eu-west-1
                   :phase [:install :configure :restart]))

  (add-slaves-to-aws-elasticloadbalancers cluster-params :mesos-eu-west-1 ["clustermap2-mesos-lb" "ccm-mesos-lb" "tcm-mesos-lb"])

  ;; general lift : configure and restart everything already installed
  (do (lift [(mesos-master-group cluster-params)
             (mesos-data-slave-group cluster-params)
             (mesos-nodata-slave-group cluster-params)]
            :compute mesos-eu-west-1
            :phase [:configure :restart])
      nil)

  ;; upgrade a package (for shellshock patches in this case)
  (do (lift [(mesos-master-group cluster-params)
             (mesos-data-slave-group cluster-params)
             (mesos-nodata-slave-group cluster-params)]
            :compute mesos-eu-west-1
            :phase (plan-fn (actions/package-manager :update)
                            (actions/package "bash" :action :upgrade)))
      nil)

  ;; DON'T DO THIS : lift a single group
  ;; groups needs access to other groups to set master ips etc
  (require '[clustermap-mesos.servers.elasticsearch :as ess])
  (do (lift [(mesos-data-slave-group cluster-params)]
            :compute mesos-eu-west-1
            :phase  (-> (ess/elasticsearch-data-server "clustermap" "2g") :phases :configure))
      nil)

  )
