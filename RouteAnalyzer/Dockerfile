# Usa OpenJDK 21 come immagine base
FROM openjdk:21

# Imposta l'autore dell'immagine
LABEL authors="G07"

# Imposta la directory di lavoro
WORKDIR /app

# Copia il file JAR dentro il container
COPY build/libs/RouteAnalyzer-1.0-SNAPSHOT-all.jar app.jar
COPY ./waypoints.csv waypoints.csv
#COPY ./custom-parameters.yml custom-parameters.yml

# Espone la porta (se necessario, rimuovila se non serve)
# EXPOSE 8080

# Comando per eseguire l'applicazione
CMD ["java", "-jar", "app.jar"]
