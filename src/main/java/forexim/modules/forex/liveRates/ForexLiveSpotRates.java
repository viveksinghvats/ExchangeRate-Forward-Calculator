package forexim.modules.forex.liveRates;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import forexim.util.FEApiResponse;
import io.socket.client.IO;
import io.socket.client.Socket;
import forexim.modules.forex.dbMapper.DBForexLive;
import forexim.util.Constants;
import forexim.util.local.TestUtil;
import io.socket.emitter.Emitter;
import io.socket.emitter.Emitter.Listener;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.Collectors;

import static org.apache.http.client.fluent.Request.Get;


public class ForexLiveSpotRates implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        while (true){
          try {
              return fetchLiveSpotRate();
//              fetchSocketDate();
          }catch (Exception e){
//              e.printStackTrace();
          }
        }
    }

    public static void fetchSocketDate(){
        String apiKey = "IjKl0NmFTiMYcIPjGK0AKDj";
        List<String> currency_ids = Arrays.asList("1", "2", "3");
        URI uri = URI.create("wss://fcsapi.com");
        IO.Options options = IO.Options.builder().setTransports(new String[]{"websocket"}).build();
        Socket socket = IO.socket(uri, options);
        socket.emit("heartbeat", apiKey);
        socket.emit("real_time_join", currency_ids);
        AtomicReference<AtomicReferenceArray<Object>> data = new AtomicReference<>(new AtomicReferenceArray<>(new Object[]{}));
        socket.on("data_received", new Listener() {
            @Override
            public void call(Object... objects) {

            }
        });
        socket.connect();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println(socket.connected()); // true
            }
        });
        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println(socket.connected()); // false
            }
        });
        System.out.println("check");
    }

    public APIGatewayProxyResponseEvent  fetchLiveSpotRate(){
        String baseUrl = "https://www.live-rates.com/api/price?key=34aadda29b&rate=USD_INR,EUR_USD,GBP_USD";
        try {
            String response = Get(baseUrl).execute().returnContent().asString();
            List<DBForexLive> forexData = Arrays.asList(new Gson().fromJson(response, DBForexLive[].class)).stream().map(i -> {i.rateType = "live-spot"; return i;}).collect(Collectors.toList());
            AtomicInteger i = new AtomicInteger();
            forexData.forEach(data -> {
                data.currencyId = new Constants().liveCurrencyId + (i.getAndIncrement());
            });
            return FEApiResponse.responseEvent(forexData, HttpStatus.SC_OK);
        } catch (Exception e) {
            return FEApiResponse.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Live Api Might Me Down Or Structure Got Changed");
        }
    }

    public static void main(String[] args) {
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.setHttpMethod("GET");
        System.out.println(new ForexLiveSpotRates().handleRequest(requestEvent, TestUtil.fetchTestContext()));
    }
}
