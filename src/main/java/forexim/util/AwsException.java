package forexim.util;

import com.amazonaws.services.lambda.runtime.Context;

public class AwsException extends RuntimeException {
    public AwsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        AwsLog.error.log(this, message);
    }

    public AwsException(Exception exception) {
        this(null, exception);
    }

    public AwsException(Context context, Exception exception) {
        super(exception.getMessage(), exception.getCause(), true, true);
        super.setStackTrace(exception.getStackTrace());
//        AwsLog.error.log(context, this);
    }

    public AwsException(String messageValue) {
        this(null, messageValue);
    }

    public AwsException(Context context, String messageValue) {
        super(messageValue);
        super.setStackTrace(Thread.currentThread().getStackTrace());
        AwsLog.error.log(context, this, messageValue);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }

}
