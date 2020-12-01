(defproject tomthought/via-plus "0.1.0"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.logging "1.1.0"]

                 [tomthought/re-frame-utils "0.1.0"]

                 [com.7theta/via "6.2.1"]
                 [com.7theta/utilis "1.10.0"]

                 [ring/ring-core "1.8.2"]
                 [ring-cors/ring-cors "0.1.13"]
                 [ring/ring-defaults "0.3.2"]
                 [bk/ring-gzip "0.3.0"]

                 [borkdude/sci "0.1.1-alpha.10"]
                 [metosin/malli "0.2.1"]

                 [tick/tick "0.4.27-alpha"]
                 [integrant/integrant "0.8.0"]

                 ;; only need pedestal for io.pedestal.http.secure-headers
                 [io.pedestal/pedestal.service "0.5.8"
                  :exclusions [io.pedestal/pedestal.route
                               io.pedestal/pedestal.log
                               cheshire/cheshire
                               org.clojure/tools.analyzer.jvm
                               org.clojure/tools.reader
                               com.cognitect/transit-clj
                               commons-codec/commons-codec
                               crypto-random/crypto-random
                               crypto-equality/crypto-equality]]]
  :source-paths ["src" "aave/src"])
