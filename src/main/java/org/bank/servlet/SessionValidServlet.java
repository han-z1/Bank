package org.bank.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bank.util.ResponseHandler;

public class SessionValidServlet extends HttpServlet
{
	@Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		ResponseHandler.respond(resp, 200, 0, "success");
	}
}
