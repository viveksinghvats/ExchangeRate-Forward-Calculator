package forexim.modules.admin.dbMapper;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.glue.model.InvalidInputException;
import forexim.assets.DBMain;
import forexim.util.FEAwsCredentials;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;

@DynamoDBTable(tableName = "user")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class DBUser extends DBMain {
    @DynamoDBHashKey public String userId;
    public String name;
    public String emailId;
    public String phoneNumber;
    public String lastLoginTime;
    @DynamoDBTypeConvertedEnum public UserRole userRole;
    public String hashedPassword;
    public String secureHash;
    public Integer noOfLogins;
    public long wrongAttemptsCount;

    public static enum UserRole{ ADMIN  }

    public static DBUser loadUserWithEmailID(String emailID) {
        HashMap<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
        eav.put(":emailId", new AttributeValue().withS(emailID));
        DynamoDBQueryExpression<DBUser> queryExpression = new DynamoDBQueryExpression<DBUser>()
                .withIndexName("emailId-index")
                .withKeyConditionExpression("emailId = :emailId")
                .withExpressionAttributeValues(eav)
                .withConsistentRead(false);
        PaginatedQueryList<DBUser> dbQueryList = FEAwsCredentials.dynamoDBMapper().query(DBUser.class, queryExpression);
        if (dbQueryList == null || dbQueryList.isEmpty()) {throw new InvalidInputException("Email Id Don't  Exists");
        }
        else { return dbQueryList.get(0); }
    }

    public static DBUser load(String userId) {
        return FEAwsCredentials.dynamoDBMapper().load(DBUser.class, userId);
    }
}
