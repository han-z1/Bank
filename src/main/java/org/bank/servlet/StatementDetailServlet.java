package org.bank.servlet;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bank.api.TransactionAPI;
import org.bank.exception.Exceptions;
import org.bank.exception.ValidationException;
import org.bank.util.ResponseHandler;

public class StatementDetailServlet extends HttpServlet
{
	@Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		Long customerID = (Long) req.getAttribute("CustomerID");
		try
		{
			Map<String, Object> respMap = TransactionAPI.getTransactions(customerID);
			ResponseHandler.respond(resp, 200, 0, "success", respMap);
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
