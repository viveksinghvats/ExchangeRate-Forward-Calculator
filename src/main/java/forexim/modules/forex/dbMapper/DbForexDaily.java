package forexim.modules.forex.dbMapper;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.glue.model.InvalidInputException;
import forexim.assets.DBMain;
import forexim.modules.forwardCalculator.dbMapper.DBForwardRates;
import forexim.util.FEAwsCredentials;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@DynamoDBTable(tableName = "forex-daily")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class DbForexDaily extends DBMain {
    @DynamoDBHashKey public String currencyId;
    public String open;
    public String high;
    public String low;
    public String close;
    public String ch;
    public String change;
    public String t;
    public String currency;
    public String inputTime;
    public String time;


    public static List<DbForexDaily> fetchAll() {
        return new ArrayList<>(FEAwsCredentials.dynamoDBMapper().scan(DbForexDaily.class, new DynamoDBScanExpression()));
    }

    public static List<DbForexDaily> loadInputForEntryDate(String entryDate) {
        HashMap<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
        eav.put(":inputTime", new AttributeValue().withS(entryDate));
        DynamoDBQueryExpression<DbForexDaily> queryExpression = new DynamoDBQueryExpression<DbForexDaily>()
                .withIndexName("inputTime-index")
                .withKeyConditionExpression("inputTime = :inputTime")
                .withExpressionAttributeValues(eav)
                .withConsistentRead(false);
        PaginatedQueryList<DbForexDaily> dbQueryList = FEAwsCredentials.dynamoDBMapper().query(DbForexDaily.class, queryExpression);
        if (dbQueryList == null || dbQueryList.isEmpty()) {return new ArrayList<>();
        }
        else { return dbQueryList;}
    }
}
