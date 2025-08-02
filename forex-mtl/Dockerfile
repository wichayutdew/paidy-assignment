FROM sbtscala/scala-sbt:eclipse-temurin-17.0.4_1.7.1_3.2.0
# Set working directory
WORKDIR /app

# Copy build configuration
COPY build.sbt .
COPY project/ project/

# Download dependencies
RUN sbt update

# Copy source code
COPY src/ src/

# Expose port
EXPOSE 8090

# Set environment variables
ENV HTTP_PORT=8090
ENV HTTP_HOST=0.0.0.0

# Run with sbt (development mode)
CMD ["sbt", "run"]
