package com.example.web.login;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mobile.device.Device;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.example.web.domain.user.SecurityUserVo;
import com.example.web.domain.user.UserVo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.compression.CompressionCodecs;

@Component
public class TokenUtils {

	private final String AUDIENCE_UNKNOWN = "unknown";
	private final String AUDIENCE_WEB = "web";
	private final String AUDIENCE_MOBILE = "mobile";
	private final String AUDIENCE_TABLET = "tablet";

	@Value("${jwt.token.secret}")
	private String secret;

	@Value("${jwt.token.expiration}")
	private Long expiration;

	public String getUsernameFromToken(String token) {
		String username;
		try {
			final Claims claims = this.getClaimsFromToken(token);
			username = claims.getSubject();
		} catch (Exception e) {
			username = null;
		}
		return username;
	}
	
	public String getAuthoritiesFromToken(String token) {
		String authorities;
		try {
			final Claims claims = this.getClaimsFromToken(token);
			authorities = (String)claims.get("authorities");
		} catch (Exception e) {
			authorities = null;
		}
		return authorities;
	}

	public Date getCreatedDateFromToken(String token) {
		Date created;
		try {
			final Claims claims = this.getClaimsFromToken(token);
			created = new Date((Long) claims.get("created"));
		} catch (Exception e) {
			created = null;
		}
		return created;
	}

	public Date getExpirationDateFromToken(String token) {
		Date expiration;
		try {
			final Claims claims = this.getClaimsFromToken(token);
			expiration = claims.getExpiration();
		} catch (Exception e) {
			expiration = null;
		}
		return expiration;
	}

	public String getAudienceFromToken(String token) {
		String audience;
		try {
			final Claims claims = this.getClaimsFromToken(token);
			audience = (String) claims.get("audience");
		} catch (Exception e) {
			audience = null;
		}
		return audience;
	}

	private Claims getClaimsFromToken(String token) {
		Claims claims;
		try {
			claims = Jwts.parser().setSigningKey(this.secret).parseClaimsJws(token).getBody();
		} catch (Exception e) {
			claims = null;
		}
		return claims;
	}

	private Date generateCurrentDate() {
		return new Date(System.currentTimeMillis());
	}

	private Date generateExpirationDate() {
		return new Date(System.currentTimeMillis() + this.expiration * 1000);
	}

	private Boolean isTokenExpired(String token) {
		final Date expiration = this.getExpirationDateFromToken(token);
		return expiration.before(this.generateCurrentDate());
	}

	private Boolean isCreatedBeforeLastPasswordReset(Date created, Date lastPasswordReset) {
		return (lastPasswordReset != null && created.before(lastPasswordReset));
	}

	private String generateAudience(Device device) {
		String audience = this.AUDIENCE_UNKNOWN;
		if (device != null) {
			if (device.isNormal()) {
				audience = this.AUDIENCE_WEB;
			} else if (device.isTablet()) {
				audience = AUDIENCE_TABLET;
			} else if (device.isMobile()) {
				audience = AUDIENCE_MOBILE;
			}
		}
		return audience;
	}

	private Boolean ignoreTokenExpiration(String token) {
		String audience = this.getAudienceFromToken(token);
		return (this.AUDIENCE_TABLET.equals(audience) || this.AUDIENCE_MOBILE.equals(audience));
	}

	public String generateToken(UserVo loginUser, Device device) {
		Map<String, Object> claims = new HashMap<String, Object>();
		claims.put("sub", loginUser.getUserId());
		claims.put("authorities", loginUser.getAuthorities());
		claims.put("audience", this.generateAudience(device));
		claims.put("created", this.generateCurrentDate());
		return this.generateToken(claims);
	}

	private String generateToken(Map<String, Object> claims) {
		return Jwts.builder()
					.setClaims(claims)
					.setExpiration(this.generateExpirationDate())
					.compressWith(CompressionCodecs.DEFLATE)
					.signWith(SignatureAlgorithm.HS512, this.secret)
					.compact();
	}

	public Boolean canTokenBeRefreshed(String token, Date lastPasswordReset) {
		final Date created = this.getCreatedDateFromToken(token);
		return (!(this.isCreatedBeforeLastPasswordReset(created, lastPasswordReset))
				&& (!(this.isTokenExpired(token)) || this.ignoreTokenExpiration(token)));
	}

	public String refreshToken(String token) {
		String refreshedToken;
		try {
			final Claims claims = this.getClaimsFromToken(token);
			claims.put("created", this.generateCurrentDate());
			refreshedToken = this.generateToken(claims);
		} catch (Exception e) {
			refreshedToken = null;
		}
		return refreshedToken;
	}

	  public Boolean validateToken(String token, UserVo currentUser) {
//		    SecurityUserVo user = (SecurityUserVo) userDetails;
		    final String username = this.getUsernameFromToken(token);
		    final Date created = this.getCreatedDateFromToken(token);
//		    return (username.equals(currentUser.getUserId()) && !(this.isTokenExpired(token)) && !(this.isCreatedBeforeLastPasswordReset(created, user.getLastPasswordReset())));
		    return (username.equals(currentUser.getUserId()) && !(this.isTokenExpired(token)));
		  }
}
