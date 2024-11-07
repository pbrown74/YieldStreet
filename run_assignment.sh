#! /bin/bash

# pull the project from github
git clone https://github.com/pbrown74/yieldstreet

# build the assignment Docker image
docker build -t assignment-0.0.1 .

# this will pull MySQL and RabbitMQ images + JDK 17 , then it will try start the assignment
# but for me the first time it tried it pulled everything but failewd to start the assignment.
# i ran the same command a second time and it worked!!
docker compose up
