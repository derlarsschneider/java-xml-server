package lars.simplehttpserver.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class DirectoryLister {
	private static final String context = "list";
	private static File currentDir;

	public static void main(String[] args) throws Exception {
		currentDir = new File(".");
		HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
		server.createContext("/" + context, new MyHandler());
		server.setExecutor(null); // creates a default executor
		server.start();
	}

	static class MyHandler implements HttpHandler {

		private Connection connection;
		private static PrintStream log = System.out;

		public void handle(HttpExchange t) throws IOException {
			URI uri = t.getRequestURI();
			HashMap<String, String> parameter = parseQuery(uri);

			try {
				if (parameter.get("db_driver") != null) {
					Class.forName(parameter.get("db_driver"));
				}
			} catch (ClassNotFoundException e) {
				log.println(e.getMessage());
				for (StackTraceElement element : e.getStackTrace()) {
					log.println(element.toString());
				}
			}

			try {
				if (parameter.get("db_url") != null
						&& parameter.get("db_user") != null) {
					String url = parameter.get("db_url") != null ? parameter
							.get("db_url") : "";
					String user = parameter.get("db_user") != null ? parameter
							.get("db_user") : "";
					String pass = parameter.get("db_pass") != null ? parameter
							.get("db_pass") : "";
					connection = DriverManager.getConnection(url, user, pass);
				}
			} catch (SQLException e) {
				log.println(e.getMessage());
				for (StackTraceElement element : e.getStackTrace()) {
					log.println(element.toString());
				}
			}

			String dir = uri.getPath().replaceAll(t.getHttpContext().getPath(),
					".");
			currentDir = new File(dir);

			String response;
			if (currentDir.isDirectory()) {
				response = listFiles(currentDir);
			} else {
				response = readFile(currentDir, (int) currentDir.length());
				if (parameter.get("xsl") != null) {
					int splitPos = response.indexOf('>') + 1;
					StringBuffer buffer = new StringBuffer();
					buffer.append(response.substring(0, splitPos));
					buffer.append("<?xml-stylesheet type=\"text/xsl\" href=\""
							+ parameter.get("xsl") + "\" ?>");
					buffer.append(response.substring(splitPos));
					response = buffer.toString();
				}
			}
			t.sendResponseHeaders(200, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}

		private HashMap<String, String> parseQuery(URI uri) {
			HashMap<String, String> result = new HashMap<String, String>();
			String query = uri.getQuery();
			if (query != null) {
				String[] keyValues = query.split("&");
				for (int i = 0; i < keyValues.length; i++) {
					String[] keyValue = keyValues[i].split("=");
					result.put(keyValue[0], keyValue[1]);
				}
			}
			return result;
		}

		private String readFile(File file, int size) {
			if (file.isDirectory()) {
				return "";
			}
			FileReader fr = null;
			try {
				log.println(file.getName());
				fr = new FileReader(file);
				char[] cbuf = new char[size];
				fr.read(cbuf);
				return replaceSqlWithResult(new String(cbuf));
			} catch (IOException e) {
				log.println(e.getMessage());
				for (StackTraceElement element : e.getStackTrace()) {
					log.println(element.toString());
				}
				return e.getMessage();
			} finally {
				close(fr);
			}
		}

		private String readFile(File file) {
			if (file.isDirectory()) {
				return "";
			}
			FileReader fr = null;
			BufferedReader br = null;
			try {
				StringBuffer result = new StringBuffer();
				fr = new FileReader(file);
				br = new BufferedReader(fr);
				while (br.ready()) {
					result.append(br.readLine() + "\n");
				}
				return replaceSqlWithResult(result.toString());
			} catch (IOException e) {
				log.println(e.getMessage());
				for (StackTraceElement element : e.getStackTrace()) {
					log.println(element.toString());
				}
				return e.getMessage();
			} finally {
				close(br);
				close(fr);
			}
		}

		private void close(Reader reader) {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					log.println(e.getMessage());
					for (StackTraceElement element : e.getStackTrace()) {
						log.println(element.toString());
					}
				}
			}
		}

		private String replaceSqlWithResult(String string) {
			Pattern pattern = Pattern
					.compile("<sql-insert>(.*?)<\\/sql-insert>");
			Matcher matcher = pattern.matcher(string);

			while (matcher.find()) {
				String sql = matcher.group(1);
				try {
					connection.createStatement().execute(sql);
				} catch (SQLException e) {
					e.printStackTrace();
				}

			}
			String regex = "<sql-select>(.*?)<\\/sql-select>";
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(string);

			while (matcher.find()) {
				String sql = matcher.group(1);
				String replacement = select(sql);
				string = string.replaceFirst(regex, replacement);

			}
			return string;
		}

		public String select(String sql) {
			try {
				Statement statement = connection.createStatement();
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
				for (StackTraceElement element : e.getStackTrace()) {
					log.println(element.toString());
				}
				return e.getMessage();
			}
		}

		private String listFiles(File dir) {
			if (!dir.isDirectory()) {
				return "";
			}
			File[] list = dir.listFiles();
			String response = "";
			response += "<p>" + currentDir + "</p>";
			if (currentDir.getParent() != null) {
				response += "<li><a href=\"/" + context + "/"
						+ currentDir.getParent() + "/" + "\">..</a></li>";
			}
			for (File file : list) {
				response += "<li><a href=\"/" + context + "/"
						+ currentDir.getPath() + "/" + file.getName() + "\">"
						+ file.getName() + "</a> " + file.length() + " </li>";
			}
			return response;
		}
	}
}
