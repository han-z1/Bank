package org.bank.servlet;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bank.api.UserAPI;
import org.bank.exception.Exceptions;
import org.bank.exception.ValidationException;
import org.bank.util.DBConnector;
import org.bank.util.ResponseHandler;
import static dev.samstevens.totp.util.Utils.getDataUriForImage;

public class LoginServlet extends HttpServlet
{
	public static final String CXID_USER_QUERY = "SELECT User.UserID, User.CustomerID, User.Email, User.Password, User.ActiveSessionID, User.SessionLastAccessedTime, BankAccount.BankAccountID, BankAccount.AccountNumber, BankAccount.AccountHolder, BankAccount.SecretTFA, BankAccount.TFAEnabled FROM User INNER JOIN BankAccount ON BankAccount.AccountHolder = User.UserID WHERE User.LOGINCOLUMN = ?";
	private static final String UPDATE_SESSION_QUERY = "UPDATE User SET ActiveSessionID = ?, SessionLastAccessedTime = ? WHERE UserID = ?";
	private static final String UPDATE_TFA_ENABLE_QUERY = "UPDATE BankAccount SET TFAEnabled = 1 WHERE BankAccountID = ?";

	@Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		try
		{
			String customerIDStr = req.getParameter("customer_id");
			Long customerID = null;
			if(NumberUtils.isDigits(customerIDStr))
			{
				customerID = Long.parseLong(customerIDStr);
			}
			String password = req.getParameter("password");

			String emailID = req.getParameter("email_id");

			ResultSet userRS = null;
			if(Objects.isNull(customerID) && StringUtils.isNotBlank(emailID))
			{
				userRS = DBConnector.get(CXID_USER_QUERY.replace("LOGINCOLUMN", "Email"), emailID);
				if(!userRS.next())
				{
					throw Exceptions.INVALID_CREDENTIALS;
				}

				if(!Objects.equals(userRS.getString("User.Email"), emailID) || !Objects.equals(userRS.getString("User.Password"), password))
				{
					throw Exceptions.INVALID_CREDENTIALS;
				}
				customerID = userRS.getLong("User.CustomerID");
			}
			else
			{
				userRS = DBConnector.get(CXID_USER_QUERY.replace("LOGINCOLUMN", "CustomerID"), customerID);
				if(!userRS.next())
				{
					throw Exceptions.INVALID_CREDENTIALS;
				}

				if(!Objects.equals(userRS.getLong("User.CustomerID"), customerID) || !Objects.equals(userRS.getString("User.Password"), password))
				{
					throw Exceptions.INVALID_CREDENTIALS;
				}
				emailID = userRS.getString("User.Email");
			}

			Long sessionID = userRS.getLong("User.ActiveSessionID");
			Long sessionAccessTime = userRS.getLong("User.SessionLastAccessedTime");

			if(Objects.nonNull(sessionID) && UserAPI.isSessionActive(sessionAccessTime))
			{
				throw Exceptions.ACTIVE_SESSION_EXISTS;
			}

			Boolean isTFAEnabled = userRS.getBoolean("BankAccount.TFAEnabled");
			Map<String, Object> respMap = new HashMap<>();
			respMap.put("is_tfa_enabled", isTFAEnabled);
			String tfaSecret = userRS.getString("BankAccount.SecretTFA");
			if(!isTFAEnabled)
			{
				QrData data = new QrData.Builder()
					.label("example@example.com")
					.secret(tfaSecret)
					.issuer("AppName")
					.algorithm(HashingAlgorithm.SHA1) // More on this below
					.digits(6)
					.period(30)
					.build();
//				QrGenerator generator = new ZxingPngQrGenerator();
//				byte[] imageData = generator.generate(data);
//				String mimeType = generator.getImageMimeType();
//				String dataUri = getDataUriForImage(imageData, mimeType);

				respMap.put("tfa_secret", tfaSecret);
//				respMap.put("tfa_qr", dataUri);
			}

			String totp = req.getParameter("totp");
			if(Objects.isNull(totp))
			{
				ResponseHandler.respond(resp, 200, 0, "success", respMap);
				return;
			}

			TimeProvider timeProvider = new SystemTimeProvider();
			CodeGenerator codeGenerator = new DefaultCodeGenerator();
			CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
			if(!verifier.isValidCode(tfaSecret, totp))
			{
				throw Exceptions.INVALID_TOTP;
			}

			if(!isTFAEnabled)
			{
				Long bankAccountID = userRS.getLong("BankAccount.BankAccountID");
				DBConnector.execute(UPDATE_TFA_ENABLE_QUERY, bankAccountID);
			}

			Long userID = userRS.getLong("User.UserID");
			Long newSessionID = ThreadLocalRandom.current().nextLong(1000000000000000000l, 9000000000000000000l);
			DBConnector.execute(UPDATE_SESSION_QUERY, newSessionID, System.currentTimeMillis(), userID);

			Cookie[] cookies = new Cookie[2];
			cookies[0] = new Cookie("CustomerID", Long.toString(customerID));
			cookies[1] = new Cookie("SessionID", Long.toString(newSessionID, 36));

			for(Cookie cookie: cookies)
			{
				resp.addHeader("Set-Cookie", cookie.getName() + "=" + cookie.getValue() + "; Path=/; SameSite=None");
			}
			ResponseHandler.respond(resp, 200, 0, "success");
		}
		catch(ValidationException ex)
		{
			throw ex;
		}
		catch(Exception exception)
		{
			throw Exceptions.INTERNAL_ERROR;
		}
	}
}
