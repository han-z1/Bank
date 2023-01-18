package org.bank.api;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.bank.TransactionType;
import org.bank.exception.Exceptions;
import org.bank.util.DBConnector;

public class TransactionAPI
{

	private static final String USER_ACCOUNT_QUERY = "SELECT BankAccountID, AccountNumber, AccountHolder, Balance, DayTxnLimit, TxnLimit FROM BankAccount WHERE AccountNumber = ?";
	private static final String USER_BENEFICIARY_QUERY = "SELECT User.UserID, User.CustomerID, BankAccount.BankAccountID, BankAccount.AccountNumber, BankAccount.AccountHolder, BankAccount.Balance, BankAccount.DayTxnLimit, BankAccount.TxnLimit, BankAccount.SecretTFA, Beneficiary.BeneficiaryID, Beneficiary.NickName, Beneficiary.OwnerAccount, Beneficiary.AccountNumber, Beneficiary.UPIID, Beneficiary.MobileNumber, Beneficiary.RegisteredTime FROM User INNER JOIN BankAccount ON BankAccount.AccountHolder = User.UserID LEFT JOIN Beneficiary ON Beneficiary.OwnerAccount = BankAccount.BankAccountID WHERE User.CustomerID = ? AND Beneficiary.BeneficiaryID = ?";
	private static final String MOBILE_ACCOUNT_QUERY = "SELECT User.UserID, User.CustomerID, User.MobileNumber, BankAccount.BankAccountID, BankAccount.AccountNumber, BankAccount.AccountHolder, BankAccount.Balance, BankAccount.DayTxnLimit, BankAccount.TxnLimit FROM User INNER JOIN BankAccount ON BankAccount.AccountHolder = User.UserID WHERE User.MobileNumber = ?";
	private static final String DAY_TXN_QUERY = "SELECT Beneficiary.BeneficiaryID, Beneficiary.OwnerAccount, Transfer.TransactionID, Transfer.BeneficiaryID, Transfer.Amount, Transfer.TxnTime FROM Beneficiary INNER JOIN Transfer ON Transfer.BeneficiaryID = Beneficiary.BeneficiaryID WHERE Beneficiary.OwnerAccount = ? AND Transfer.TxnTime BETWEEN ? AND ?";
	private static final String TXN_QUERY = "INSERT INTO TABLE (BankAccountID, UPIStoreID, Amount, Description, TxnTime) VALUES(?, ?, ?, ?, ?)";
	private static final String TRANSFER_QUERY = "INSERT INTO Bank.Transfer (WithdrawalID, DepositID, BeneficiaryID, Amount, Description, TxnTime) VALUES(?, ?, ?, ?, ?, ?)";
	private static final String STATEMENT_QUERY = "INSERT INTO Statement (BankAccountID, Type, TransactionID, Amount, PostTxnBalance, TxnTime) VALUES(?, ?, ?, ?, ?, ?)";
	private static final String BALANCE_UPDATE_QUERY = "UPDATE BankAccount SET Balance = ? WHERE BankAccountID = ?";
	private static final String UPI_QUERY = "SELECT UPIStoreID, BankAccountID, UPIID, PIN FROM UPIStore WHERE UPIID = ?";
	private static final String CX_ACCOUNT_QUERY = "SELECT User.UserID, User.CustomerID, User.MobileNumber, BankAccount.BankAccountID, BankAccount.AccountNumber, BankAccount.AccountHolder, BankAccount.Balance, BankAccount.DayTxnLimit, BankAccount.TxnLimit FROM User INNER JOIN BankAccount ON BankAccount.AccountHolder = User.UserID WHERE User.CustomerID = ?";
	private static final String TXN_STATEMENT_QUERY = "SELECT Statement.StatementID, Statement.BankAccountID, Statement.Type, Statement.TransactionID, Statement.Amount, Statement.PostTxnBalance, Statement.TxnTime, Deposit.DepositID, Deposit.Amount, Deposit.Description, Withdrawal.WithdrawalID, Withdrawal.Amount, Withdrawal.Description, Transfer.TransactionID, Transfer.BeneficiaryID, Transfer.Amount, Transfer.Description, Beneficiary.BeneficiaryID, Beneficiary.NickName FROM Statement LEFT JOIN Deposit ON Deposit.DepositID = Statement.TransactionID LEFT JOIN Withdrawal ON Withdrawal.WithdrawalID = Statement.TransactionID LEFT JOIN Transfer ON Transfer.TransactionID = Statement.TransactionID LEFT JOIN Beneficiary ON Transfer.BeneficiaryID = Beneficiary.BeneficiaryID WHERE Statement.BankAccountID = ? ORDER BY Statement.TxnTime DESC";

	public static void createTransaction(Integer transactionType, Long accountNumber, String description, BigDecimal amount) throws Exception
	{
		if(!Objects.equals(TransactionType.DEPOSIT.ordinal(), transactionType) && !Objects.equals(TransactionType.WITHDRAW.ordinal(), transactionType))
		{
			throw Exceptions.STAFF_TRANSFER_NOT_ALLOWED;
		}

		if(StringUtils.isBlank(description))
		{
			throw Exceptions.DESCRIPTION_MANDATORY;
		}

		if(Objects.isNull(amount))
		{
			throw Exceptions.AMOUNT_MANDATORY;
		}

		ResultSet accountRS = DBConnector.get(USER_ACCOUNT_QUERY, accountNumber);
		if(!accountRS.next())
		{
			throw Exceptions.ACCOUNT_NOT_EXIST;
		}

		Long bankAccountID = accountRS.getLong("BankAccountID");

		BigDecimal balance = accountRS.getBigDecimal("Balance");
		if(Objects.equals(transactionType, TransactionType.WITHDRAW.ordinal()) && amount.compareTo(balance) > 0)
		{
			throw Exceptions.INSUFFICIENT_BALANCE;
		}

		Connection db = null;
		try
		{
			db = DBConnector.getConnection();
			db.setAutoCommit(false);

			ResultSet txnRS = DBConnector.execute(db, getTxnQuery(transactionType), bankAccountID, null, amount, description, System.currentTimeMillis());
			txnRS.next();
			Long txnID = txnRS.getLong(1);

			BigDecimal postTxnBalance = getPostTxnAmount(transactionType, balance, amount);
			DBConnector.execute(db, STATEMENT_QUERY, bankAccountID, transactionType, txnID, amount, postTxnBalance, System.currentTimeMillis());
			DBConnector.execute(db, BALANCE_UPDATE_QUERY, postTxnBalance, bankAccountID);
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

	public static void createTransfer(Long customerID, Long beneficiaryID, String description, BigDecimal amount, String upiID, String password) throws Exception
	{

		ResultSet accountRS = DBConnector.get(USER_BENEFICIARY_QUERY, customerID, beneficiaryID);
		if(!accountRS.next())
		{
			throw Exceptions.CUSTOMER_BENEFICIARY_MISMATCH;
		}

		if(StringUtils.isBlank(description))
		{
			throw Exceptions.DESCRIPTION_MANDATORY;
		}

		if(Objects.isNull(amount))
		{
			throw Exceptions.AMOUNT_MANDATORY;
		}

		Long accountID = accountRS.getLong("BankAccount.BankAccountID");
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Long dayStartTime = cal.getTimeInMillis();
		cal.add(Calendar.DATE, 1);
		Long dayEndTime = cal.getTimeInMillis();

		BigDecimal balance = accountRS.getBigDecimal("BankAccount.Balance");
		if(balance.compareTo(amount) < 0)
		{
			throw Exceptions.INSUFFICIENT_BALANCE;
		}

		BigDecimal txnLimit = accountRS.getBigDecimal("BankAccount.TxnLimit");
		if(txnLimit.compareTo(amount) < 0)
		{
			throw Exceptions.TXN_LIMIT_EXCEEDED;
		}

		ResultSet dayTxnRS = DBConnector.get(DAY_TXN_QUERY, accountID, dayStartTime, dayEndTime);
		BigDecimal dayTxnTotal = BigDecimal.ZERO;
		while(dayTxnRS.next())
		{
			dayTxnTotal = dayTxnTotal.add(dayTxnRS.getBigDecimal("Transfer.Amount"));
		}

		BigDecimal dayTxnLimit = accountRS.getBigDecimal("BankAccount.DayTxnLimit");
		if(dayTxnLimit.compareTo(dayTxnTotal.add(amount)) < 0)
		{
			throw Exceptions.PER_TXN_LIMIT_EXCEEDED;
		}

		String beneficiaryUPIID = accountRS.getString("Beneficiary.UPIID");
		BigInteger mobileNumber = (BigInteger) accountRS.getObject("Beneficiary.MobileNumber");
		BigInteger accountNumber = (BigInteger) accountRS.getObject("Beneficiary.AccountNumber");

		Long beneficiaryAccountID = null;
		Long upiStoreID = null;

		if(Objects.nonNull(beneficiaryUPIID))
		{
			ResultSet upiRS = DBConnector.get(UPI_QUERY, beneficiaryUPIID);
			if(!upiRS.next())
			{
				throw Exceptions.INVALID_UPIID;
			}
			upiStoreID = upiRS.getLong("UPIStoreID");
			beneficiaryAccountID = upiRS.getLong("BankAccountID");
		}

		if(Objects.nonNull(mobileNumber))
		{
			ResultSet mobileAccRS = DBConnector.get(MOBILE_ACCOUNT_QUERY, mobileNumber);
			if(!mobileAccRS.next())
			{
				throw Exceptions.ACCOUNT_NOT_EXIST;
			}
			beneficiaryAccountID = mobileAccRS.getLong("BankAccount.BankAccountID");
		}

		if(Objects.nonNull(accountNumber))
		{
			ResultSet beneficiaryAccRS = DBConnector.get(USER_ACCOUNT_QUERY, accountNumber);
			if(!beneficiaryAccRS.next())
			{
				throw Exceptions.ACCOUNT_NOT_EXIST;
			}
			beneficiaryAccountID = beneficiaryAccRS.getLong("BankAccountID");
		}

		if(Objects.nonNull(beneficiaryUPIID))
		{
			ResultSet upiRS = DBConnector.get(UPI_QUERY, upiID);
			if(!upiRS.next())
			{
				throw Exceptions.UPIID_MISMATCH;
			}

			if(!Objects.equals(upiRS.getLong("BankAccountID"), accountID))
			{
				throw Exceptions.UPIID_MISMATCH;
			}

			if(!Objects.equals(String.valueOf(upiRS.getLong("PIN")), password))
			{
				throw Exceptions.UPIID_PIN_INCORRECT;
			}
		}
		else
		{
			String tfaSecret = accountRS.getString("BankAccount.SecretTFA");

			TimeProvider timeProvider = new SystemTimeProvider();
			CodeGenerator codeGenerator = new DefaultCodeGenerator();
			CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
			if(!verifier.isValidCode(tfaSecret, password))
			{
				throw Exceptions.INVALID_TOTP;
			}
		}

		Connection db = null;
		try
		{
			db = DBConnector.getConnection();
			db.setAutoCommit(false);
			ResultSet withdrawRS = DBConnector.execute(db, getTxnQuery(TransactionType.WITHDRAW.ordinal()), accountID, null, amount, description, System.currentTimeMillis());
			withdrawRS.next();
			Long withdrawID = withdrawRS.getLong(1);

			ResultSet depositRS = DBConnector.execute(db, getTxnQuery(TransactionType.DEPOSIT.ordinal()), beneficiaryAccountID, upiStoreID, amount, description, System.currentTimeMillis());
			depositRS.next();
			Long depositID = depositRS.getLong(1);

			ResultSet transferRS = DBConnector.execute(db, TRANSFER_QUERY, withdrawID, depositID, beneficiaryID, amount, description, System.currentTimeMillis());
			transferRS.next();
			Long transferID  = transferRS.getLong(1);

			BigDecimal postTxnBalance = getPostTxnAmount(TransactionType.WITHDRAW.ordinal(), balance, amount);
			DBConnector.execute(db, STATEMENT_QUERY, accountID, TransactionType.TRANSFER_OUT.ordinal(), transferID, amount, postTxnBalance, System.currentTimeMillis());

			DBConnector.execute(db, BALANCE_UPDATE_QUERY, postTxnBalance, accountID);

			BigDecimal beneficiaryPostTxnBalance = getPostTxnAmount(TransactionType.DEPOSIT.ordinal(), balance, amount);
			DBConnector.execute(db, STATEMENT_QUERY, beneficiaryAccountID, TransactionType.TRANSFER_IN.ordinal(), transferID, amount, beneficiaryPostTxnBalance, System.currentTimeMillis());

			DBConnector.execute(db, BALANCE_UPDATE_QUERY, beneficiaryPostTxnBalance, beneficiaryAccountID);
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

	public static Map<String, Object> getTransactions(Long customerID) throws Exception
	{
		ResultSet accountRS = DBConnector.get(CX_ACCOUNT_QUERY, customerID);
		if(!accountRS.next())
		{
			throw Exceptions.INVALID_CUSTOMERID;
		}
		Map<String, Object> respMap = new HashMap<>();

		respMap.put("account_number", accountRS.getLong("BankAccount.AccountNumber"));
		respMap.put("customer_id", accountRS.getLong("User.CustomerID"));
		respMap.put("balance", accountRS.getLong("BankAccount.Balance"));

		SimpleDateFormat timeFormat = new SimpleDateFormat("dd MMM yyyy HH:mm a");
		Long accountID = accountRS.getLong("BankAccount.BankAccountID");
		ResultSet statementRS = DBConnector.get(TXN_STATEMENT_QUERY, accountID);
		List<Map<String, Object>> transactions = new ArrayList<>();
		while(statementRS.next())
		{
			HashMap<String, Object> transaction = new HashMap<>();
			Integer transactionType = statementRS.getInt("Statement.Type");
			transaction.put("type", getType(transactionType));
			transaction.put("statement_type", getStatementType(transactionType));
			if(isCredit(transactionType))
			{
				transaction.put("credit_amount", statementRS.getBigDecimal("Statement.Amount"));
				transaction.put("debit_amount", "");
			}
			if(isDebit(transactionType))
			{
				transaction.put("debit_amount", statementRS.getBigDecimal("Statement.Amount"));
				transaction.put("credit_amount", "");
			}
			transaction.put("balance", statementRS.getBigDecimal("Statement.PostTxnBalance"));

			transaction.put("txn_time", timeFormat.format(new Date(statementRS.getLong("Statement.TxnTime"))));
			transaction.put("description", statementRS.getString(getType(transactionType) + ".Description"));
			transaction.put("beneficiary_name", "");
			if(Objects.equals(TransactionType.TRANSFER_OUT.ordinal(), transactionType))
			{
				transaction.put("beneficiary_name", statementRS.getString("Beneficiary.NickName"));
			}
			transactions.add(transaction);
		}
		respMap.put("transactions", transactions);
		return respMap;
	}

	private static boolean isCredit(Integer transactionType)
	{
		if(Objects.equals(TransactionType.DEPOSIT.ordinal(), transactionType) || Objects.equals(TransactionType.TRANSFER_IN.ordinal(), transactionType))
		{
			return true;
		}
		return false;
	}

	private static boolean isDebit(Integer transactionType)
	{
		if(Objects.equals(TransactionType.WITHDRAW.ordinal(), transactionType) || Objects.equals(TransactionType.TRANSFER_OUT.ordinal(), transactionType))
		{
			return true;
		}
		return false;
	}

	private static String getStatementType(Integer transactionType)
	{
		if(isCredit(transactionType))
		{
			return "Credit";
		}
		if(isDebit(transactionType))
		{
			return "Debit";
		}
		return "";
	}

	private static String getType(Integer transactionType)
	{
		if(Objects.equals(TransactionType.DEPOSIT.ordinal(), transactionType))
		{
			return "Deposit";
		}
		if(Objects.equals(TransactionType.WITHDRAW.ordinal(), transactionType))
		{
			return "Withdrawal";
		}
		if(Objects.equals(TransactionType.TRANSFER_OUT.ordinal(), transactionType) || Objects.equals(TransactionType.TRANSFER_IN.ordinal(), transactionType))
		{
			return "Transfer";
		}
		return "";
	}

	private static BigDecimal getPostTxnAmount(Integer transactionType, BigDecimal currentBalance, BigDecimal amount)
	{
		if(Objects.equals(TransactionType.DEPOSIT.ordinal(), transactionType))
		{
			return currentBalance.add(amount);
		}
		if(Objects.equals(TransactionType.WITHDRAW.ordinal(), transactionType))
		{
			return currentBalance.subtract(amount);
		}
		return currentBalance;
	}

	private static String getTxnQuery(Integer transactionType)
	{
		if(Objects.equals(TransactionType.DEPOSIT.ordinal(), transactionType))
		{
			return TXN_QUERY.replace("TABLE", "Deposit");
		}
		if(Objects.equals(TransactionType.WITHDRAW.ordinal(), transactionType))
		{
			return TXN_QUERY.replace("TABLE", "Withdrawal");
		}
		return null;
	}
}
