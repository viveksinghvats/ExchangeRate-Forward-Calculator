package forexim.modules.forwardCalculator.dbMapper;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.glue.model.InvalidInputException;
import forexim.assets.DBMain;
import forexim.util.FEAwsCredentials;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@DynamoDBTable(tableName = "forward-rate-table")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class DBForwardRates extends DBMain {
    @DynamoDBHashKey public String forwardRateId;
    public List<ForwardEntry> forwardEntries;
    public String currency;
    public Date entryDate;
    @DynamoDBTypeConvertedEnum public InputType inputType;

    @DynamoDBDocument @Getter @Setter @AllArgsConstructor @NoArgsConstructor
    public static class ForwardEntry{
        public Date startDate;
        public Date endDate;
        public Integer noOfDays;
        public Double premiaBid;
        public Double premiaAsk;
        @DynamoDBTypeConvertedEnum public Expiration expiration;
    }

    public static enum Expiration{
        OverNight, Spot, OneWeek, OneMonth, TwoMonths, ThreeMonths, FourMonths, FiveMonths, SixMonths, SevenMonths, EightMonths, NineMonths, TenMonths, ElevenMonths, OneYear, TwoYear, ThreeYear, FourYear, FiveYear
    }

    public static List<DBForwardRates> fetchAll() {
        return new ArrayList<>(FEAwsCredentials.dynamoDBMapper().scan(DBForwardRates.class, new DynamoDBScanExpression()));
    }

    public static List<DBForwardRates> loadInputForEntryDate(String entryDate, String currency) {
        HashMap<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
        eav.put(":entryDate", new AttributeValue().withS(entryDate));
        DynamoDBQueryExpression<DBForwardRates> queryExpression = new DynamoDBQueryExpression<DBForwardRates>()
                .withIndexName("entryDate-index")
                .withKeyConditionExpression("entryDate = :entryDate")
                .withExpressionAttributeValues(eav)
                .withConsistentRead(false);
        PaginatedQueryList<DBForwardRates> dbQueryList = FEAwsCredentials.dynamoDBMapper().query(DBForwardRates.class, queryExpression);
        if (dbQueryList == null) {throw new InvalidInputException("Entry  Don't  Exists");
        }
        else { return dbQueryList.stream().filter(i -> i.currency != null && i.currency.equals(currency)).collect(Collectors.toList()); }
    }

    public enum InputType{
        Cron, Manual
    }
}
