package lars.simplehttpserver.main;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
	private static PrintStream log = System.out;
	private Connection conn;

	public DatabaseConnection() {
		init();
	}

	private void init() {
		try {
			Class.forName("org.hsqldb.jdbcDriver");
		} catch (ClassNotFoundException e) {
			log.println(e.getMessage());
			for (StackTraceElement element: e.getStackTrace()) {
				log.println(element.toString());
			}
		}

		try {
			conn = DriverManager.getConnection("jdbc:hsqldb:mem:.", "sa", "");
		} catch (SQLException e) {
			log.println(e.getMessage());
			for (StackTraceElement element: e.getStackTrace()) {
				log.println(element.toString());
			}
		}
	}

	public void insert(String sql) {
		try {
			Statement statement = conn.createStatement();
			statement.execute(sql);
			log.println(sql);
		} catch (SQLException e) {
			log.println(sql);
			log.println(e.getMessage());
			for (StackTraceElement element: e.getStackTrace()) {
				log.println(element.toString());
			}
		}
	}

	public String select(String sql) {
		try {
			Statement statement = conn.createStatement();
			statement.execute(sql);
			ResultSet resultSet = statement.getResultSet();
			String result = "";
			int columnCount = resultSet.getMetaData().getColumnCount();
			while (resultSet.next()) {
				result += "<tr>";
				for (int i = 1; i <= columnCount; i++) {
					result += "<td>" + resultSet.getObject(i) + "</td>";
				}
				result += "</tr>";
			}
			log.println(result);
			log.println(sql);
			return result;
		} catch (SQLException e) {
			log.println(sql);
			log.println(e.getMessage());
			for (StackTraceElement element: e.getStackTrace()) {
				log.println(element.toString());
			}
			return e.getMessage();
		}
	}
}
