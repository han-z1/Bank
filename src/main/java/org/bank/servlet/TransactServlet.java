package org.bank.servlet;

import java.io.IOException;
import java.math.BigDecimal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.math.NumberUtils;
import org.bank.api.TransactionAPI;
import org.bank.exception.Exceptions;
import org.bank.exception.ValidationException;
import org.bank.util.ResponseHandler;

public class TransactServlet extends HttpServlet
{
	@Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		String accountNumber = req.getParameter("account_number");
		if(!NumberUtils.isDigits(accountNumber))
		{
			throw Exceptions.INVALID_ACCOUNT_NUMBER;
		}
		String description = req.getParameter("description");
		Integer transactionType = Integer.parseInt(req.getParameter("transaction_type"));
		BigDecimal amount = new BigDecimal(req.getParameter("amount"));
		try
		{
			TransactionAPI.createTransaction(transactionType, Long.parseLong(accountNumber), description, amount);
			ResponseHandler.respond(resp, 200, 0, "success");
		}
		catch(ValidationException ex)
		{
			throw ex;
		}
		catch(Exception ex)
		{
			throw Exceptions.INTERNAL_ERROR;
		}
	}
}
