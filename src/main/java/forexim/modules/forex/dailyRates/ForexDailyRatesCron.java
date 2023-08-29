package forexim.modules.forex.dailyRates;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import forexim.modules.forex.dbMapper.DbForexDaily;
import forexim.util.FEAwsCredentials;
import forexim.util.FEApiResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.http.client.fluent.Request.Get;


public class ForexDailyRatesCron implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public static void main(String[] args) {
        fetchDailySpotRate();
    }



    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        return fetchDailySpotRate();
    }

    public static APIGatewayProxyResponseEvent fetchDailySpotRate(){
        String baseUrl = "https://fcsapi.com/api-v3/forex/latest?symbol=USD/INR,EUR/INR,GBP/INR,EUR/USD,GBP/USD,JPY/INR,USD/JPY&access_key=IjKl0NmFTiMYcIPjGK0AKDj";
        try {
            String response = Get(baseUrl).execute().returnContent().asString();
            ForexDailyResponse responseModel = new Gson().fromJson(response, ForexDailyResponse.class);
            List<DbForexDaily> dbForexDailies2 = DbForexDaily.loadInputForEntryDate(LocalDate.now().toString());
            List<DbForexDaily> dailyData = new ArrayList<>();
            responseModel.response.forEach(forexDaily -> {
                Map<String, String> dbForexDailies = dbForexDailies2.stream().collect(Collectors.toMap(i -> i.currency, j -> j.currencyId,(i, j) -> i));
                if(!dbForexDailies2.isEmpty()){
                    dailyData.add(new DbForexDaily(dbForexDailies.get(forexDaily.s), forexDaily.o, forexDaily.h, forexDaily.l, forexDaily.c, forexDaily.ch, forexDaily.cp,
                            forexDaily.t, forexDaily.s, LocalDate.now().toString(), forexDaily.tm));
                }
                else
                    dailyData.add(new DbForexDaily(UUID.randomUUID().toString(), forexDaily.o, forexDaily.h, forexDaily.l, forexDaily.c, forexDaily.ch, forexDaily.cp,
                        forexDaily.t, forexDaily.s, LocalDate.now().toString(), forexDaily.tm));
            });
            FEAwsCredentials.dynamoDBMapper().batchSave(dailyData);
            return FEApiResponse.responseEvent(dailyData, HttpStatus.SC_OK);
        } catch (Exception e) {
            return FEApiResponse.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Forex Live Api Structure Got Changed");
        }
    }

    public class ForexDailyResponse{
        List<ForexDaily> response;
    }
    public class ForexDaily{
        public String id;
        public String o;
        public String h;
        public String l;
        public String c;
        public String ch;
        public String cp;
        public String t;
        public String s;
        public String tm;
    }

}
