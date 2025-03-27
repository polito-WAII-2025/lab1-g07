[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/vlo9idtn)
# lab1-wa2025

# Route Analyzer Docker

This repository contains a Dockerfile to build and run the **Route Analyzer** application in a Docker container.

## Prerequisites

Make sure you have the following tools installed and running:

- [Docker Desktop](https://www.docker.com/get-started)

## Build the .jar file

To build the .jar file, run the following command in the project directory:

```bash
./gradlew build
```

## Build the Docker Image

To build the Docker image, run the following command in the project directory where the Dockerfile is located:

```bash
docker build -t route-analyzer .
```

## Run the Docker Image

To run the Docker image, run the following command 
in the directory where the input file is located:

```bash
docker run -v ${pwd}/output:/app/output -v ${pwd}/custom-parameters.yml:/app/custom-parameters.yml route-analyzer
```