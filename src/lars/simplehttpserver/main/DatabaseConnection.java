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

	public DatabaseConnection() throws IOException, ClassNotFoundException,
			SQLException {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Properties props = new Properties();
		try (InputStream resourceStream = loader
				.getResourceAsStream(DATABASE_PROPERTIES_FILE)) {
			props.load(resourceStream);
		}
		init(props.getProperty("dbDriver", "org.hsqldb.jdbcDriver"),
				props.getProperty("dbUrl", "jdbc:hsqldb:hsql://localhost:9001"),
				props.getProperty("dbUser", "sa"),
				props.getProperty("dbPass", ""));
	}

	public DatabaseConnection(String driver, String url, String user,
			String pass) throws ClassNotFoundException, SQLException {
		init(driver, url, user, pass);
	}

	private void init(String driver, String url, String user, String pass)
			throws ClassNotFoundException, SQLException {
		Class.forName(driver);

		connection = DriverManager.getConnection(url, user, pass);
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
