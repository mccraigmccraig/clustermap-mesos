(ns clustermap-mesos.servers.ruby
  (:require
   [pallet.api :refer [server-spec plan-fn]]
   [pallet.actions :refer [package package-source directory remote-file symbolic-link]]))

(defn ruby-server
  []
  (server-spec
   :phases
   {:configure (plan-fn
                (package "autoconf")
                (package "bison")
                (package "build-essential")
                (package "libssl-dev")
                (package "libyaml-dev")
                (package "libreadline6-dev")
                (package "zlib1g-dev")
                (package "libncurses5-dev")
                (package "ruby2.0")
                (package "ruby2.0-dev")

                (symbolic-link "ruby2.0" "/usr/bin/ruby")
                (symbolic-link "erb2.0" "/usr/bin/erb")
                (symbolic-link "gem2.0" "/usr/bin/gem")
                (symbolic-link "irb2.0" "/usr/bin/irb")
                (symbolic-link "rake2.0" "/usr/bin/rake")
                (symbolic-link "rdoc2.0" "/usr/bin/rdoc")
                (symbolic-link "ri2.0" "/usr/bin/ri")
                (symbolic-link "testrb2.0" "/usr/bin/testrb"))}))
