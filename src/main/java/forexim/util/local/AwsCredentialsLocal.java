package forexim.util.local;

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
import forexim.util.AwsServerType;

class AwsCredentialsLocal {

    public static String localProfileName = AwsServerType.DEV.getAwsCredentialsProfileName();
    public static Regions region = Regions.US_EAST_1;


    private static DynamoDBMapper cachedDynamoDBMapper;
    public static DynamoDBMapper dynamoDBMapper() {
        if (cachedDynamoDBMapper == null) {
            cachedDynamoDBMapper = new DynamoDBMapper(dynamoDBClient());
        }
        return cachedDynamoDBMapper;
    }

    public static DynamoDB dynamoDB() {
        return new DynamoDB(dynamoDBClient());
    }

    private static AWSStaticCredentialsProvider cachedAWSStaticCredentialsProvider;
    public static AWSStaticCredentialsProvider getAWSCredentialProvider() {
        if (cachedAWSStaticCredentialsProvider == null) {
            cachedAWSStaticCredentialsProvider = new AWSStaticCredentialsProvider(new ProfileCredentialsProvider(localProfileName)
                    .getCredentials());
        }
        return cachedAWSStaticCredentialsProvider;
    }

    public static AWSLambda lambdaClient() {
        return AWSLambdaClientBuilder.standard().withRegion(region).withCredentials(getAWSCredentialProvider()).build();
    }

    public static AmazonDynamoDB dynamoDBClient() {
        return AmazonDynamoDBClientBuilder.standard().withCredentials(getAWSCredentialProvider()).withRegion(region).build();
    }

    public static AmazonSNS snsClient() {
        return AmazonSNSClientBuilder.standard().withCredentials(getAWSCredentialProvider()).withRegion(region).build();
    }

    public static AmazonS3 s3Client() {
        return AmazonS3ClientBuilder.standard().withCredentials(getAWSCredentialProvider()).withRegion(region).build();
    }

    public static AmazonSimpleEmailService sesClient() {
        return AmazonSimpleEmailServiceClientBuilder.standard().withCredentials(getAWSCredentialProvider()).build();
    }

    public static AmazonCognitoIdentity cognitoIdentityClient() {
        return AmazonCognitoIdentityClientBuilder.standard().withCredentials(getAWSCredentialProvider()).build();
    }

    public static AmazonAthena athenaClient() {
        return AmazonAthenaClientBuilder.standard().withCredentials(getAWSCredentialProvider()).withRegion(region).build();
    }

    //		public AmazonIdentityManagement amazonIdentityClient() {
    //			return AmazonIdentityManagementClientBuilder.standard().withCredentials(getAWSCredentialProvider()).build();
    //		}

    /** Returns an AWSLogs client object. This can be used to work with CloudWatch Logs. Note that CloudWatch Logs and CloudWatch Alarms and Events
     *  are really two separate services and are just branded under the CloudWatch name. They also have separate Java clients - AWSLogs for logs
     *  and AmazonCloudWatch for alarms and events. */
    public static AWSLogs cloudWatchLogsClient() {
        return AWSLogsClientBuilder.standard().withCredentials(getAWSCredentialProvider()).build();
    }

    public static AmazonCloudWatchEvents cloudWatchEvetnsClient() {
        return AmazonCloudWatchEventsClientBuilder.standard().withCredentials(getAWSCredentialProvider()).build();
    }

    public static AmazonApiGateway gatewayAPIClient() {
        return AmazonApiGatewayClientBuilder.standard().withCredentials(getAWSCredentialProvider()).build();
    }

    public static AmazonRoute53 route53Client() {
        return AmazonRoute53ClientBuilder.standard().withCredentials(getAWSCredentialProvider()).build();
    }

    public static AmazonSQS sqsClient() {
        return AmazonSQSClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).withRegion(region).build();
    }

    public static AWSGlue awsGlueClient() {
        return AWSGlueClientBuilder.standard().withCredentials(getAWSCredentialProvider() ).build();
    }
    public static AWSCostExplorer awsCostExplorerClient() {
        return AWSCostExplorerClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).withRegion(region).build();
    }

    public static AmazonIdentityManagement iamClient() {
        return AmazonIdentityManagementClientBuilder.standard().withCredentials(new EnvironmentVariableCredentialsProvider()).withRegion(region).build();
    }
}
