FROM maven:3-jdk-9-slim
COPY . .
COPY mauth-proxy/pom.local.xml mauth-proxy/pom.xml
EXPOSE 9090
RUN mvn package -pl mauth-proxy -am -Dmaven.test.skip=true
CMD ["./runMauthProxyServer.bash"]


