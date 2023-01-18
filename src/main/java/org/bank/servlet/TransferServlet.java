package org.bank.servlet;

import java.io.IOException;
import java.math.BigDecimal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bank.api.TransactionAPI;
import org.bank.exception.Exceptions;
import org.bank.exception.ValidationException;
import org.bank.util.ResponseHandler;

public class TransferServlet extends HttpServlet
{
	@Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		Long customerID = (Long) req.getAttribute("CustomerID");
		String description = req.getParameter("description");
		Long beneficiaryID = Long.parseLong(req.getParameter("beneficiary_id"));
		BigDecimal amount = new BigDecimal(req.getParameter("amount"));
		String upiID = req.getParameter("upi_id");
		String password = req.getParameter("password");
		try
		{
			TransactionAPI.createTransfer(customerID, beneficiaryID, description, amount, upiID, password);
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
