package org.bank.api;

import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.bank.exception.Exceptions;
import org.bank.util.DBConnector;

public class UserAPI
{
	private static final String CREATE_USER_QUERY = "INSERT INTO User (CustomerID, FirstName, LastName, DateOfBirth, MobileNumber, Email, Password, SignupTime) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String UPDATE_USER_QUERY = "UPDATE User SET Email = ?, MobileNumber = ? WHERE CustomerID = ?";
	private static final String CREATE_ACCOUNT_QUERY = "INSERT INTO BankAccount (AccountNumber, AccountHolder, SecretTFA, CreatedTime) VALUES(?, ?, ?, ?)";
	private static final String HIGH_CXID_QUERY = "SELECT UserID, CustomerID FROM User ORDER BY CustomerID DESC LIMIT 1";
	private static final String HIGH_ACCOUNTID_QUERY = "SELECT BankAccountID, AccountNumber FROM BankAccount ORDER BY AccountNumber DESC LIMIT 1";
	public static final String CXID_USER_QUERY = "SELECT UserID, CustomerID, FirstName, LastName, DateOfBirth, MobileNumber, Email, Password, ActiveSessionID, SessionLastAccessedTime, SignupTime, isBankStaff FROM User WHERE CustomerID = ?";
	private static final String USER_EXISTS_QUERY = "SELECT UserID, CustomerID, MobileNumber, Email FROM User WHERE Email = ? OR MobileNumber = ?";

	public static void createUser(String firstName, String lastName, Long dateOfBirth, Long mobileNumber, String emailID, String password) throws Exception
	{
		if(StringUtils.isBlank(firstName))
		{
			throw Exceptions.USER_NAME_MANDATORY;
		}
		firstName = firstName.trim();

		if(StringUtils.isBlank(lastName))
		{
			lastName = null;
		}

		if(Objects.nonNull(lastName))
		{
			lastName = lastName.trim();
		}

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -18);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Long adultAgeLimit = cal.getTimeInMillis();

		if(Objects.isNull(dateOfBirth) || adultAgeLimit < dateOfBirth)
		{
			throw Exceptions.ONLY_ADULT_SIGNUP;
		}

		validateMobileNumber(mobileNumber);
		validateEmail(emailID);
		emailID = emailID.trim();

		if(StringUtils.isEmpty(password) || password.length() < 8)
		{
			throw Exceptions.PASSWORD_MIN_LEN;
		}

		ResultSet userExistRS = DBConnector.get(USER_EXISTS_QUERY, emailID, mobileNumber);
		if(userExistRS.next())
		{
			throw Exceptions.USER_ALREADY_EXISTS;
		}

		Long customerID = 10000001l;
		ResultSet highCxRS = DBConnector.get(HIGH_CXID_QUERY);
		if(highCxRS.next())
		{
			customerID = highCxRS.getLong("CustomerID") + 1;
		}

		Long accountNumber = 5000000001l;
		ResultSet highAccRS = DBConnector.get(HIGH_ACCOUNTID_QUERY);
		if(highAccRS.next())
		{
			accountNumber = highAccRS.getLong("AccountNumber") + 1;
		}

		SecretGenerator secretGenerator = new DefaultSecretGenerator();
		String tfaSecret = secretGenerator.generate();

		Connection db = null;

		try
		{
			db = DBConnector.getConnection();
			db.setAutoCommit(false);
			ResultSet userRS = DBConnector.execute(db, CREATE_USER_QUERY, customerID, firstName, lastName, dateOfBirth, mobileNumber, emailID, password, System.currentTimeMillis());
			if(!userRS.next())
			{
				throw Exceptions.INTERNAL_ERROR;
			}

			Long userID = userRS.getLong(1);

			ResultSet accountRS = DBConnector.execute(db, CREATE_ACCOUNT_QUERY, accountNumber, userID, tfaSecret, System.currentTimeMillis());
			if(!accountRS.next())
			{
				throw Exceptions.INTERNAL_ERROR;
			}
			db.commit();
		}
		catch(Exception ex)
		{
			if(Objects.nonNull(db))
			{
				db.rollback();
			}
			throw ex;
		}
	}

	public static Map<String, Object> getUser(Long customerID) throws Exception
	{
		ResultSet userRS = DBConnector.get(CXID_USER_QUERY, customerID);
		if(userRS.next())
		{
			if(Objects.equals(userRS.getLong("CustomerID"), customerID))
			{
				Map<String, Object> responseMap = new HashMap<>();
				responseMap.put("customer_id", userRS.getLong("CustomerID"));
				responseMap.put("first_name", userRS.getString("FirstName"));
				responseMap.put("last_name", userRS.getString("LastName"));
				responseMap.put("email_id", userRS.getString("Email"));
				responseMap.put("mobile_number", userRS.getLong("MobileNumber"));
				responseMap.put("date_of_birth", new SimpleDateFormat("dd MMM yyyy").format(new Date(userRS.getLong("DateOfBirth"))));
				responseMap.put("register_date", new SimpleDateFormat("dd MMM yyyy").format(new Date(userRS.getLong("SignupTime"))));
				responseMap.put("is_bank_staff", userRS.getBoolean("isBankStaff"));
				return responseMap;
			}
		}
		throw Exceptions.INVALID_CUSTOMERID;
	}

	public static void updateUser(Long customerID, Long mobileNumber, String emailID) throws Exception
	{
		ResultSet userRS = DBConnector.get(CXID_USER_QUERY, customerID);
		if(userRS.next())
		{
			if(Objects.equals(userRS.getLong("CustomerID"), customerID))
			{
				if(Objects.isNull(mobileNumber))
				{
					mobileNumber = userRS.getLong("MobileNumber");
				}

				if(Objects.isNull(emailID))
				{
					emailID = userRS.getString("Email");
				}

				validateMobileNumber(mobileNumber);
				validateEmail(emailID);
				emailID = emailID.trim();

				ResultSet userExistRS = DBConnector.get(USER_EXISTS_QUERY, emailID, mobileNumber);
				while(userExistRS.next())
				{
					if(!Objects.equals(userExistRS.getLong("CustomerID"), customerID))
					{
						throw Exceptions.USER_CONTACT_EXISTS;
					}
				}

				DBConnector.execute(UPDATE_USER_QUERY, emailID, mobileNumber, customerID);
				return;
			}
		}
		throw Exceptions.INVALID_CUSTOMERID;
	}

	public static boolean validateSession(Long customerID, String sessionID) throws Exception
	{
		ResultSet userRS = DBConnector.get(CXID_USER_QUERY, customerID);
		if(userRS.next())
		{
			if(Objects.equals(userRS.getLong("CustomerID"), customerID))
			{
				if(Objects.equals(userRS.getLong("ActiveSessionID"), Long.parseLong(sessionID, 36)))
				{
					if(isSessionActive(userRS.getLong("SessionLastAccessedTime")))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean isSessionActive(Long lastAccessTime)
	{
		Long sessionValidTime = System.currentTimeMillis() - (5 * 60 * 1000);
		if(sessionValidTime < lastAccessTime)
		{
			return true;
		}
		return false;
	}

	private static void validateEmail(String emailID) throws Exception
	{
		if(StringUtils.isBlank(emailID))
		{
			throw Exceptions.EMAIL_MANDATORY;
		}

		int atIndex = emailID.indexOf("@");
		int dotIndex = emailID.indexOf(".", atIndex);
		if(atIndex == 0 || atIndex == -1 || dotIndex == -1 || dotIndex == emailID.length() - 1)
		{
			throw Exceptions.INVALID_EMAIL;
		}
	}

	private static void validateMobileNumber(Long mobileNumber) throws Exception
	{
		if(Objects.isNull(mobileNumber) || String.valueOf(mobileNumber).length() != 10)
		{
			throw Exceptions.INVALID_MOBILE_NUMBER;
		}
	}
}
