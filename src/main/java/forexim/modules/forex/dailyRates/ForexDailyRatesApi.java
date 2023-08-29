package forexim.modules.forex.dailyRates;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import forexim.modules.forex.dbMapper.DbForexDaily;
import forexim.util.FEApiResponse;
import org.apache.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ForexDailyRatesApi implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public static void main(String[] args) {
        fetchDailySpotData();
    }
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        return fetchDailySpotData();
    }

    public static APIGatewayProxyResponseEvent fetchDailySpotData(){

        return FEApiResponse.responseEvent(DbForexDaily.loadInputForEntryDate(LocalDate.now().toString()), HttpStatus.SC_OK);
    }

}
