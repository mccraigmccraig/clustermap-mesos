(ns clustermap-mesos.nodes
  (:require
   [pallet.api :refer [node-spec]]))

(defn eu-west-ubuntu-1404-hvm-ebs-node
  [hardware location subnet-id security-group-id key-name]
  (node-spec
   :image {:image-id "ami-96c41ce1";; alestic eu-west-1 ubuntu 14.04 HVM EBS-SSD
           :os-family :ubuntu
           :os-version "14.04"
           :os-64-bit true
           :login-user "ubuntu"
           :key-name key-name}
   :hardware {:hardware-id hardware}
   :location {:location-id location}
   :provider {
              :pallet-ec2
              {
               :network-interfaces [{:device-index 0
                                     :subnet-id subnet-id
                                     :groups [security-group-id]
                                     :associate-public-ip-address true
                                     :delete-on-termination true}]
               :block-device-mappings [{:device-name "/dev/sda1"
                                        :ebs {:volume-size 100
                                              :delete-on-termination true
                                              :volume-type "gp2"}}]}}))

(defn eu-west-ubuntu-1404-pv-ebs-node
  [hardware location subnet-id security-group-id key-name]
  (node-spec
   :image {:image-id "ami-aec41cd9"  ;; alestic eu-west-1 ubuntu 14.04 PV EBS-SSD
           :os-family :ubuntu
           :os-version "14.04"
           :os-64-bit true
           :login-user "ubuntu"
           :key-name key-name}
   :hardware {:hardware-id hardware}
   :location {:location-id location}
   :provider {
              :pallet-ec2
              {
               :network-interfaces [{:device-index 0
                                     :subnet-id subnet-id
                                     :groups [security-group-id]
                                     :associate-public-ip-address true
                                     :delete-on-termination true}]
               :block-device-mappings [{:device-name "/dev/sda1"
                                        :ebs {:volume-size 200
                                              :delete-on-termination true
                                              :volume-type "gp2"}}]}}))
