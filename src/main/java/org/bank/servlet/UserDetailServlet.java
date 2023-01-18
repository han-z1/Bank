package org.bank.servlet;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bank.api.UserAPI;
import org.bank.exception.Exceptions;
import org.bank.exception.ValidationException;
import org.bank.util.ResponseHandler;

public class UserDetailServlet extends HttpServlet
{
	@Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		Long customerID = (Long) req.getAttribute("CustomerID");
		try
		{
			Map<String, Object> userMap = UserAPI.getUser(customerID);
			ResponseHandler.respond(resp, 200, 0, "success", userMap);
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
		try
		{
			Long customerID = (Long) req.getAttribute("CustomerID");
			String mobileStr = req.getParameter("mobile_number");
			if(StringUtils.isEmpty(mobileStr))
			{
				throw Exceptions.MOBILE_MANDATORY;
			}
			if(!NumberUtils.isDigits(mobileStr))
			{
				throw Exceptions.INVALID_MOBILE_NUMBER;
			}
			Long mobileNumber = Optional.ofNullable(mobileStr).map(Long::parseLong).orElse(null);
			String emailID = req.getParameter("email_id");
			UserAPI.updateUser(customerID, mobileNumber, emailID);
			ResponseHandler.respond(resp, 200, 0, "success");
		}
		catch(ValidationException ex)
		{
			throw ex;
		}
		catch(Exception exception)
		{
			throw Exceptions.INTERNAL_ERROR;
		}
	}
}
