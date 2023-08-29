package forexim.util;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.maven.shared.invoker.*;
import org.joda.time.Instant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AwsLambdaUpload {

    private static AwsServerType serverType              = AwsServerType.DEV;
    private static List<String> lambdaFunctionsToUpload = Arrays.asList(
//            "ForexIndicatorsCron", "ForexIndicatorApi", "ForexDailyRatesApi", "ForexDailyRatesCron"
//            "ForexLiveSpotRates"
            "AsyncForwardCalculatorInput", "ForwardCalculator", "ForwardCalculatorInput", "ForexLiveSpotRates", "ForexIndicatorApi", "ForexIndicatorsCron", "ForexDailyRatesApi", "ForexDailyRatesCron", "UserSignUp", "UserSignIn"
    );
    private static String projectPOMArtifactId = "DigiFun";
    private static String projectPOMVersion   = "1.0.0.";
    private static String projectPOMPackaging = "jar";
    private static String mavenPackagedZipFileName = projectPOMArtifactId + "-" + projectPOMVersion + projectPOMPackaging;
    private static String mavenPackagedZipFilePath = "target/" + mavenPackagedZipFileName;

    private static String lambdaCodeZipFileName  = Instant.now() + ".zip";
    private static String aliasName = serverType.getCurrentAPIVersion(); //"$latest";
    private static boolean isVersionEnabled = true;

    public static void main(String[] args) throws IOException, InterruptedException, MavenInvocationException, ParseException {
        System.out.println("Working On: " + serverType.toString().toUpperCase());
        createLambdaCodeZipFile(aliasName);
        uploadZipFileToS3(lambdaCodeZipFileName, mavenPackagedZipFilePath);
        updateCodeAndAlias();
//		 updateLayersAndCheckFunctionConfiguration();
        System.out.println("Done");
    }

    private static void createLambdaCodeZipFile(String fileName)
            throws IOException, InterruptedException, MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(Arrays.asList("package"));
        request.setBatchMode(true);
        request.setBaseDirectory(new File(System.getProperty("user.dir")));
        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File(System.getenv("HOME") + "/Applications/apache-maven-3.8.1/"));
        InvocationResult result = invoker.execute(request);
        if (result.getExitCode() != 0) {
            throw new AwsException("Could not create lambda package file.");
        }
//        		PrintLogUtil.print(fileName, new File(System.getProperty("user.dir")).toPath());
    }

    private static void uploadZipFileToS3(String fileName, String mvnRelativePath) throws FileNotFoundException {
        System.out.println("Uploading " + fileName);
        AmazonS3 awsLambdaClient = AmazonS3ClientBuilder.standard().withRegion(Regions.AP_SOUTH_1)
                .withCredentials(new AWSStaticCredentialsProvider(
                        new ProfileCredentialsProvider(serverType.getAwsCredentialsProfileName()).getCredentials()))
                .build();
        File file = new File(System.getProperty("home.dir"), mvnRelativePath);
        double fileSize = file.length() / (1024.0 * 1024.0);
        System.out.printf("Upload Size: %.2f MB \n", fileSize);
        System.out.println(file.canRead());
        awsLambdaClient.putObject(serverType.getLambdaFunctionBucketName(), fileName, file);
        System.out.println("Uploaded " + fileName);
    }

    private static void updateCodeAndAlias() {
        AWSLambda awsLambdaClient = AWSLambdaClientBuilder.standard().withRegion(Regions.AP_SOUTH_1)
                .withCredentials(new AWSStaticCredentialsProvider(
                        new ProfileCredentialsProvider(serverType.getAwsCredentialsProfileName()).getCredentials()))
                .build();
        List<FunctionConfiguration> serverLambdaFunctions = listAllFunctions(awsLambdaClient);

        System.out.println("Total Lambda Functions Count: " + serverLambdaFunctions.size());

        List<String> lambdaFunctionToUpdate = serverLambdaFunctions.stream().filter(f -> lambdaFunctionsToUpload.contains(f.getFunctionName())).map(FunctionConfiguration::getFunctionName).collect(Collectors.toList());
        System.out.println("Update Lambda Functions  Count: " + serverLambdaFunctions.size());
        System.out.println("Lambda Functions To Update: " + lambdaFunctionToUpdate.size());
        lambdaFunctionToUpdate.forEach(System.out::println);
        lambdaFunctionToUpdate.forEach(lambdaFunction -> {
            System.out.print("Lambda function '" + lambdaFunction + "' ");
            UpdateFunctionCodeRequest updateFunctionCodeRequest = new UpdateFunctionCodeRequest()
                    .withFunctionName(lambdaFunction).withPublish(false).withS3Bucket(serverType.getLambdaFunctionBucketName())
                    .withS3Key(lambdaCodeZipFileName);
            UpdateFunctionCodeResult updateCodeResult = awsLambdaClient.updateFunctionCode(updateFunctionCodeRequest);
            System.out.print("code updated with version : " + updateCodeResult.getVersion() + ".");
            if (isVersionEnabled) {
                PublishVersionRequest publishRequest = new PublishVersionRequest().withFunctionName(updateCodeResult.getFunctionName());
                PublishVersionResult publishedResult = awsLambdaClient.publishVersion(publishRequest);
                System.out.print(" updated version ." + publishedResult.getVersion());
                try {
                    CreateAliasRequest createAliasRequest = new CreateAliasRequest().withFunctionName(publishedResult.getFunctionName()).withFunctionVersion(publishedResult.getVersion()).withName(aliasName);
                    awsLambdaClient.createAlias(createAliasRequest);
                    System.out.print(" Created alias " + aliasName + " Version: " + publishedResult.getVersion());
                } catch(ResourceConflictException e) {
                    UpdateAliasRequest updateAliasRequest = new UpdateAliasRequest().withFunctionName(lambdaFunction).withFunctionVersion(publishedResult.getVersion()).withName(aliasName);
                    awsLambdaClient.updateAlias(updateAliasRequest);
                    System.out.print(" Updated alias " + aliasName  + " , Version: " + updateCodeResult.getVersion());
                }
            }
            System.out.print("\n");
        });
    }

    public static List<FunctionConfiguration> listAllFunctions(AWSLambda awsLambdaClient) {
        ArrayList<FunctionConfiguration> serverLambdaFunctions = new ArrayList<>();
        String nextMarker = null;
        boolean isDone = false;
        while (!isDone) {
            ListFunctionsResult listFunctionsResult = (nextMarker != null)
                    ? awsLambdaClient.listFunctions(new ListFunctionsRequest().withMarker(nextMarker))
                    : awsLambdaClient.listFunctions();
            serverLambdaFunctions.addAll(listFunctionsResult.getFunctions());
            nextMarker = listFunctionsResult.getNextMarker();
            System.out.println("Size: " + " " + listFunctionsResult.getFunctions().size());
            isDone = (nextMarker == null);
        }
        return serverLambdaFunctions;
    }

}
