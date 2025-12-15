# --- Stage 1: Build the Spring Boot app ---
FROM amazoncorretto:25-alpine3.21 as build
LABEL stage=build

# Install Maven manually since Corretto base images don't include it
RUN apk add --no-cache maven
# apk add is Alpine’s package manager
#--no-cache: Prevents caching of the package index, making the image smaller and cleaner.

WORKDIR /app

# Copy pom.xml first to leverage build cache
COPY pom.xml .
# Copies only the pom.xml file into the image first. This allows Docker to cache dependencies, meaning that if your code changes but your dependencies don’t, Docker won’t re-download everything next time you build.

RUN mvn dependency:go-offline -B
# Downloads all dependencies declared in pom.xml ahead of time. The -B flag enables batch mode (non-interactive, faster, cleaner log


# Copy application source and build
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Run the app ---
FROM amazoncorretto:25-alpine3.21
WORKDIR /application

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar audio_player_be_app.jar

EXPOSE 8082

# Step 4: Run the app
ENTRYPOINT ["java","-jar","/application/audio_player_be_app.jar"]
#When you run this container, Docker will execute: java -jar /app.jar
