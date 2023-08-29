package forexim.modules.forwardCalculator;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;

import forexim.assets.ForEximConstants;
import forexim.modules.forwardCalculator.dbMapper.DBForwardClientInput;
import forexim.modules.forwardCalculator.dbMapper.DBForwardClientInput.InputType;
import forexim.modules.forwardCalculator.dbMapper.DBForwardRates;
import forexim.modules.forwardCalculator.dbMapper.DBForwardRates.ForwardEntry;
import forexim.util.FEApiRequest;
import forexim.util.FEApiResponse;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static forexim.modules.forwardCalculator.ForwardCalculatorInput.allCurrencies;
import static org.apache.http.client.fluent.Request.Get;

public class ForwardCalculator implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @SneakyThrows
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        return FEApiResponse.error(HttpStatus.SC_INTERNAL_SERVER_ERROR,"error");
//        switch (input.getHttpMethod()){
//            case "POST": return feedClientInput(input);
//            case "GET":
//                if(input.getPath().contains("dashboard")) return fetchAllForwardValues(input, null);
//                else if( input.getPath().contains("importer-exporter")) return fetchImportExportTable(input);
//            default: return FEApiResponse.error(HttpStatus.SC_BAD_REQUEST, "Invalid Methods Called");
//        }
    }

    private APIGatewayProxyResponseEvent feedClientInput(APIGatewayProxyRequestEvent input) throws InstantiationException, IllegalAccessException {
        ClientForwardInputRequest request = ClientForwardInputRequest.from(input, ClientForwardInputRequest.class);
        if(request.type == null || request.currencyPair == null || request.forwardDate == null){
            return FEApiResponse.error(HttpStatus.SC_BAD_REQUEST, "notional, type, currencyPair and forwardDate are required fields");
        }
        DBForwardClientInput forwardClientInput = new DBForwardClientInput(UUID.randomUUID().toString(), request.userId, request.entryDate != null ? Date.from(ForwardCalculatorInput.convertToInstant(request.entryDate)) : Date.from(Instant.now()), request.notional, request.type, request.currencyPair, Date.from(ForwardCalculatorInput.convertToInstant(request.forwardDate)));
        ClientForwardResponse response  = new Gson().fromJson(fetchAllForwardValues(input, forwardClientInput).getBody(), ClientForwardResponse.class);
//        FEAwsCredentials.dynamoDBMapper().save(forwardClientInput);
        return FEApiResponse.responseEvent(response, HttpStatus.SC_OK);
    }

    private APIGatewayProxyResponseEvent fetchAllForwardValues(APIGatewayProxyRequestEvent input, DBForwardClientInput clientInput) {
        List<DBForwardClientInput> dbForwardClientInputs = clientInput != null ? Collections.singletonList(clientInput) : DBForwardClientInput.fetchAll();
        Map<String, SpotResponse> currencySpotMap = fetchLiveSpotForAll();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd");
        String entryDate = format.format(new Date())+"T00:00:00.000Z";
        Map<String, List<DBForwardRates>> forwardRatesByCurrency = DBForwardRates.fetchAll().stream().filter(i -> i.entryDate != null && i.currency != null).collect(Collectors.groupingBy(i -> i.currency));
        List<ClientForwardResponse> response = new ArrayList<>();
        dbForwardClientInputs.forEach(dbForwardClientInput -> {
            if(!dbForwardClientInput.forwardDate.after(new Date())) return;   // should be grater that spot date
            if(forwardRatesByCurrency.containsKey(dbForwardClientInput.currencyPair)){
                forwardRatesByCurrency.get(dbForwardClientInput.currencyPair).stream().filter(i -> i.entryDate.equals(dbForwardClientInput.entryDate)).collect(Collectors.toList()).forEach(dbForwardRates -> {
                    ForwardEntry firstValidEntry = null ;
                    ForwardEntry secondNextEntry = null;
                    for(int i = 0; i < dbForwardRates.forwardEntries.size(); i++){
                        if(dbForwardRates.forwardEntries.get(i).startDate.before(dbForwardClientInput.forwardDate) && dbForwardRates.forwardEntries.get(i).endDate.after(dbForwardClientInput.forwardDate)){
                            if(i != 0) {
                                firstValidEntry = dbForwardRates.forwardEntries.get(i -1);
                                secondNextEntry = dbForwardRates.forwardEntries.get(i);
                                break;
                            }
                        }
                    }
                    if( firstValidEntry != null && secondNextEntry != null) {
                        ClientForwardResponse forwardResponse = new ClientForwardResponse();
                        forwardResponse.userId = dbForwardClientInput.userId;
                        forwardResponse.notional = dbForwardClientInput.notional;
                        forwardResponse.type = dbForwardClientInput.type;
                        forwardResponse.currencyPair = dbForwardClientInput.currencyPair;
                        forwardResponse.forwardDate = dbForwardClientInput.forwardDate.toString();
                        SpotResponse liveSpotResponse = currencySpotMap.get(dbForwardClientInput.currencyPair);
                        forwardResponse.currentSpot = (Double.parseDouble(liveSpotResponse.h) + Double.parseDouble(liveSpotResponse.l)) / 2;
                        try {
                            forwardResponse.forwardRate = (((((double) (dateDifference(dbForwardClientInput.forwardDate, dbForwardClientInput.entryDate) - dateDifference(firstValidEntry.endDate, firstValidEntry.startDate))
                                    / ((double) (dateDifference(secondNextEntry.endDate, secondNextEntry.startDate) - dateDifference(firstValidEntry.endDate, firstValidEntry.startDate)))) * (secondNextEntry.premiaBid - firstValidEntry.premiaBid) + firstValidEntry.premiaBid) / 100) + Double.parseDouble(liveSpotResponse.l));
                        } catch (Exception e){
                         return;
                        }
                        forwardResponse.forwardValue = forwardResponse.forwardRate * dbForwardClientInput.notional;
                        forwardResponse.hedgingCost = ((forwardResponse.forwardRate / forwardResponse.currentSpot) - 1) * 100;
                        response.add(forwardResponse);
                    }
                });
            }
        });
        if(clientInput != null && !response.isEmpty()){
            return FEApiResponse.responseEvent(response.get(0), HttpStatus.SC_OK);
        }
        return FEApiResponse.responseEvent(response, HttpStatus.SC_OK);
    }

    private APIGatewayProxyResponseEvent fetchImportExportTable(APIGatewayProxyRequestEvent input) {
        List<ExportImportResponse> responses = new ArrayList<>();
       allCurrencies.forEach(currencyValue -> {
           Map<String, List<DBForwardRates>> forwardRatesByCurrency = DBForwardRates.loadInputForEntryDate(LocalDate.now()+"T00:00:00.000Z", currencyValue).stream().filter(i -> i.entryDate != null && i.currency != null).collect(Collectors.groupingBy(i -> i.currency));
           Map<String, SpotResponse> currencySpotMap = fetchLiveSpotForAll();
           forwardRatesByCurrency.forEach((currency, forwardRates) -> {
               if(forwardRatesByCurrency.containsKey(currency) && currencySpotMap.containsKey(currency)){
                   forwardRatesByCurrency.get(currency).forEach(forwardRate -> {
                       ExportImportResponse importResponse = new ExportImportResponse();
                       importResponse.currency = currency;
                       importResponse.entryDate = forwardRate.entryDate.toString();
                       importResponse.data = new ArrayList<>();
                       if(forwardRate.forwardEntries != null && !forwardRate.forwardEntries.isEmpty()){
                           forwardRate.forwardEntries.forEach(forwardEntry -> {
                               ImportExport temp = new ImportExport();
                               temp.expiryDate = forwardEntry.endDate != null ? forwardEntry.endDate.toString() : null;
                               temp.premia = forwardEntry.premiaBid != null && forwardEntry.premiaAsk != null ? ((forwardEntry.premiaBid+forwardEntry.premiaAsk)/2)+ "" : null;
                               SpotResponse spotResponse = currencySpotMap.get(currency);
                               if(forwardEntry.premiaBid == null) return;
                               temp.exporters = (Double.parseDouble(spotResponse.l) + (forwardEntry.premiaBid / 100.0));
                               temp.importers = (Double.parseDouble(spotResponse.h) + (forwardEntry.premiaBid / 100.0));
                               importResponse.data.add(temp);
                           });
                       }
                       if(!importResponse.data.isEmpty()) {responses.add(importResponse);}
                   });
               }
           });
       });
        return FEApiResponse.responseEvent(responses, HttpStatus.SC_OK);
    }

    private SpotResponse fetchLiveSpotForCurrency(String currency){
        String baseUrl = "https://fcsapi.com/api-v3/forex/latest?symbol=USD/INR,EUR/INR,GBP/INR,EUR/USD,GBP/USD&access_key="+ ForEximConstants.accessKey;
        try {
            String response = Get(baseUrl).execute().returnContent().asString();
            SpotRateResponse spotRateResponse = new Gson().fromJson(response, SpotRateResponse.class);
            Map<String, SpotResponse> currencySpotMap = spotRateResponse.response.stream().collect(Collectors.toMap(i -> i.s, Function.identity()));
            if(currencySpotMap.containsKey(currency)) return currencySpotMap.get(currency);
            else return new SpotResponse();
        } catch (IOException e) {
            return new SpotResponse();
        }
    }

    private Map<String, SpotResponse> fetchLiveSpotForAll(){
        String baseUrl = "https://fcsapi.com/api-v3/forex/latest?symbol=USD/INR,EUR/INR,GBP/INR,EUR/USD,GBP/USD&access_key="+ ForEximConstants.accessKey;
        try {
            String response = Get(baseUrl).execute().returnContent().asString();
            SpotRateResponse spotRateResponse = new Gson().fromJson(response, SpotRateResponse.class);
            Map<String, SpotResponse> currencySpotMap = spotRateResponse.response.stream().collect(Collectors.toMap(i -> i.s, Function.identity()));
            return currencySpotMap;
        } catch (IOException e) {
            return new LinkedHashMap<>();
        }
    }

    private  int dateDifference(Date startDate, Date endDate){
        return (int) ChronoUnit.DAYS.between(endDate.toInstant(), startDate.toInstant());
    }

    private class ClientForwardInputRequest extends FEApiRequest {
        public String userId;
        public String entryDate;
        public double notional;
        public InputType type;
        public String currencyPair;
        public String forwardDate;
    }

    private class ClientForwardResponse{
        public String userId;
        public double notional;
        public InputType type;
        public String entryDate;
        public String currencyPair;
        public String forwardDate;
        public double currentSpot;
        public double forwardRate;
        public double forwardValue;
        public double hedgingCost;


    }

    private class LiveSpotResponse{
        public String currency;
        public Date dateTime;
        public double bid;
        public double ask;
    }

    private   class SpotRateResponse{
        public List<SpotResponse> response;
    }

    private class SpotResponse{
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

    private class ExportImportResponse{
        public String currency;
        public String entryDate;
        public List<ImportExport> data;

    }

    private class ImportExport{
        public String expiryDate;
        public String premia;
        public double exporters;
        public double importers;
    }

    public static void main(String[] args) {
//        System.out.println(new ForwardCalculator().fetchAllForwardValues(new APIGatewayProxyRequestEvent(), null));
//        System.out.println("ch");
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.setBody("{\n" +
                "    \"entryDate\":\"2021-10-25\",\n" +
                "    \"notional\": 400,\n" +
                "    \"type\":\"Sell\",\n" +
                "    \"forwardDate\":\"2022-01-11\",\n" +
                "    \"currencyPair\":\"USD/INR\"\n" +
                "}");
        requestEvent.setHttpMethod("POST");
        System.out.println(new ForwardCalculator().fetchImportExportTable(requestEvent));
    }

}
