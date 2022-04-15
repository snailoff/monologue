FROM clojure:openjdk-17-lein-buster AS buildjar
WORKDIR /knot-clj
COPY . .
RUN lein uberjar

FROM amazoncorretto:17-alpine
WORKDIR /knot-clj

COPY --from=build-jar /knot-clj/target/knot-backend-0.1.0-SNAPSHOT-standalone.jar .
COPY --from=build-jar /knot-clj/entrypoint.sh .

ENTRYPOINT ["/bin/sh", "./entrypoint.sh"]