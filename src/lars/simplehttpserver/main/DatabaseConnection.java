package lars.simplehttpserver.main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnection {
	private Connection connection;

	public DatabaseConnection() {
		init("org.hsqldb.jdbcDriver", "jdbc:hsqldb:hsql://localhost:9001",
				"sa", "");
	}

	public DatabaseConnection(String driver, String url, String user,
			String pass) {
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

	public void close() throws SQLException {
		connection.close();
	}

	public List<String> select(String sql) throws SQLException {

		Statement statement = connection.createStatement();
		try {
			statement.execute(sql);
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println(sql);
			return null;
		}
		ResultSet resultSet = statement.getResultSet();
		int columnCount = resultSet.getMetaData().getColumnCount();
		List<String> rowList = new ArrayList<String>();
		while (resultSet.next()) {
			String rowString = "<tr>";
			for (int i = 1; i <= columnCount; i++) {
				if (resultSet.getObject(i) == null) {
					rowString += "<td></td>";
				} else {
					rowString += "<td>" + resultSet.getObject(i) + "</td>";
				}
			}
			rowString += "</tr>";
			rowList.add(rowString);
		}
		return rowList;
	}
}
