FROM amd64/clojure:openjdk-11-lein-buster AS build-jar
WORKDIR /monologue
COPY . .
RUN lein uberjar

FROM amd64/amazoncorretto:11-alpine
WORKDIR /monologue

COPY --from=build-jar /monologue/target/knot-monologue-0.1.0-SNAPSHOT-standalone.jar .
COPY --from=build-jar /monologue/docker-entrypoint.sh .

ENTRYPOINT ["/bin/sh", "./docker-entrypoint.sh"]
