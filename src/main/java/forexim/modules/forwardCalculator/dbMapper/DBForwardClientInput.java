package forexim.modules.forwardCalculator.dbMapper;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.glue.model.InvalidInputException;
import forexim.assets.DBMain;
import forexim.modules.admin.dbMapper.DBUser;
import forexim.modules.forex.dbMapper.DbForexDaily;
import forexim.util.FEAwsCredentials;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.swing.text.DateFormatter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static forexim.modules.forwardCalculator.ForwardCalculatorInput.convertInstantToStartDateInstant;
import static forexim.modules.forwardCalculator.ForwardCalculatorInput.convertToInstant;

@DynamoDBTable(tableName = "forward-client-input")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class DBForwardClientInput extends DBMain {
    @DynamoDBHashKey public String inputId;
    public String userId;
    public Date entryDate;
    public double notional;
    @DynamoDBTypeConvertedEnum public InputType type;
    public String currencyPair;
    public Date forwardDate;

    public enum InputType{ Sell, Buy }

    public static List<DBForwardClientInput> fetchAll() {
        return new ArrayList<>(FEAwsCredentials.dynamoDBMapper().scan(DBForwardClientInput.class, new DynamoDBScanExpression()));
    }
    public static DBForwardClientInput loadClientInputForEntryDate(String entryDate) {
        HashMap<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
        eav.put(":entryDate", new AttributeValue().withS(entryDate));
        DynamoDBQueryExpression<DBForwardClientInput> queryExpression = new DynamoDBQueryExpression<DBForwardClientInput>()
                .withIndexName("entryDate-index")
                .withKeyConditionExpression("entryDate = :entryDate")
                .withExpressionAttributeValues(eav)
                .withConsistentRead(false);
        PaginatedQueryList<DBForwardClientInput> dbQueryList = FEAwsCredentials.dynamoDBMapper().query(DBForwardClientInput.class, queryExpression);
        if (dbQueryList == null || dbQueryList.isEmpty()) {throw new InvalidInputException("Entry  Don't  Exists");
        }
        else { return dbQueryList.get(0); }
    }

    // 2021-05-21T00:00:00.000Z

    public static void main(String[] args) {
        System.out.println("check");
        Format formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm");
        formatter = new SimpleDateFormat("yyyy-MM-dd");
        String s = formatter.format(Date.from(Instant.now().minus(142,ChronoUnit.DAYS)));
        System.out.println(s);
        s+="T00:00:00.000Z";
//        DateFormatter formatter = new DateFormatter().
// 2021-05-21
        DBForwardClientInput data = loadClientInputForEntryDate(s);
        System.out.println("check");
    }

}
