package forexim.modules.admin.signin;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import forexim.modules.admin.dbMapper.DBUser;
import forexim.modules.admin.dbMapper.DBUser.UserRole;
import forexim.modules.admin.signup.UserSignUp.CreateUserRequest;
import forexim.util.FEApiRequest;
import forexim.util.FEApiResponse;
import forexim.util.tokenVerify.JwtTokenVerify.TokenPair;
import forexim.util.tokenVerify.SecurityTokenService;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;


public class UserSignIn implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @SneakyThrows
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        return userSignIn(input);
    }

    public static APIGatewayProxyResponseEvent userSignIn(APIGatewayProxyRequestEvent input) throws InstantiationException, IllegalAccessException {
        CreateUserRequest request = FEApiRequest.from(input, CreateUserRequest.class);
        if(request.emailId == null || request.plainPassword == null) return FEApiResponse.error(HttpStatus.SC_BAD_REQUEST, "Email and password are required fields");
        try {
            DBUser dbUser = DBUser.loadUserWithEmailID(request.emailId);
            if(!dbUser.hashedPassword.equals(DigestUtils.md5Hex(request.plainPassword.getBytes()))) return FEApiResponse.error(HttpStatus.SC_BAD_REQUEST, "Wrong password");
            TokenPair tokenPair = SecurityTokenService.generateAuthTokenPair(dbUser, request.deviceId, null, "v1", true);
            return FEApiResponse.responseEvent(new SignInResponse(dbUser.userId, dbUser.name, dbUser.emailId, dbUser.phoneNumber, dbUser.userRole, tokenPair.accessToken, tokenPair.refreshToken), HttpStatus.SC_OK);

        }catch (Exception e){
            return FEApiResponse.error(HttpStatus.SC_BAD_REQUEST, "Invalid Email Id");
        }
    }

    @AllArgsConstructor
    public static class SignInResponse{
        public String userId;
        public String userName;
        public String emailId;
        public String phoneNumber;
        public UserRole userRole;
        public String accessToken;
        public String refreshToken;
    }

}
