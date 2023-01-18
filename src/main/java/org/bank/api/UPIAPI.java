package org.bank.api;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.bank.exception.Exceptions;
import org.bank.util.DBConnector;
import org.bank.util.ResponseHandler;

public class UPIAPI
{

	private static final String CREATE_UPI_QUERY = "INSERT INTO UPIStore (BankAccountID, UPIID, PIN, RegisteredDate) VALUES(?, ?, ?, ?)";
	private static final String UPI_EXISTS_QUERY = "SELECT UPIStoreID, UPIID FROM UPIStore WHERE UPIID = ?";
	private static final String USER_ACCOUNT_QUERY = "SELECT User.UserID, User.CustomerID, BankAccount.BankAccountID, BankAccount.AccountNumber, BankAccount.AccountHolder FROM User INNER JOIN BankAccount ON BankAccount.AccountHolder = User.UserID WHERE User.CustomerID = ?";
	private static final String UPI_LIST_QUERY = "SELECT User.UserID, User.CustomerID, BankAccount.BankAccountID, BankAccount.AccountNumber, BankAccount.AccountHolder, UPIStore.UPIStoreID, UPIStore.BankAccountID, UPIStore.UPIID, UPIStore.PIN, UPIStore.Status, UPIStore.RegisteredDate FROM User INNER JOIN BankAccount ON BankAccount.AccountHolder = User.UserID LEFT JOIN UPIStore ON UPIStore.BankAccountID = BankAccount.BankAccountID WHERE User.CustomerID = ?";

	public static List<Map<String, Object>> getUPIs(Long customerID) throws Exception
	{
		ResultSet upiRS = DBConnector.get(UPI_LIST_QUERY, customerID);
		List<Map<String, Object>> upiList = new ArrayList<>();
		while(upiRS.next())
		{
			Object upiStoreID = upiRS.getObject("UPIStore.UPIStoreID");
			if(Objects.nonNull(upiStoreID))
			{
				Map<String, Object> upiDetails = new HashMap<>();
				upiDetails.put("upi_store_id", upiStoreID);
				upiDetails.put("upi_id", upiRS.getString("UPIStore.UPIID"));
				upiDetails.put("register_date", new SimpleDateFormat("dd MMM yyyy").format(new Date(upiRS.getLong("UPIStore.RegisteredDate"))));
				upiList.add(upiDetails);
			}
		}
		return upiList;
	}

	public static void createUPI(Long customerID, String upiID, Long upiPin) throws Exception
	{
		if(Objects.isNull(upiID))
		{
			throw Exceptions.UPI_MANDATORY;
		}

		Integer atIndex = upiID.indexOf("@");

		if(atIndex == -1 || upiID.indexOf("@", atIndex + 1) != -1)
		{
			throw Exceptions.INVALID_UPI_FORMAT;
		}

		if(!upiID.endsWith("@okhdfcbank"))
		{
			throw Exceptions.UPI_END_FORMAT;
		}

		if(!StringUtils.isAlphanumeric(upiID.substring(0, atIndex-1)))
		{
			throw Exceptions.INVALID_UPI_FORMAT;
		}

		if(upiPin < 100000 || upiPin > 999999)
		{
			throw Exceptions.UPI_PIN_SIZE;
		}

		ResultSet upiExistRS = DBConnector.get(UPI_EXISTS_QUERY, upiID);
		if(upiExistRS.next())
		{
			throw Exceptions.UPI_ID_EXISTS;
		}

		ResultSet accountRS = DBConnector.get(USER_ACCOUNT_QUERY, customerID);
		if(accountRS.next())
		{
			if(Objects.equals(accountRS.getLong("User.CustomerID"), customerID))
			{
				Long bankAccoutID = accountRS.getLong("BankAccount.BankAccountID");

				DBConnector.execute(CREATE_UPI_QUERY, bankAccoutID, upiID, upiPin, System.currentTimeMillis());
				return;
			}
		}
		throw Exceptions.INVALID_CUSTOMERID;
	}
}
