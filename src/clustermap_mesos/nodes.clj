(ns clustermap-mesos.nodes
  (:require
   [pallet.api :refer [node-spec]]))

(def ubuntu-1404-hvm-ebs-200-node
  (node-spec :image {:image-id "ami-96c41ce1";; alestic eu-west-1 ubuntu 14.04 HVM EBS-SSD
                     :os-family :ubuntu
                     :os-version "14.04"
                     :os-64-bit true
                     :login-user "ubuntu"
                     :key-name "mccraigkey"}
             :location {:location-id "eu-west-1b"}

             :hardware {}
             :network {}
             :qos {:enable-monitoring true}
             :provider {:pallet-ec2 {
                                     ;; :iam-instance-profile {:name instance-arn}
                                     ;; :subnet-id "clustermap-mesos-webapp"
                                     :network-interface [{:subnet-id "clustermap-mesos-webapp"
                                                          ;; :security-group-id "clustermap-mesos-webapp-sg"
                                                          :delete-on-termination true
                                                          :associate-public-ip-address false}]
                                     :block-device-mapping [{:device-name "/dev/xvda1"
                                     :ebs {:volume-size 200
                                           :delete-on-termination true
                                           }}]}
                        }
             ))

(def ubuntu-1404-pv-ebs-200-node
  (node-spec :image {:image-id "ami-aec41cd9"  ;; alestic eu-west-1 ubuntu 14.04 PV EBS-SSD
                     :os-family :ubuntu
                     :os-version "14.04"
                     :os-64-bit true
                     :login-user "ubuntu"
                     :key-name "mccraigkey"}
             :location {:location-id "eu-west-1b"}
             :qos {:enable-monitoring true}
             :provider {:pallet-ec2 {:instance-type :m3.large

                                     ;; :iam-instance-profile {:name instance-arn}
                                     ;; :subnet-id "clustermap-mesos-webapp"
                                     :network-interface [{:subnet-id "clustermap-mesos-webapp"
                                                          ;; :security-group-id "clustermap-mesos-webapp-sg"
                                                          :delete-on-termination true}]
                                     :block-device-mapping [{:device-name "/dev/xvda1"
                                                             :ebs {:volume-size 200
                                                                   :delete-on-termination true
                                                                   }}]}
                        }))
