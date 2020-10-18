FROM openjdk:11
WORKDIR /app/
COPY ./* ./
RUN javac First.java