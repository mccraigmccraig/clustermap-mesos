;;; Pallet project configuration file

(require
 '[pallet.project.loader :refer [defproject]]
 '[clustermap-mesos.groups
   :refer [mesos-master-group
           mesos-slave-es-data-group
           mesos-slave-es-nodata-group]])

(defproject clustermap-mesos
  :provider {}
  :groups [mesos-master-group
           mesos-slave-es-data-group
           mesos-slave-es-nodata-group])
