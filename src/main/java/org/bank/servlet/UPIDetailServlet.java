package org.bank.servlet;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bank.api.UPIAPI;
import org.bank.exception.Exceptions;
import org.bank.exception.ValidationException;
import org.bank.util.ResponseHandler;

public class UPIDetailServlet extends HttpServlet
{
	@Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		Long customerID = (Long) req.getAttribute("CustomerID");
		try
		{
			List<Map<String, Object>> upiList = UPIAPI.getUPIs(customerID);
			ResponseHandler.respond(resp, 200, 0, "success", upiList);
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

	@Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		Long customerID = (Long) req.getAttribute("CustomerID");
		String upiID = req.getParameter("upi_id");
		String upiPinStr = req.getParameter("upi_pin");
		if(!NumberUtils.isDigits(upiPinStr))
		{
			throw Exceptions.UPI_PIN_SIZE;
		}

		try
		{
			UPIAPI.createUPI(customerID, upiID, Long.parseLong(upiPinStr));
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
