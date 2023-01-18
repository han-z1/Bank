package org.bank.servlet;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bank.api.BeneficiaryAPI;
import org.bank.api.UPIAPI;
import org.bank.exception.Exceptions;
import org.bank.exception.ValidationException;
import org.bank.util.ResponseHandler;

public class BeneficiaryServlet extends HttpServlet
{
	@Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		Long customerID = (Long) req.getAttribute("CustomerID");
		try
		{
			List<Map<String, Object>> beneficiaryList = BeneficiaryAPI.getBeneficiaries(customerID);
			ResponseHandler.respond(resp, 200, 0, "success", beneficiaryList);
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
		String nickName = req.getParameter("nick_name");
		String accountNumber = req.getParameter("account_number");
		if(StringUtils.isNotEmpty(accountNumber) && !NumberUtils.isDigits(accountNumber))
		{
			throw Exceptions.INVALID_ACCOUNT_NUMBER;
		}
		String upiID = req.getParameter("upi_id");
		String mobileNumber = req.getParameter("mobile_number");
		if(StringUtils.isNotEmpty(mobileNumber) && !NumberUtils.isDigits(mobileNumber))
		{
			throw Exceptions.INVALID_MOBILE_NUMBER;
		}

		try
		{
			BeneficiaryAPI.createBeneficiary(customerID, nickName, NumberUtils.createLong(accountNumber), upiID, NumberUtils.createLong(mobileNumber));
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
