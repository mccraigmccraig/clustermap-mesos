(defproject clustermap-mesos "0.1.0-SNAPSHOT"
  :description "FIXME Pallet project for clustermap-mesos"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojure/core.incubator "0.1.3"]
                 [mccraigmccraig/pallet "0.8.0-SNAPSHOT"] ;; "0.8.0-RC.9"
                 [com.palletops/pallet-jclouds "1.7.3"]

                 ;; direct aws provider...
                 [mccraigmccraig/pallet-aws "0.2.4-SNAPSHOT" :exclusions [[com.palletops/awaze]]]
                 [mccraigmccraig/awaze "0.1.2-SNAPSHOT" :exclusions [[commons-logging] [commons-codec]]]
                 [org.slf4j/jcl-over-slf4j "1.7.5"]

                 ;; To get started we include all jclouds compute providers.
                 ;; You may wish to replace this with the specific jclouds
                 ;; providers you use, to reduce dependency sizes.
                 [org.apache.jclouds/jclouds-allblobstore "1.7.2"]
                 [org.apache.jclouds/jclouds-allcompute "1.7.2"]
                 [org.apache.jclouds.driver/jclouds-slf4j "1.7.2"
                  ;; the declared version is old and can overrule the
                  ;; resolved version
                  :exclusions [org.slf4j/slf4j-api]]
                 [org.apache.jclouds.driver/jclouds-sshj "1.7.2" :exclusions [net.schmizz/sshj]]
                 [ch.qos.logback/logback-classic "1.0.9"]]
  :profiles {:dev
             {:dependencies
              [[com.palletops/pallet "0.8.0-RC.9"
                :classifier "tests"]]
              :plugins
              [[com.palletops/pallet-lein "0.8.0-alpha.1"]]}
             :leiningen/reply
             {:dependencies [[org.slf4j/jcl-over-slf4j "1.7.2"]]
              :exclusions [commons-logging]}}
  :local-repo-classpath true
  :repositories
  {"sonatype" "https://oss.sonatype.org/content/repositories/releases/"}

  :jvm-opts ["-Xmx1g" "-XX:MaxPermSize=128m"]
  )
