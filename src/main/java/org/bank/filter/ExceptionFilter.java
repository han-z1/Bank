package org.bank.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.bank.exception.Exceptions;
import org.bank.exception.ValidationException;
import org.bank.util.ResponseHandler;

public class ExceptionFilter implements Filter
{
	@Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		try
		{
			chain.doFilter(request, response);
		}
		catch(ValidationException ex)
		{
			ResponseHandler.respond((HttpServletResponse) response, 400, ex.getCode(), ex.getMessage());
		}
		catch(Exception ex)
		{
			ResponseHandler.respond((HttpServletResponse) response, 500, 1000, "Issue processing your request.. Please contact your branch!");
		}
	}
}
