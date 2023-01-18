package org.bank.exception;

import java.io.IOException;

public class ValidationException extends IOException
{
	int code;

	String message;

	ValidationException(int code, String message)
	{
		this.code = code;
		this.message = message;
	}

	public int getCode()
	{
		return code;
	}

	@Override public String getMessage()
	{
		return message;
	}
}
