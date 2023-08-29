package forexim.util.tokenVerify;

import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.microsoft.graph.models.RolePermission;
import forexim.modules.admin.dbMapper.DBUser;
import forexim.util.FEAwsCredentials;
import forexim.util.tokenVerify.dbMapper.DBSecurityToken;
import forexim.util.tokenVerify.dbMapper.DBSecurityToken.SecurityTokenType;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

public class JwtTokenVerify {
    private static boolean isValidRefreshToken(String token) {
        try {
            return new SecurityTokenService("some_random_private_key", token).verifyToken();
        } catch (IllegalArgumentException | UnsupportedEncodingException | JWTVerificationException e) {
            e.printStackTrace();
            return false;
        }
    }
    public static String securityTokenPrivateKey = "Private-Security-Token";
    public static String generateVerificationToken(DBUser dbUser, String deviceId, String version) throws IllegalArgumentException, UnsupportedEncodingException, JWTCreationException {
        HashMap<String, String> payload = new HashMap<String, String>();
        payload.put("timestamp", Instant.now().toString());
        payload.put("signUpId", UUID.randomUUID().toString());
        payload.put("type", SecurityTokenType.SignUp.name());
        payload.put("version", version);
        String token = new SecurityTokenService(securityTokenPrivateKey, null).generateJWTToken(payload);
        System.out.println("Token: "+ token);
        FEAwsCredentials.dynamoDBMapper().save(new DBSecurityToken(token, SecurityTokenType.SignUp, dbUser.userId, deviceId));
        return token;
    }
    @AllArgsConstructor @NoArgsConstructor
    public static class TokenPair {
        public String accessToken;
        public String refreshToken;
    }

    public static class TokenPayload {
        public String timestamp;
        public SecurityTokenType type;
        public String userId;
        public String userRole;
        public String deviceId;
        public String version;
        public String refreshTokenExpTime;
        public boolean isSuperAdmin;
        public String request;

        public TokenPayload(String timestamp, SecurityTokenType type, String userId, String userRole, String deviceId, String version, String refreshTokenExpTime, boolean isSuperAdmin) {
            this.timestamp = timestamp;
            this.type = type;
            this.userId = userId;
            this.userRole = userRole;
            this.deviceId = deviceId;
            this.version = version;
            this.refreshTokenExpTime = refreshTokenExpTime;
            this.isSuperAdmin = isSuperAdmin;
        }

        public HashMap<String, String> toHashMap() {
            HashMap<String, String> payload = new HashMap<String, String>();
            if (timestamp != null)           { payload.put("timestamp",           timestamp);           }
            if (type != null)                { payload.put("type",                type.name());         }
            if (userId != null)              { payload.put("userId",              userId);              }
            if (deviceId != null)            { payload.put("deviceId",            deviceId);            }
            if (refreshTokenExpTime != null) { payload.put("refreshTokenExpTime", refreshTokenExpTime); }
            if (version != null) 			 { payload.put("version",			  version); 			}
            payload.put("isSuperAdmin", Boolean.toString(isSuperAdmin));
            return payload;
        }
    }
}
