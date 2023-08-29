package forexim.modules.forex.indicators;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;

import forexim.assets.ForEximConstants;
import forexim.modules.forex.dbMapper.DBForexIndicators;
import forexim.modules.forex.dbMapper.DBForexIndicators.Root;
import forexim.util.FEApiResponse;
import forexim.util.FEAwsCredentials;
import org.apache.http.HttpStatus;

import java.util.UUID;

import static org.apache.http.client.fluent.Request.Get;


public class ForexIndicatorsCron implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        fetchForexIndicatorsData("USD/INR");
        fetchForexIndicatorsData("EUR/INR");
        fetchForexIndicatorsData("GBP/INR");
        fetchForexIndicatorsData("EUR/USD");
        fetchForexIndicatorsData("GBP/USD");
        return FEApiResponse.success("Saved Data For All currencies");
    }

    public static APIGatewayProxyResponseEvent fetchForexIndicatorsData(String currency){
        String baseUrl = "https://fcsapi.com/api-v3/forex/pivot_points?symbol="+currency+"&period=1d&access_key="+ ForEximConstants.accessKey;
        try {
            String response = Get(baseUrl).execute().returnContent().asString();
            Root responseModel = new Gson().fromJson(response, Root.class);
            DBForexIndicators  forexIndicators = new DBForexIndicators();
            forexIndicators.indicatorId  = UUID.randomUUID().toString();
            forexIndicators.info         = responseModel.info;
            forexIndicators.classic      = responseModel.response.pivot_point.classic;
            forexIndicators.fibonacci    = responseModel.response.pivot_point.fibonacci;
            forexIndicators.camarilla    = responseModel.response.pivot_point.camarilla;
            forexIndicators.woodie       = responseModel.response.pivot_point.woodie;
            forexIndicators.demark       = responseModel.response.pivot_point.demark;
            forexIndicators.overall      = responseModel.response.overall;
            forexIndicators.currency     = currency;
            FEAwsCredentials.dynamoDBMapper().save(forexIndicators);
            return FEApiResponse.responseEvent(forexIndicators, HttpStatus.SC_OK);
        } catch (Exception e) {
            return FEApiResponse.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Forex Live Api Structure Got Changed");
        }

    }

    public static void main(String[] args) {
        System.out.println(fetchForexIndicatorsData("USD/INR"));
    }

}
