package forexim.util;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.google.gson.Gson;

public class FEApiRequest {
    public boolean warmUp;
    public String sourceIP;
    public String userAgent;
    public String stage;
    public String authorizationToken;
    public Boolean forceSwitchToProd;

    public String resourceId;

    public static <T extends FEApiRequest> T from(APIGatewayProxyRequestEvent event, Class<T> classOfT) throws IllegalAccessException, InstantiationException {
        HttpMethod httpMethod = HttpMethod.valueOf(event.getHttpMethod());
        String jsonString = (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE) ?	// GET, DELETE: Parameters comes in query string.
                event.getQueryStringParameters() == null ? null : new Gson().toJson(event.getQueryStringParameters()) :
                event.getBody();					// POST, PUT, PATCH: Request comes in body.
        T request = jsonString != null ? new Gson().fromJson(jsonString, classOfT) : classOfT.newInstance();
        if (event.getPathParameters() != null) { request.resourceId = event.getPathParameters().get("urlpath"); }
        return request;
    }
}
