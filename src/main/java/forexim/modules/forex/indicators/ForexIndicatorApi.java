package forexim.modules.forex.indicators;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import forexim.modules.forex.dbMapper.DBForexIndicators;
import forexim.util.FEApiResponse;
import org.apache.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ForexIndicatorApi implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        return fetchLastSevenDaysForexIndicator(input);
    }

    public static APIGatewayProxyResponseEvent fetchLastSevenDaysForexIndicator(APIGatewayProxyRequestEvent input){

        Map<LocalDateTime, List<DBForexIndicators>> dateIndicatorMap = DBForexIndicators.fetchAll().stream().filter(i -> i.createTime.after(Date.from(Instant.now().minus(7, ChronoUnit.DAYS)))).collect(Collectors.groupingBy(i -> LocalDate.ofInstant(i.createTime.toInstant(), ZoneId.systemDefault()).atStartOfDay()));
        return FEApiResponse.responseEvent(dateIndicatorMap, HttpStatus.SC_OK);

    }

    public static void main(String[] args) {
        fetchLastSevenDaysForexIndicator(new APIGatewayProxyRequestEvent());
    }

}
