package forexim.modules.forwardCalculator;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import forexim.modules.admin.dbMapper.DBUser;
import forexim.modules.admin.dbMapper.DBUser.UserRole;
import forexim.modules.forwardCalculator.dbMapper.DBForwardRates;
import forexim.modules.forwardCalculator.dbMapper.DBForwardRates.Expiration;
import forexim.modules.forwardCalculator.dbMapper.DBForwardRates.ForwardEntry;
import forexim.modules.forwardCalculator.dbMapper.DBForwardRates.InputType;
import forexim.util.FEApiRequest;
import forexim.util.FEApiResponse;
import forexim.util.FEAwsCredentials;
import forexim.util.local.TestUtil;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;

import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static forexim.modules.forwardCalculator.dbMapper.DBForwardRates.Expiration.*;
import static forexim.modules.forwardCalculator.dbMapper.DBForwardRates.loadInputForEntryDate;


public class ForwardCalculatorInput implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @SneakyThrows
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        switch (input.getHttpMethod()){
            case "POST":
                if(input.getPath().contains("premia-input")) return savePremiaInput(input, context);
                else saveInputData(input);

        }
        return saveInputData(input);
    }

    private APIGatewayProxyResponseEvent savePremiaInput(APIGatewayProxyRequestEvent input, Context context) throws IllegalAccessException, InstantiationException {
        PremiaInputRequest inputRequest = FEApiRequest.from(input, PremiaInputRequest.class);
        if(inputRequest.entries == null || inputRequest.entries.size() != 19){
            return FEApiResponse.error(HttpStatus.SC_BAD_REQUEST, "entries length should be equal to 19");
        }
        if(inputRequest.currency == null || !allCurrencies.contains(inputRequest.currency)){
            return FEApiResponse.error(HttpStatus.SC_BAD_REQUEST, "Valid currency could be "+"USD/INR "+"EUR/INR "+"GBP/INR "+"EUR/USD "+"GBP/USD "+"JPY/INR "+"USD/JPY");
        }
        java.sql.Date entryDate = inputRequest.entryDate != null ? java.sql.Date.valueOf(LocalDate.parse(inputRequest.entryDate)): java.sql.Date.valueOf(LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault()));
        String s =  LocalDate.parse(entryDate.toString()).toString();
        s+="T00:00:00.000Z";
       //        DateFormatter formatter = new DateFormatter().
       // 2021-05-21
        List<Expiration> allExpirationInput = Arrays.asList( OverNight, Spot, OneWeek, OneMonth, TwoMonths, ThreeMonths, FourMonths, FiveMonths, SixMonths, SevenMonths, EightMonths, NineMonths, TenMonths, ElevenMonths, OneYear, TwoYear, ThreeYear, FourYear, FiveYear );
        List<DBForwardRates> temp = loadInputForEntryDate(s, inputRequest.currency);
        DBForwardRates data = temp != null && !temp.isEmpty() ? temp.get(0) : null;
        AtomicInteger i = new AtomicInteger();
        if( data != null && data.forwardEntries != null && !data.forwardEntries.isEmpty()){
            data.forwardEntries.forEach(forwardEntry -> {
                forwardEntry.startDate = entryDate;
                if(i.get() <= 2){
                    forwardEntry.endDate = i.get() < 2  ? java.sql.Date.valueOf(entryDate.toLocalDate().plusDays(i.get() + 1)) :  java.sql.Date.valueOf(entryDate.toLocalDate().plusDays(7));
                }
                else if(i.get() > 2 && i.get() < 15) {
                    forwardEntry.endDate = java.sql.Date.valueOf(entryDate.toLocalDate().plusMonths(i.get() - 2));
                } else if( i.get() >= 15) {
                    forwardEntry.endDate = java.sql.Date.valueOf(entryDate.toLocalDate().plusYears(i.get() - 13));
                }
                forwardEntry.premiaAsk = inputRequest.entries.get(i.get()).premiaAsk;
                forwardEntry.premiaBid = inputRequest.entries.get(i.get()).premiaBid;
                forwardEntry.expiration = allExpirationInput.get(i.get());
                forwardEntry.noOfDays = (int) TimeUnit.DAYS.convert(Math.abs(forwardEntry.endDate.getTime() - forwardEntry.startDate.getTime()), TimeUnit.MILLISECONDS);
                i.getAndIncrement();
            });
            data.inputType = InputType.Manual;
            FEAwsCredentials.dynamoDBMapper().save(data);
        }else {
            data = new DBForwardRates();
            data.forwardRateId = UUID.randomUUID().toString();
            data.entryDate = entryDate;
            data.currency = inputRequest.currency;
            data.inputType = InputType.Manual;
            data.forwardEntries = new ArrayList<>();
            for (PremiaObject forwardEntry : inputRequest.entries) {
                ForwardEntry entry1 = new ForwardEntry();
                entry1.premiaBid = forwardEntry.premiaBid;
                entry1.premiaAsk = forwardEntry.premiaAsk;
                entry1.expiration = allExpirationInput.get(i.get());
                entry1.startDate = entryDate;
                if (i.get() <= 2) {
                    entry1.endDate = i.get() < 2 ? java.sql.Date.valueOf(entryDate.toLocalDate().plusDays(i.get() + 1)) : java.sql.Date.valueOf(entryDate.toLocalDate().plusDays(7));
                } else if (i.get() > 2 && i.get() < 15) {
                    entry1.endDate = java.sql.Date.valueOf(entryDate.toLocalDate().plusMonths(i.get() - 2));
                } else if (i.get() >= 15) {
                    entry1.endDate = java.sql.Date.valueOf(entryDate.toLocalDate().plusYears(i.get() - 13));
                }
                entry1.noOfDays = (int) TimeUnit.DAYS.convert(Math.abs(entry1.endDate.getTime() - entry1.startDate.getTime()), TimeUnit.MILLISECONDS);
                data.forwardEntries.add(entry1);
                i.getAndIncrement();
            }
            FEAwsCredentials.dynamoDBMapper().save(data);
        }
        return FEApiResponse.responseEvent(data, HttpStatus.SC_OK);
    }

    public static APIGatewayProxyResponseEvent saveInputData(APIGatewayProxyRequestEvent input) throws InstantiationException, IllegalAccessException {
        ForwardCalculatorInputRequest request = FEApiRequest.from(input, ForwardCalculatorInputRequest.class);
        if( request.userId == null) return FEApiResponse.error(HttpStatus.SC_BAD_REQUEST, "Invalid Request");
        DBUser dbUser = DBUser.load(request.userId);
        if(dbUser == null || !dbUser.userRole.equals(UserRole.ADMIN)){
            return FEApiResponse.error(HttpStatus.SC_UNAUTHORIZED, "Not Authorized To Input Data");
        }
        DBForwardRates forwardRates = new DBForwardRates();
        forwardRates.forwardRateId = UUID.randomUUID().toString();
        List<ForwardEntry> forwardEntries = new ArrayList<>();
        request.entryList.stream().map(entry -> forwardEntries.add(new ForwardEntry(Date.from(convertToInstant(entry.startDate)), Date.from(convertToInstant(entry.endDate)), entry.noOfDays, entry.premia, null, null))).collect(Collectors.toList());
        forwardRates.forwardEntries = forwardEntries;
        forwardRates.currency = request.currency;
        forwardRates.entryDate = Date.from(convertToInstant(request.entryDate));
        FEAwsCredentials.dynamoDBMapper().save(forwardRates);
        return FEApiResponse.success("Data Saved");
    }

    public static class PremiaInputRequest extends FEApiRequest{
        public String entryDate; // yyyy-mm-dd
        public String currency;
        public List<PremiaObject> entries;
    }

    public static class PremiaObject{
        public Double premiaBid;
        public Double premiaAsk;
    }

    public static class ForwardCalculatorInputRequest extends FEApiRequest {
        public String currency;
        public String entryDate;
        public List<ForwardInput> entryList;
        public String userId;
        public String accessToken;
    }

    public static class ForwardInput{
        public String startDate;
        public String  endDate;
        public Integer noOfDays;
        public Double premia;
    }

    public static Instant convertToInstant(String date){
        try {
            LocalDate localDate = LocalDate.parse(date); // yyyy-mm-dd
            LocalDateTime localDateTime = localDate.atStartOfDay();
            Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
            return instant;
        }catch (Exception e){
            try {
                return Instant.parse(date);
            } catch (Exception e1){
                return  null;
            }
        }
    }



    public static Instant convertInstantToStartDateInstant(Instant inputDate){
        try {
            LocalDate localDate = LocalDate.ofInstant(inputDate, ZoneId.systemDefault()); // yyyy-mm-dd
            LocalDateTime localDateTime = localDate.atStartOfDay();
            Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
            return instant;
        }catch (Exception e){
            try {
                return Instant.now();
            } catch (Exception e1){
                return  null;
            }
        }
    }
    public static List<String>  allCurrencies =  Arrays.asList("USD/INR","EUR/INR","GBP/INR","EUR/USD","GBP/USD","JPY/INR","USD/JPY");

    public static void main(String[] args) throws IllegalAccessException, InstantiationException {
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.setBody("{\n" +
                "  \"entryDate\": \"2021-10-25\",\n" +
                "  \"currency\":\"USD/INR\",\n" +
                "  // 19 premiaBidAskPair for 19 entries\n" +
                "  \"entries\": [\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    },\n" +
                "    {\n" +
                "      \"premiaBid\": 122333,\n" +
                "      \"premiaAsk\": 122333\n" +
                "    }\n" +
                "  ]\n" +
                "}");
        requestEvent.setHttpMethod("POST");
        System.out.println(new ForwardCalculatorInput().savePremiaInput(requestEvent, TestUtil.fetchTestContext()));
    }
}


