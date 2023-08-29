package forexim.util.tokenVerify.dbMapper;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import forexim.assets.DBMain;
import forexim.util.FEAwsCredentials;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DynamoDBTable(tableName = "security-token")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class DBSecurityToken extends DBMain {
    @DynamoDBHashKey public String token;
    @DynamoDBTypeConvertedEnum public SecurityTokenType tokenType;
    public String userId;
    public String deviceId;

    public enum SecurityTokenType {
        SignUp, SignIn, Authorization, RefreshToken
    }

    public void save() { FEAwsCredentials.dynamoDBMapper().save(this); }
}
