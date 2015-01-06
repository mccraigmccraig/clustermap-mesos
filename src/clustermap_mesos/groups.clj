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
    :refer [elasticsearch-master-server elasticsearch-data-server elasticsearch-nodata-server]]))

(defn mesos-master-group
  [{:keys [cluster-name node-spec extends]}]
  (group-spec (str cluster-name "-master")
              :extends (into [(base-server)
                              (mesos-master-server cluster-name)]
                             extends)
              :roles [:mesos-master]
              :node-spec node-spec))

(defn mesos-slave-group
  [{:keys [cluster-name slave-group-name node-spec attributes extends]}]
  (group-spec (str cluster-name "-" (or slave-group-name "slave"))
              :extends (into [(base-server)
                              (mesos-slave-server attributes)]
                             extends)
              :roles [:mesos-slave]
              :node-spec node-spec))

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
  [cluster-groups compute-service-id load-balancer-names]
  (let [slave-groups (filter #(:mesos-slave (:roles %)) (keys cluster-groups))]
    (add-nodes-to-aws-elasticloadbalancers compute-service-id slave-groups load-balancer-names)))

(comment
  (require '[pallet.api :refer :all])
  (require '[clustermap-mesos.groups :refer :all] :reload)
  (require '[clustermap-mesos.nodes :refer :all] :reload)
  (require '[pallet.actions :as actions])
  (require '[clustermap-mesos.servers.elasticsearch
             :refer [elasticsearch-master-server elasticsearch-data-server elasticsearch-nodata-server]])

  (def mesos-eu-west-1 (compute-service :mesos-eu-west-1))

  (def vpc-params {:location "eu-west-1c"
                   :subnet-id "subnet-c9ece28f"
                   :security-group-id "sg-8c2a86e9"
                   :key-name "mccraigkey"
                   :iam-instance-profile-name "cmap2-appserver"})

  (def small-node (eu-west-ubuntu-1404-hvm-ebs-node (merge vpc-params {:hardware "t2.small" :volume-size 100})))
  (def large-node (eu-west-ubuntu-1404-hvm-ebs-node (merge vpc-params {:hardware "m3.large" :volume-size 200})))

  (def cluster-groups
    {(mesos-master-group {:cluster-name "mesos" :node-spec small-node :extends [(elasticsearch-master-server cluster-name "512m")]}) 3
     (mesos-slave-group {:cluster-name "mesos" :node-spec large-node :extends [(elasticsearch-data-server cluster-name "2g")]}) 2})

  (def cluster-groups
    {(mesos-master-group {:cluster-name "test"
                          :node-spec small-node
                          :extends [(elasticsearch-master-server "test" "512m")]}) 3

     (mesos-slave-group {:cluster-name "test"
                         :slave-group-name "cassandra-slave"
                         :node-spec large-node
                         :attributes {:cassandra true}}) 2

     (mesos-slave-group {:cluster-name "test"
                         :slave-group-name "es-slave"
                         :node-spec large-node
                         :extends [(elasticsearch-data-server "test" "2g")]
                         :attributes {:elasticsearch true}}) 2})

  (def s (converge cluster-groups
                   :compute mesos-eu-west-1
                   :phase [:install :configure :restart]))

  (add-slaves-to-aws-elasticloadbalancers cluster-groups :mesos-eu-west-1 ["clustermap2-mesos-lb" "ccm-mesos-lb" "tcm-mesos-lb"])

  ;; general lift : configure and restart everything already installed
  (do (lift (keys cluster-groups)
            :compute mesos-eu-west-1
            :phase [:configure :restart])
      nil)

  ;; upgrade a package (for shellshock patches in this case)
  (do (lift (keys cluster-groups)
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
