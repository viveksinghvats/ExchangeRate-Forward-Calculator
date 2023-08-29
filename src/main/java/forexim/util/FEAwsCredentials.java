package forexim.util;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.apigateway.AmazonApiGateway;
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder;
import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.AmazonAthenaClientBuilder;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEvents;
import com.amazonaws.services.cloudwatchevents.AmazonCloudWatchEventsClientBuilder;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentity;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClientBuilder;
import com.amazonaws.services.costexplorer.AWSCostExplorer;
import com.amazonaws.services.costexplorer.AWSCostExplorerClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.glue.AWSGlue;
import com.amazonaws.services.glue.AWSGlueClientBuilder;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53ClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

public class FEAwsCredentials {

    private static final boolean runLocally = true;

    private static Regions region = Regions.AP_SOUTH_1;

    private static DynamoDBMapper cachedDynamoDBMapper;

    private AWSCredentialsProvider cachedCredentialsProvider = null;

    private static final String localAwsDevProfileName = AwsServerType.DEV.getAwsCredentialsProfileName();

    public static final FEAwsCredentials shared = new FEAwsCredentials();

    private AWSCredentialsProvider _getAWSCredentialProvider() {
        if (cachedCredentialsProvider != null) { return cachedCredentialsProvider; }
        synchronized (this) {
            if (cachedCredentialsProvider == null) {	// Null check is duplicated to avoid an expensive sync operation.
                if (runLocally) {
                    var profileName = localAwsDevProfileName;
                    var profileProviderCredentials = new ProfileCredentialsProvider(profileName).getCredentials();
                    cachedCredentialsProvider = new AWSStaticCredentialsProvider(profileProviderCredentials);
                } else {
                    cachedCredentialsProvider = new EnvironmentVariableCredentialsProvider();
                }
            }
        }
        return cachedCredentialsProvider;
    }

    private AWSLambda cachedLambdaClient = null;

    public static AWSLambda lambdaClient() { return shared._lambdaClient(); }

    private AWSLambda _lambdaClient() {
        if (cachedLambdaClient == null) {
            cachedLambdaClient = AWSLambdaClientBuilder.standard().withCredentials(getAWSCredentialProvider()).withRegion(region).build();
        }
        return cachedLambdaClient;
    }

    public static DynamoDBMapper dynamoDBMapper() { return shared._dynamoDBMapper(); }
    private DynamoDBMapper _dynamoDBMapper() {
        if (cachedDynamoDBMapper != null) { return cachedDynamoDBMapper; }
        synchronized (this) {
            if (cachedDynamoDBMapper == null) {
                cachedDynamoDBMapper = new DynamoDBMapper(dynamoDBClient());
            }
        }
        return cachedDynamoDBMapper;
    }

    private AmazonDynamoDB cachedDynamoDBClient = null;
    public static AmazonDynamoDB dynamoDBClient() { return shared._dynamoDBClient(); }
    private AmazonDynamoDB _dynamoDBClient() {
        if (cachedDynamoDBClient != null) { return cachedDynamoDBClient; }
        synchronized (this) {
            if (cachedDynamoDBClient == null) {
                cachedDynamoDBClient = AmazonDynamoDBClientBuilder.standard().withCredentials(getAWSCredentialProvider()).withRegion(region).build();
            }
        }
        return cachedDynamoDBClient;
    }

    public static DynamoDB dynamoDB() {
        return new DynamoDB(dynamoDBClient());
    }

    public static AmazonSNS snsClient() { return shared._snsClient(); }

    private AmazonSNS _snsClient() {
        return AmazonSNSClientBuilder.standard().withCredentials(getAWSCredentialProvider()).withRegion(region).build();
    }

    private AmazonS3 cachedS3Client = null;

    public static AmazonS3 s3Client() { return shared._s3Client(); }

    private AmazonS3 _s3Client() {
        if (cachedS3Client != null) { return cachedS3Client; }
        synchronized (this) {
            if (cachedS3Client == null) {
                cachedS3Client = AmazonS3ClientBuilder.standard().withCredentials(getAWSCredentialProvider()).withRegion(region).build();
            }
        }
        return cachedS3Client;
    }

    public static AWSCredentialsProvider getAWSCredentialProvider() { return shared._getAWSCredentialProvider(); }

    public static AmazonSimpleEmailService sesClient() {
        return AmazonSimpleEmailServiceClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).build();
    }

    public static AmazonCognitoIdentity cognitoIdentityClient() {
        return AmazonCognitoIdentityClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).build();
    }

    public static AmazonAthena athenaClient() {
        return AmazonAthenaClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).build();
    }

    public static AWSLogs cloudWatchLogsClient() {
        return AWSLogsClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).build();
    }

    public static AmazonCloudWatchEvents cloudWatchEventsClient() {
        return AmazonCloudWatchEventsClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).build();
    }

    public static AmazonApiGateway gatewayAPIClient() {
        return AmazonApiGatewayClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).build();
    }

    public static AmazonApiGateway gatewayAPIClient(String localAWSProfileName) {
        return AmazonApiGatewayClientBuilder.standard().withCredentials(new ProfileCredentialsProvider(localAWSProfileName)).build();
    }

    public static AmazonRoute53 route53Client() {
        return AmazonRoute53ClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).build();
    }

    public static AmazonSQS sqsClient() {
        return AmazonSQSClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).withRegion(region).build();
    }

    public static AWSGlue awsGlueClient() {
        return AWSGlueClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).build();
    }

    public static AWSCostExplorer awsCostExplorerClient() {
        return AWSCostExplorerClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).withRegion(region).build();
    }

    public static AmazonIdentityManagement iamClient() {
        return AmazonIdentityManagementClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).withRegion(region).build();
    }
}

