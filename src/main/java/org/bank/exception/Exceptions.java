package org.bank.exception;

public class Exceptions
{
	public static final ServerException INTERNAL_ERROR = new ServerException();
	public static final ValidationException USER_ALREADY_EXISTS = new ValidationException(1001, "User has already registered. Please login.");
	public static final ValidationException USER_NAME_MANDATORY = new ValidationException(1002, "First Name is mandatory to sign up!");
	public static final ValidationException ONLY_ADULT_SIGNUP = new ValidationException(1003, "You should be 18 years or above to sign up!");
	public static final ValidationException INVALID_MOBILE_NUMBER = new ValidationException(1004, "Mobile number entered is invalid!");
	public static final ValidationException EMAIL_MANDATORY = new ValidationException(1005, "Email ID is mandatory!");
	public static final ValidationException INVALID_EMAIL = new ValidationException(1006, "Email ID entered is invalid!");
	public static final ValidationException PASSWORD_MIN_LEN = new ValidationException(1007, "Password should be atleast 8 characters!");
	public static final ValidationException DOB_MANDATORY = new ValidationException(1008, "Date of Birth is mandatory!");
	public static final ValidationException INVALID_DATEFORMAT = new ValidationException(1009, "Date format invalid!");
	public static final ValidationException MOBILE_MANDATORY = new ValidationException(1010, "Mobile number is mandatory!");
	public static final ValidationException ACTIVE_SESSION_EXISTS = new ValidationException(1011, "Active session already exists! You can't login now.");
	public static final ValidationException INVALID_CREDENTIALS = new ValidationException(1012, "Customer ID/Password is incorrect!");
	public static final ValidationException INVALID_CUSTOMERID = new ValidationException(1013, "Customer ID is invalid!");
	public static final ValidationException USER_CONTACT_EXISTS = new ValidationException(1014, "User with this contact details already exists!");
	public static final ValidationException UPI_MANDATORY = new ValidationException(1015, "UPI ID is mandatory to create UPI!");
	public static final ValidationException INVALID_UPI_FORMAT = new ValidationException(1016, "UPI format is invalid!");
	public static final ValidationException UPI_END_FORMAT = new ValidationException(1017, "UPI ID should end with @okhdfcbank!");
	public static final ValidationException UPI_PIN_SIZE = new ValidationException(1018, "UPI Pin should be 6 digits!");
	public static final ValidationException UPI_ID_EXISTS = new ValidationException(1019, "UPI ID is already taken!");
	public static final ValidationException NICK_NAME_MANDATORY = new ValidationException(1020, "Nickname is mandatory!");
	public static final ValidationException ONLY_ONE_BENEFICIARY_REFERENCE = new ValidationException(1021, "Beneficiary can be based on only one of account number, upi id or mobile number!");
	public static final ValidationException BENEFICIARY_NOT_EXISTS = new ValidationException(1022, "Beneficiary doesn't exist!");
	public static final ValidationException INVALID_ACCOUNT_NUMBER = new ValidationException(1023, "Account number entered is invalid!");
	public static final ValidationException STAFF_TRANSFER_NOT_ALLOWED = new ValidationException(1024, "Staff are not allowed to transfer funds!");
	public static final ValidationException DESCRIPTION_MANDATORY = new ValidationException(1025, "Description is mandatory for transactions!");
	public static final ValidationException ACCOUNT_NOT_EXIST = new ValidationException(1025, "Account number entered doesn't exists!");
	public static final ValidationException INSUFFICIENT_BALANCE = new ValidationException(1026, "Insufficient funds to proceed with the transaction!");
	public static final ValidationException AMOUNT_MANDATORY = new ValidationException(1027, "Amount is mandatory!");
	public static final ValidationException CUSTOMER_BENEFICIARY_MISMATCH = new ValidationException(1028, "Invalid Beneficiary ID!");
	public static final ValidationException PER_TXN_LIMIT_EXCEEDED = new ValidationException(1029, "This transaction exceeds per day limit!");
	public static final ValidationException TXN_LIMIT_EXCEEDED = new ValidationException(1030, "Transaction amount exceeds per transaction limit!");
	public static final ValidationException INVALID_UPIID = new ValidationException(1031, "UPI ID is invalid!");
	public static final ValidationException INVALID_TOTP = new ValidationException(1032, "Invalid TOTP code!");
	public static final ValidationException UPIID_MISMATCH = new ValidationException(1033, "This UPI ID doesn't belong to you!");
	public static final ValidationException UPIID_PIN_INCORRECT = new ValidationException(1034, "UPI Pin entered is incorrect!");
}
