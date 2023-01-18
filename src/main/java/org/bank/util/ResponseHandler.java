package org.bank.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ResponseHandler
{

	public static void respond(HttpServletResponse response, Integer httpCode, Integer code, String message) throws IOException
	{
		respond(response, httpCode, code, message, null);
	}

	public static void respond(HttpServletResponse response, Integer httpCode, Integer code, String message, Object data) throws IOException
	{
		respond(response, httpCode);
		Map<String, Object> responseMap = new HashMap<String, Object>();
		responseMap.put("code", code);
		responseMap.put("message", message);
		if(Objects.nonNull(data))
		{
			responseMap.put("data", data);
		}
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
//		response.addHeader("Access-Control-Allow-Origin", "*");
		PrintWriter writer = response.getWriter();
		GsonBuilder gb = new GsonBuilder();
		gb.serializeNulls();
		writer.write(gb.create().toJson(responseMap));
		writer.flush();
	}

	public static void respond(HttpServletResponse response, Integer httpCode)
	{
		response.setStatus(httpCode);
	}
}
