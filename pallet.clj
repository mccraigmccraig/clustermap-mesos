;;; Pallet project configuration file

(require
 '[pallet.project.loader :refer [defproject]]
 '[clustermap-mesos.groups
   :refer [mesos-master-group
           mesos-data-slave-group
           mesos-nodata-slave-group]])

(defproject clustermap-mesos
  :source-paths ["src"]
  :provider {}
  :groups [(mesos-master-group)
           (mesos-data-slave-group)
           (mesos-nodata-slave-group)])
