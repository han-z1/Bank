package org.bank.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
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

public class UserRegisterServlet extends HttpServlet
{
	@Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		try
		{
			String firstName = req.getParameter("first_name");
			String lastName = req.getParameter("last_name");
			String dateOfBirthStr = req.getParameter("date_of_birth");
			if(StringUtils.isEmpty(dateOfBirthStr))
			{
				throw Exceptions.DOB_MANDATORY;
			}

			Long dateOfBirth = null;
			try
			{
				dateOfBirth = new SimpleDateFormat("yyyy-MM-dd").parse(dateOfBirthStr).getTime();
			}
			catch(Exception ex)
			{
				throw Exceptions.INVALID_DATEFORMAT;
			}
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
			String password = req.getParameter("password");
			UserAPI.createUser(firstName, lastName, dateOfBirth, mobileNumber, emailID, password);
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
