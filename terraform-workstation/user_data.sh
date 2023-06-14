#!/bin/bash
sudo apt-get update
sudo apt-get install -y default-jre

# Download and run the Spring Boot application
sudo wget -O app.jar /Users/kdp/Projects/IdeaProjects/book-sharing-app/book-sharing-app-svc/target/book-sharing-app-svc-1.0-SNAPSHOT.jar
sudo nohup java -jar app.jar --server.port=8080 &
