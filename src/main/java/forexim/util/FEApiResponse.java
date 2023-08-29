package forexim.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.http.HttpStatus;

import java.util.HashMap;

public class FEApiResponse {

    public static APIGatewayProxyResponseEvent createSuccess(String resourceId, Object responseBody) {
        HashMap<String, String> headers = defaultHeaders();
        headers.put("location", resourceId);
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent().withHeaders(defaultHeaders()).withStatusCode(HttpStatus.SC_CREATED);
        if (responseBody != null) { responseEvent = responseEvent.withBody(FEGson.gson.toJson(responseBody)); }
        return responseEvent;
    }

    public static APIGatewayProxyResponseEvent success(String message) {
        return responseEvent(new Message(message), HttpStatus.SC_OK);
    }

    public static APIGatewayProxyResponseEvent error(Context context, int httpStatus, String message, Exception exception) {
        AwsLog.error.log(context, exception, message);
        return responseEvent(new ErrorMessage(message), httpStatus);
    }

    public static APIGatewayProxyResponseEvent error(int httpStatus, String message) {
//        AwsLog.error.log(message);
        return responseEvent(new ErrorMessage(message), httpStatus);
    }

    public static APIGatewayProxyResponseEvent error(Context context, int httpStatus) {
//        AwsLog.error.log(context, String.format("%d", httpStatus));
        return responseEvent(null, httpStatus);
    }

    public static APIGatewayProxyResponseEvent responseEvent(Object response, int httpStatusCode) {
        String responseString = response != null ? FEGson.gson.toJson(response) : null;
        return new APIGatewayProxyResponseEvent().withHeaders(defaultHeaders()).withStatusCode(httpStatusCode).withBody(responseString);
    }

    public static APIGatewayProxyResponseEvent responseEventForHTML(String response, int httpStatusCode) {
        return new APIGatewayProxyResponseEvent().withHeaders(defaultHeadersForHTML()).withStatusCode(httpStatusCode).withBody(response);
    }

    private static class ErrorMessage {
        String message;
        public ErrorMessage(String message) { this.message = message; }
    }

    private static class Message {
        String message;
        public Message(String message) { this.message = message; }
    }

    public static HashMap<String,String> defaultHeaders() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        headers.put("Content-Type", "application/json");
        return headers;
    }

    public static HashMap<String,String> defaultHeadersForHTML() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        headers.put("Content-Type", "text/html");
        return headers;
    }
}
