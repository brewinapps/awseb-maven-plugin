package com.brewinapps.awseb;

import java.io.File;
import java.net.URL;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.joda.time.DateTime;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * @author Brewin' Apps AS
 */
@MojoGoal("deploy")
@MojoPhase("verify")
public class DeployMojo extends AbstractMojo
{
	private Log logger = getLog();

	@MojoParameter(expression="${awseb.accessKey}", required=true)
	private String accessKey;
	@MojoParameter(expression="${awseb.secretKey}", required=true)
	private String secretKey;
	@MojoParameter(expression="${awseb.s3Bucket}", required=true)
	private String s3Bucket;
	@MojoParameter(expression="${awseb.endpoint}", 
			defaultValue="elasticbeanstalk.eu-west-1.amazonaws.com", required=true)
	private String endpoint;
	@MojoParameter(expression="${awseb.s3Key}", 
			defaultValue="${project.build.finalName}-${maven.build.timestamp}.${project.packaging}")
	private String s3Key;	
	@MojoParameter(expression="${awseb.applicationName}", required=true)
	private String applicationName;
	@MojoParameter(expression="${awseb.environmentName}", required=true)
	private String environmentName;
	@MojoParameter(expression="${awseb.artifact}", 
			defaultValue="${project.build.directory}/${project.build.finalName}.${project.packaging}")
	private File artifact;
	@MojoParameter(expression="${awseb.versionLabel}",
			defaultValue="${project.version}-${maven.build.timestamp}")
	private String versionLabel;
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
		AWSElasticBeanstalkClient awsebClient = new AWSElasticBeanstalkClient(awsCredentials);
		awsebClient.setEndpoint(endpoint);
		
		logger.info(String.format("Using endpoint: %s", endpoint));
		
		// Upload artifact
		logger.info("Uploading artifact...");
		uploadArtifact(awsCredentials);
		
		// Deploy version
		logger.info("Deploying version...");
		deployVersion(awsebClient);
	}
	
	/**
	 * @param awsCredentials
	 * @return
	 * @throws MojoFailureException
	 */
	private URL uploadArtifact(AWSCredentials awsCredentials) throws MojoFailureException {
		AmazonS3Client s3Client = new AmazonS3Client(awsCredentials);
		
		if (!artifact.exists()) {
			throw new MojoFailureException(String.format("Artifact does not exist: %s", artifact));
		}
		
		s3Client.putObject(s3Bucket, s3Key, artifact);
		return s3Client.generatePresignedUrl(s3Bucket, s3Key, 
				new DateTime().plusDays(7).toDate());
	}
	
	private void deployVersion(AWSElasticBeanstalkClient awsebClient) {
		S3Location sourceBundleLocation = new S3Location(s3Bucket, s3Key);
		UpdateEnvironmentRequest updateEnvironmentRequest = new UpdateEnvironmentRequest();
		
		
		// Create application version
		logger.info("* Creating application version...");
		CreateApplicationVersionRequest createApplicationVersionRequest = 
				new CreateApplicationVersionRequest();
		createApplicationVersionRequest.withApplicationName(applicationName)
			.withVersionLabel(versionLabel)
			.withSourceBundle(sourceBundleLocation);
		awsebClient.createApplicationVersion(createApplicationVersionRequest);
		
		// Update environment
		logger.info("* Updating environment...");
		updateEnvironmentRequest.withEnvironmentName(environmentName)
			.withVersionLabel(versionLabel);
		awsebClient.updateEnvironment(updateEnvironmentRequest);
	}

}
