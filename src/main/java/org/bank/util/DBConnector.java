package org.bank.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnector
{

	private static final String MYSQL_DOMAIN = "localhost";
	private static final String MYSQL_PORT = "3306";
	private static final String MYSQL_DATABASE = "Bank";
	private static final String MYSQL_USER = "root";
	private static final String MYSQL_PASSWORD = "";

	public static Connection getConnection() throws SQLException
	{
		return DriverManager.getConnection("jdbc:mysql://" + MYSQL_DOMAIN + ":" + MYSQL_PORT + "/" + MYSQL_DATABASE + "?user=" + MYSQL_USER + "&password=" + MYSQL_PASSWORD);
	}

	public static ResultSet get(String query, Object... params) throws SQLException
	{
		return get(getConnection(), query, params);
	}

	public static ResultSet get(Connection connection, String query, Object... params) throws SQLException
	{
		PreparedStatement statement = connection.prepareStatement(query);
		for(int i = 0; i < params.length; i++)
		{
			statement.setObject(i + 1, params[i]);
		}
		return statement.executeQuery();
	}

	public static ResultSet execute(String query, Object... params) throws SQLException
	{
		return execute(getConnection(), query, params);
	}

	public static ResultSet execute(Connection connection, String query, Object... params) throws SQLException
	{
		PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		for(int i = 0; i < params.length; i++)
		{
			statement.setObject(i + 1, params[i]);
		}
		statement.execute();
		return statement.getGeneratedKeys();
	}
}
