package forexim.modules.admin.signup;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import forexim.modules.admin.dbMapper.DBUser;
import forexim.modules.admin.dbMapper.DBUser.UserRole;
import forexim.util.FEApiRequest;
import forexim.util.FEApiResponse;
import forexim.util.FEAwsCredentials;
import forexim.util.tokenVerify.JwtTokenVerify;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;

import java.io.UnsupportedEncodingException;
import java.util.UUID;


public class UserSignUp implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @SneakyThrows
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        return createAdminUser(input);
    }

    public static APIGatewayProxyResponseEvent createAdminUser(APIGatewayProxyRequestEvent input) throws InstantiationException, IllegalAccessException, UnsupportedEncodingException {
        CreateUserRequest request = FEApiRequest.from(input, CreateUserRequest.class);
        if( request == null || request.emailId == null)
            return FEApiResponse.error(HttpStatus.SC_BAD_REQUEST, "Invalid Body Data");
        try {
            DBUser user = DBUser.loadUserWithEmailID(request.emailId);
            if( user != null) return FEApiResponse.error(HttpStatus.SC_BAD_REQUEST,"EmailId Already Exists");
        }catch (Exception e){
            DBUser dbUser = new DBUser();
            dbUser.userId = UUID.randomUUID().toString();
            dbUser.emailId = request.emailId;
            dbUser.userRole = UserRole.ADMIN;
            dbUser.name = request.name;
            dbUser.phoneNumber = request.phoneNumber;
            dbUser.hashedPassword = DigestUtils.md5Hex(request.plainPassword.getBytes());
            FEAwsCredentials.dynamoDBMapper().save(dbUser);
            String verificationToken = JwtTokenVerify.generateVerificationToken(dbUser, request.deviceId, "v1");
            if( verificationToken != null){
                return FEApiResponse.success("User Created");
            }
            return FEApiResponse.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, "User not able to create");
        }
        return null;
    }

    public static class CreateUserRequest extends FEApiRequest{
        public String userId;
        public String name;
        public String emailId;
        public String phoneNumber;
        public String lastLoginTime;
        public UserRole userRole;
        public String plainPassword;
        public String secureHash;
        public Integer noOfLogins;
        public String deviceId;
        public long wrongAttemptsCount;
    }

}
