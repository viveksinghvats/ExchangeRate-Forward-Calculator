package forexim.util;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UnknownFormatConversionException;

import com.google.gson.Gson;

public enum AwsLog {
    fatal, error, warning, info, debug;

    public void log(String message, Object... args)                      { log(null, null, message, args); }
    public void log(Exception exception)                                 { log(null, exception, null, null); }
    public void log(Context context, Exception exception)                { log(context, exception, null, null); }
    public void log(Exception exception, String message, Object... args) { log(null, exception, message, args); }
    public void log(Context context, String message, Object... args)     { log(context, null, message, args); }

    public void log(Context context, Exception exception, String message, Object... args) {
        new AwsLogger(this, context, exception, message, args).log();
    }

    private static class AwsLogger {
        public AwsLog   severity;
        public Context context;
        public String  message;
        public String  exception;

        public AwsLogger() { }

        public AwsLogger(AwsLog severity, Context context, Exception exception, String messageFormat, Object... args) {
            this.severity  = severity;
            this.context   = context;
            if (exception != null)     { this.exception = niceStackTrace(exception); }
            if (messageFormat != null && (args == null || args.length == 0)) {
                this.message = messageFormat;
            } else if (messageFormat != null) {
                // Catch the exception where the format specifier has an error, for example, when %d is used for a string.
                try { this.message = String.format(messageFormat, args); } catch (UnknownFormatConversionException e) { };
            }
        }

        void log() {
            if (loggingDisabled()) { return; }
            String messageJson = new Gson().toJson(this, AwsLogger.class);
            System.out.println(messageJson);
        }
        
        private boolean loggingDisabled() {
            String loggingEnabled = System.getenv("loggingDisabled");
            return loggingEnabled != null && loggingEnabled.equalsIgnoreCase("TRUE");
        }

        private String niceStackTrace(Exception exception) {
            if (exception == null) { return null; }
            StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        }

    }
}
