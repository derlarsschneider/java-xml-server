package lars.simplehttpserver.main;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
	private static PrintStream log = System.out;
	private Connection connection;

	public DatabaseConnection() {
		init("org.hsqldb.jdbcDriver", "jdbc:hsqldb:hsql://localhost:9001", "sa", "");
	}

	public DatabaseConnection(String driver, String url, String user, String pass) {
		init(driver, url, user, pass);
	}

	private void init(String driver, String url, String user, String pass) {
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		try {
			connection = DriverManager.getConnection(url, user, pass);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public String select(String sql) throws SQLException {

		Statement statement = connection.createStatement();
		try {
			statement.execute(sql);
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println(sql);
			return "";
		}
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
		return result;
	}
}
