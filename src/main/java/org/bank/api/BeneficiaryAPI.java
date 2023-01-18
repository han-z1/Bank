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

public class BeneficiaryAPI
{
	private static final String CREATE_BENEFICIARY_QUERY = "INSERT INTO Beneficiary (NickName, OwnerAccount, AccountNumber, UPIID, MobileNumber, RegisteredTime, LastAccessTime) VALUES(?, ?, ?, ?, ?, ?, ?)";
	private static final String BENEFICIARY_EXISTS_QUERY = "SELECT * FROM TABLE WHERE COLUMN = ?";
	private static final String USER_ACCOUNT_QUERY = "SELECT User.UserID, User.CustomerID, BankAccount.BankAccountID, BankAccount.AccountNumber, BankAccount.AccountHolder FROM User INNER JOIN BankAccount ON BankAccount.AccountHolder = User.UserID WHERE User.CustomerID = ?";
	private static final String BENEFICIARY_LIST_QUERY = "SELECT User.UserID, User.CustomerID, BankAccount.BankAccountID, BankAccount.AccountNumber, BankAccount.AccountHolder, Beneficiary.BeneficiaryID, Beneficiary.NickName, Beneficiary.OwnerAccount, Beneficiary.AccountNumber, Beneficiary.UPIID, Beneficiary.MobileNumber, Beneficiary.RegisteredTime FROM User INNER JOIN BankAccount ON BankAccount.AccountHolder = User.UserID LEFT JOIN Beneficiary ON Beneficiary.OwnerAccount = BankAccount.BankAccountID WHERE User.CustomerID = ?";

	public static List<Map<String, Object>> getBeneficiaries(Long customerID) throws Exception
	{
		ResultSet beneficiaryRS = DBConnector.get(BENEFICIARY_LIST_QUERY, customerID);
		List<Map<String, Object>> beneficiaryList = new ArrayList<>();
		while(beneficiaryRS.next())
		{
			Object beneficiaryID = beneficiaryRS.getObject("Beneficiary.BeneficiaryID");
			if(Objects.nonNull(beneficiaryID))
			{
				Map<String, Object> beneficiaryDetails = new HashMap<>();
				beneficiaryDetails.put("beneficiary_id", beneficiaryID);
				beneficiaryDetails.put("nick_name", beneficiaryRS.getString("Beneficiary.NickName"));
				if(Objects.nonNull(beneficiaryRS.getObject("Beneficiary.AccountNumber")))
				{
					beneficiaryDetails.put("type", "Account Number");
					beneficiaryDetails.put("reference", beneficiaryRS.getObject("Beneficiary.AccountNumber"));
				}
				if(Objects.nonNull(beneficiaryRS.getObject("Beneficiary.UPIID")))
				{
					beneficiaryDetails.put("type", "UPI");
					beneficiaryDetails.put("reference", beneficiaryRS.getObject("Beneficiary.UPIID"));
				}
				if(Objects.nonNull(beneficiaryRS.getObject("Beneficiary.MobileNumber")))
				{
					beneficiaryDetails.put("type", "Mobile Number");
					beneficiaryDetails.put("reference", beneficiaryRS.getObject("Beneficiary.MobileNumber"));
				}
				beneficiaryDetails.put("register_date", new SimpleDateFormat("dd MMM yyyy").format(new Date(beneficiaryRS.getLong("Beneficiary.RegisteredTime"))));
				beneficiaryList.add(beneficiaryDetails);
			}
		}
		return beneficiaryList;
	}

	public static void createBeneficiary(Long customerID, String nickName, Long accountNumber, String upiID, Long mobileNumber) throws Exception
	{
		if(StringUtils.isBlank(nickName))
		{
			throw Exceptions.NICK_NAME_MANDATORY;
		}

		Map<String, Object> valueMap = new HashMap<>();
		String tableName = null;
		if(Objects.nonNull(accountNumber))
		{
			valueMap.put("AccountNumber", accountNumber);
			tableName = "BankAccount";
		}

		if(Objects.nonNull(upiID))
		{
			valueMap.put("UPIID", upiID);
			tableName = "UPIStore";
		}

		if(Objects.nonNull(mobileNumber))
		{
			valueMap.put("MobileNumber", mobileNumber);
			tableName = "User";
		}

		if(valueMap.entrySet().size() != 1)
		{
			throw Exceptions.ONLY_ONE_BENEFICIARY_REFERENCE;
		}

		Map.Entry<String, Object> columnVsValue = valueMap.entrySet().stream().findFirst().get();

		String query = BENEFICIARY_EXISTS_QUERY.replace("TABLE", tableName);
		query = query.replace("COLUMN", columnVsValue.getKey());
		ResultSet beneficiaryExistRS = DBConnector.get(query, columnVsValue.getValue());
		if(!beneficiaryExistRS.next())
		{
			throw Exceptions.BENEFICIARY_NOT_EXISTS;
		}

		ResultSet accountRS = DBConnector.get(USER_ACCOUNT_QUERY, customerID);
		if(accountRS.next())
		{
			if(Objects.equals(accountRS.getLong("User.CustomerID"), customerID))
			{
				Long bankAccoutID = accountRS.getLong("BankAccount.BankAccountID");

				DBConnector.execute(CREATE_BENEFICIARY_QUERY, nickName, bankAccoutID, accountNumber, upiID, mobileNumber, System.currentTimeMillis(), System.currentTimeMillis());
				return;
			}
		}
		throw Exceptions.INVALID_CUSTOMERID;
	}
}
