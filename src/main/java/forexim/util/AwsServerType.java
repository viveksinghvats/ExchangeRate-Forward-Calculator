package forexim.util;

public enum AwsServerType {
    DEV, TEST, DEPLOY, PROD;

    public String getCurrentAPIVersion() {
        switch (this) {
            case DEV:
            case TEST:
            case PROD:
            case DEPLOY:
                return "v4";
        }
        return null;
    }

    // TODO config profile names over aws
    public String getAwsCredentialsProfileName() {
        switch (this) {
            case DEV: 	            return "forexim-dev";
            case TEST:              return "ForExim-TEST";
            case PROD:	            return "ForExim-PROD";
            case DEPLOY:            return "SK-ForExim-DEPLOY";
        }
        return null;
    }

    // TODO config bucket Name
    public String getLambdaFunctionBucketName() {
        switch (this) {
            case DEV: 	    return "fe-bucket-dev-vk";
            case TEST:      return "lambda-Test-bucket-";
            case PROD:	    return "lambda-Prod-bucket-";
            case DEPLOY:    return "lambda-Deploy-bucket";
        }
        return null;
    }

}
