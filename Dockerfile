FROM maven:3-jdk-9-slim
COPY . .
EXPOSE 9090
RUN mvn package -pl mauth-proxy -am -Dmaven.test.skip=true
CMD ["./dockerRunMauthProxy.sh"]
