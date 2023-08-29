package forexim.util.tokenVerify;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;

import forexim.modules.admin.dbMapper.DBUser;
import forexim.util.tokenVerify.JwtTokenVerify.TokenPair;
import forexim.util.tokenVerify.JwtTokenVerify.TokenPayload;
import forexim.util.tokenVerify.dbMapper.DBSecurityToken;
import forexim.util.tokenVerify.dbMapper.DBSecurityToken.SecurityTokenType;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class SecurityTokenService {

    private String privateKey;
    private String token;
    private String issuer = "ForExim";

    public SecurityTokenService(String privateKey, String token) {
        super();
        this.privateKey = privateKey;
        this.token      = token;
    }

    public String getToken() { return this.token; }
    public String getPrivateKey() { return this.privateKey; }

    /** Generates a JWT Token with given payload. Token with exp time. */
    public String generateJWTToken(Map<String, String> payload, Instant expTime) throws IllegalArgumentException, UnsupportedEncodingException , JWTCreationException {
        Algorithm algorithm = Algorithm.HMAC256(privateKey);
        Builder builder = JWT.create();
        if (payload != null) {
            payload.entrySet().forEach(entry -> builder.withClaim(entry.getKey(), entry.getValue()));
        }
        token = builder.withExpiresAt(Date.from(expTime)).withIssuer(issuer).sign(algorithm);
        return token;
    }

    /** Generates a JWT Token with given payload. Token is valid forever. */
    public String generateJWTToken(Map<String, String> payload) throws IllegalArgumentException, UnsupportedEncodingException , JWTCreationException {
        Algorithm algorithm = Algorithm.HMAC256(privateKey);
        Builder builder = JWT.create();
        if (payload != null) {
            payload.entrySet().forEach(entry -> builder.withClaim(entry.getKey(), entry.getValue()));
        }
        token = builder.withIssuer(issuer).sign(algorithm);
        return token;
    }

    public static TokenPair generateAuthTokenPair(DBUser dbUser, String deviceId, Instant refreshTokenExpTime, String version, Boolean isSuperAdmin) throws IllegalArgumentException, UnsupportedEncodingException, JWTCreationException {
        Instant accessTokenExpiryTime    = Instant.now().plusSeconds(21600);//Instant.now().plusSeconds(21600); // Instant.now().plus(24, ChronoUnit.DAYS);
        Instant refreshTokenExpiryTime   = (refreshTokenExpTime != null) ? refreshTokenExpTime : Instant.now().plus(24, ChronoUnit.HOURS);

        TokenPayload accessTokenPayload = new TokenPayload(Instant.now().toString(), SecurityTokenType.Authorization, dbUser.userId, dbUser.userRole.toString(), deviceId, version, refreshTokenExpiryTime.toString(), isSuperAdmin);
        String accessToken           = new SecurityTokenService(JwtTokenVerify.securityTokenPrivateKey, null).generateJWTToken(accessTokenPayload.toHashMap(), accessTokenExpiryTime);

        TokenPayload refreshTokenPayload = new TokenPayload(Instant.now().toString(), SecurityTokenType.RefreshToken, dbUser.userId, dbUser.userRole.name(), deviceId, version, refreshTokenExpiryTime.toString(), isSuperAdmin);
        String refreshToken         = new SecurityTokenService(JwtTokenVerify.securityTokenPrivateKey, null).generateJWTToken(refreshTokenPayload.toHashMap(), refreshTokenExpiryTime);
        new DBSecurityToken(refreshToken, SecurityTokenType.RefreshToken, dbUser.userId, deviceId).save();
        return new TokenPair(accessToken, refreshToken);
    }

    public String generateJWTTokenWithExpires(Integer milliSecs, Map<String, String> payload) throws IllegalArgumentException, UnsupportedEncodingException , JWTCreationException {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        JwtBuilder builder = Jwts.builder();
        if (payload != null) {
            payload.entrySet().forEach(entry -> builder.claim(entry.getKey(), entry.getValue()));
        }
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        if (milliSecs >= 0) {
            long expMillis = nowMillis + milliSecs;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp);
        }

        token = builder.setIssuer(issuer).setIssuedAt(now).signWith(signatureAlgorithm, "some_random_private_key").compact();
        return token;
    }

    public boolean verifyToken() throws JWTDecodeException, IllegalArgumentException, UnsupportedEncodingException, JWTVerificationException {
        Algorithm algorithm  = Algorithm.HMAC256(privateKey);
        JWTVerifier verifier = JWT.require(algorithm).withIssuer(issuer).build();
        verifier.verify(token);
        return true;
    }

    public String getPayloadItemForKey(String key) {
        return JWT.decode(token).getClaim(key).asString();
    }

    public Instant getExpTime() {
        Date expTime = JWT.decode(token).getExpiresAt();
        return expTime != null ? expTime.toInstant() : null;
    }


    @Override
    public String toString() {
        return "SecurityTokenService [privateKey=" + privateKey + ", token=" + token + "]";
    }

}
