# Simple Scalatra Archetype

An Apache Maven archetype to generate a simple Scalatra webapp.

# Quick Start

		mvn archetype:generate -DarchetypeArtifactId=simple-scalatra-archetype -DarchetypeGroupId=org.scalatra -DarchetypeVersion=1.0-SNAPSHOT -DgroupId={TempGroupId} -DartifactId={TempArtifactId}

		cd {TempArtifactId}
		
		mvn install
		
		mvn jetty:run