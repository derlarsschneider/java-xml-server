package lars.simplehttpserver.main;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DatabaseConnection {
	private static final String DATABASE_PROPERTIES_FILE = "database.properties";
	private Connection connection;

	public DatabaseConnection() throws IOException {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Properties props = new Properties();
		try (InputStream resourceStream = loader
				.getResourceAsStream(DATABASE_PROPERTIES_FILE)) {
			props.load(resourceStream);
		}
		if (props.contains("dbDriver") && props.contains("dbUrl") && 
				props.contains("dbUser") && props.contains("dbPass")) {
			init(props.getProperty("dbDriver"), props.getProperty("dbUrl"),
					props.getProperty("dbUser"), props.getProperty("dbPass"));
		} else {
			init("org.hsqldb.jdbcDriver", "jdbc:hsqldb:hsql://localhost:9001",
					"sa", "");
		}

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
