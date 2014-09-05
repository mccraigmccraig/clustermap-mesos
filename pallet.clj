;;; Pallet project configuration file

(require
 '[pallet.project.loader :refer [defproject]]
 '[clustermap-mesos.groups.mesos-master :refer [mesos-master]]
 '[clustermap-mesos.groups.mesos-slave :refer [mesos-slave]])

(defproject clustermap-mesos
  :provider {:jclouds
             {:node-spec
              {:image {:os-family :ubuntu :os-version-matches "14.04"
                       :os-64-bit true}}}
             }

  :groups [mesos-master mesos-slave])
