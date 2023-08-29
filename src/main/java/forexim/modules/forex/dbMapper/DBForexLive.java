package forexim.modules.forex.dbMapper;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.google.gson.annotations.SerializedName;
import forexim.assets.DBMain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DynamoDBTable(tableName = "forex-live")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DBForexLive extends DBMain {
    @DynamoDBHashKey public String currencyId;
    public String currency;
    public String rate;
    public String bid;
    public String ask;
    public String high;
    public String low;
    public String open;
    public String close;
    @SerializedName("timestamp") public String timeStamp;
    // Compulsory
    public String rateType;
    //
    public enum  RateType { Daily, Live, }

}
