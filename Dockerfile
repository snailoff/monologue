FROM amd64/clojure:openjdk-11-lein-buster AS build-jar
WORKDIR /knot-clj
COPY . .
RUN lein uberjar

FROM amd64/amazoncorretto:11-alpine
WORKDIR /knot-clj

COPY --from=build-jar /knot-clj/target/knot-backend-0.1.0-SNAPSHOT-standalone.jar .
COPY --from=build-jar /knot-clj/docker-entrypoint.sh .

ENTRYPOINT ["/bin/sh", "./docker-entrypoint.sh"]
