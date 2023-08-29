package forexim.modules.forwardCalculator;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import forexim.modules.forwardCalculator.dbMapper.DBForwardRates;
import forexim.modules.forwardCalculator.dbMapper.DBForwardRates.Expiration;
import forexim.modules.forwardCalculator.dbMapper.DBForwardRates.InputType;
import forexim.util.AwsLog;
import forexim.util.FEAwsCredentials;
import forexim.util.local.TestUtil;

import java.lang.reflect.Type;
import java.sql.Date;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static forexim.modules.forwardCalculator.ForwardCalculatorInput.allCurrencies;
import static forexim.modules.forwardCalculator.ForwardCalculatorInput.convertToInstant;
import static forexim.modules.forwardCalculator.dbMapper.DBForwardRates.Expiration.*;
import static forexim.modules.forwardCalculator.dbMapper.DBForwardRates.loadInputForEntryDate;

// Cron job which automatically feed entry of the day
public class AsyncForwardCalculatorInput  implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    static ZoneId adminEntryRegion = ZoneId.of("Asia/Kolkata");



    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

        allCurrencies.forEach(currency -> {
            try {
                Date entryDate = java.sql.Date.valueOf(LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault()));
                Format formatter = new SimpleDateFormat("yyyy-MM-dd");
                String s = entryDate.toLocalDate().minus(1, ChronoUnit.DAYS).toString();
                s+="T00:00:00.000Z";
                //        DateFormatter formatter = new DateFormatter().
                // 2021-05-21

                Type listType = new com.google.common.reflect.TypeToken<DBForwardRates>() {
                }.getType();
                DBForwardRates dbData = new Gson().fromJson(new Gson().toJson(loadInputForEntryDate(s, currency).get(0)), listType);
                DBForwardRates data = new DBForwardRates(UUID.randomUUID().toString(), dbData.forwardEntries, dbData.currency, entryDate, InputType.Cron);
                Calendar calendar = Calendar.getInstance();
                DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                calendar.add(Calendar.MONTH, 1);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.add(Calendar.DATE, -1);
                LocalDate currentDate = LocalDate.now();
                java.util.Date lastDayOfMonth = calendar.getTime();
                String todayEntryDate = formatter.format(entryDate);
                s+="T00:00:00.000Z";

                int currentMonthValue = currentDate.getMonthValue();
                boolean isTodayLastDayOfMonth = sdf.format(lastDayOfMonth).equals(sdf.format(entryDate));
                boolean islLastMonthOfYear = currentMonthValue == 12;

//                AwsLog.info.log("Today            : " + sdf.format(entryDate));
//                AwsLog.info.log("Last Day of Month: " + sdf.format(lastDayOfMonth));
//                AwsLog.info.log("is today last day of month:"+ isTodayLastDayOfMonth);
//                AwsLog.info.log("is last month of the year: "+ islLastMonthOfYear);
                List<Expiration> allExpirationInput = Arrays.asList( OverNight, Spot, OneWeek, OneMonth, TwoMonths, ThreeMonths, FourMonths, FiveMonths, SixMonths, SevenMonths, EightMonths, NineMonths, TenMonths, ElevenMonths, OneYear, TwoYear, ThreeYear, FourYear, FiveYear );
                data.forwardRateId = UUID.randomUUID().toString();
                data.entryDate = Date.from(convertToInstant(todayEntryDate));
                AtomicInteger i = new AtomicInteger();
                data.forwardEntries.forEach(forwardEntry -> {
                    forwardEntry.startDate = data.entryDate;
                    if(i.get() <= 2){
                        forwardEntry.endDate = i.get() < 2  ? Date.valueOf(entryDate.toLocalDate().plusDays(i.get() + 1)) :  Date.valueOf(entryDate.toLocalDate().plusDays(7));
                    }
                    else if(i.get() > 2 && i.get() < 15) {
                        forwardEntry.endDate = Date.valueOf(entryDate.toLocalDate().plusMonths(i.get() - 2));
                    } else if( i.get() >= 15) {
                        forwardEntry.endDate = Date.valueOf(entryDate.toLocalDate().plusYears(i.get() - 13));
                    }
                    forwardEntry.expiration = allExpirationInput.get(i.get());
                    forwardEntry.noOfDays = (int) TimeUnit.DAYS.convert(Math.abs(forwardEntry.endDate.getTime() - forwardEntry.startDate.getTime()), TimeUnit.MILLISECONDS);;
                    i.getAndIncrement();
                });
                data.inputType = InputType.Cron;
                FEAwsCredentials.dynamoDBMapper().save(data);
            } catch (Exception e){
//                AwsLog.info.log("Date Not Saved For currency:-%s On Date: %s", currency, LocalDate.now().toString());
            }
        });
          return null;
    }

    public static void main(String[] args) {

        System.out.println(new AsyncForwardCalculatorInput().handleRequest(new APIGatewayProxyRequestEvent(), TestUtil.fetchTestContext()));

    }



}
