# Simple Scalatra Archetype

An Apache Maven archetype to generate a simple Scalatra webapp.

# Quick Start

1. Generate your Scalatra project
		mvn archetype:generate -DarchetypeArtifactId=simple-scalatra-archetype -DarchetypeGroupId=org.scalatra -DarchetypeVersion=1.0-SNAPSHOT -DgroupId={TempGroupId} -DartifactId={TempArtifactId}

2. Switch to the project directory
		cd {TempArtifactId}

3. Install your project
		mvn install

4. Run Jetty webserver
		mvn jetty:run