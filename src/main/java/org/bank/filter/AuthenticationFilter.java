package org.bank.filter;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.math.NumberUtils;
import org.bank.api.UserAPI;
import org.bank.exception.Exceptions;
import org.bank.exception.ValidationException;
import org.bank.util.DBConnector;
import org.bank.util.ResponseHandler;

public class AuthenticationFilter implements Filter
{

	private static final String UPDATE_ACCESS_TIME_QUERY = "UPDATE User SET SessionLastAccessedTime = ? WHERE CustomerID = ?";
	FilterConfig config = null;

	@Override public void init(FilterConfig config) throws ServletException
	{
		this.config = config;
	}

	@Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		Cookie[] cookies = Optional.ofNullable(((HttpServletRequest) request).getCookies()).orElse(new Cookie[0]);
		Map<String, String> cookieVsValue = Arrays.asList(cookies).stream().collect(Collectors.toMap(Cookie::getName, Cookie::getValue));

		try
		{
			String customerIDStr = cookieVsValue.get("CustomerID");
			Long customerID = NumberUtils.isDigits(customerIDStr)? Long.parseLong(customerIDStr): null;
			String sessionID = cookieVsValue.get("SessionID");

			if(Objects.isNull(customerID) || Objects.isNull(sessionID))
			{
				customerIDStr = ((HttpServletRequest) request).getParameter("customer_id");
				customerID = NumberUtils.isDigits(customerIDStr)? Long.parseLong(customerIDStr): null;
				if(Objects.nonNull(customerID))
				{
					request.setAttribute("CustomerID", customerID);
					chain.doFilter(request, response);
					return;
				}
			}

			if(Objects.nonNull(customerID) && Objects.nonNull(sessionID))
			{
				ResultSet userRS = DBConnector.get(UserAPI.CXID_USER_QUERY, customerID);
				if(userRS.next())
				{
					if(Objects.equals(userRS.getLong("CustomerID"), customerID) && Objects.equals(userRS.getLong("ActiveSessionID"), Long.parseLong(sessionID, 36)))
					{
						if(UserAPI.isSessionActive(userRS.getLong("SessionLastAccessedTime")))
						{
							if(isBankStaffAPI() && !userRS.getBoolean("isBankStaff"))
							{
								ResponseHandler.respond((HttpServletResponse) response, 403);
								return;
							}

							DBConnector.execute(UPDATE_ACCESS_TIME_QUERY, System.currentTimeMillis(), customerID);
							request.setAttribute("CustomerID", customerID);
							chain.doFilter(request, response);
							return;
						}
					}
				}
			}
			ResponseHandler.respond((HttpServletResponse) response, 401);
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

	private boolean isBankStaffAPI()
	{
		return Boolean.parseBoolean(config.getInitParameter("isBankStaffAPI"));
	}
}
