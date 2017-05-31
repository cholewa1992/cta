FROM java:latest

# Install npm
RUN apt-get update && apt-cache search maven && apt-get install -y maven

RUN mkdir -p /src
WORKDIR /src

# install app dependencies and build project

ADD . /src
RUN mvn clean install

EXPOSE 3000