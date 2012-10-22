# awseb-maven-plugin

Automate deployment to AWS Elastic Beanstalk

Example: pom.xml

    <plugin>
      <groupId>com.brewinapps</groupId>
      <artifactId>awseb-maven-plugin</artifactId>
      <version>1.0-SNAPSHOT</version>
      <configuration>
        <versionLabel>${env}-${project.version}-build${buildNumber}</versionLabel>
        <s3Key>builds/${project.version}/${env}/${project.artifactId}-build${buildNumber}-${maven.build.timestamp}.war</s3Key>
        <artifact>${project.build.directory}/${project.build.finalName}-${env}.war</artifact>
      </configuration>
    </plugin>

Example: execution

    awseb:deploy \
    -Denv=dev \
    -Dawseb.applicationName=project-ws \
    -Dawseb.environmentName=dev-project-ws \
    -Dawseb.s3Bucket=some-bucket \
    -DbuildNumber=${PROMOTED_NUMBER} \
    -Dawseb.accessKey=xxx \
    -Dawseb.secretKey=xxx

### License
ios-maven-plugin is licensed under the Creative Commons 3.0 License. Details can be found in the file LICENSE.
