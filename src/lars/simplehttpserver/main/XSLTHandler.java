package lars.simplehttpserver.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import lars.simplehttpserver.helper.URIUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class XSLTHandler implements HttpHandler{
	private static final String transformContext = "trans";

	public static void main(String[] args) throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
		server.createContext("/" + transformContext, new XSLTHandler());
		server.setExecutor(null); // creates a default executor
		server.start();
	}
	@Override
	public void handle(HttpExchange t) throws IOException {

		URI uri = t.getRequestURI();
		HashMap<String, String> parameter = URIUtils.parseQuery(uri);

		String response = "";
		try {
			XSLTProcessor p = new XSLTProcessor();
			if (parameter.get("xml") == null) {
				throw new Exception("URL parameter xml missing.");
			} else {
				p.setXmlFile(new File(parameter.get("xml")));
			}
			if (parameter.get("xsl") != null) {
				p.setXslFile(new File(parameter.get("xsl")));
			} else {
				p.parseXML();
				String xsl = p.getRoot().getNodeName().replace(':', '-');
				p.setXslFile(new File(xsl + ".xsl"));
			}
			p.applyXSL();
			p.replaceSqlWithResult();
			p.compareXmlWithDB();
			p.printRoot();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			Source source = new DOMSource(p.getRoot());
			Result target = new StreamResult(out);
			transformer.transform(source, target);
			response = out.toString();
		} catch (Exception e) {
			e.printStackTrace();
			response = "<pre>"
					+ e.getMessage()
					+ "\r\n\t"
					+ Arrays.toString(e.getStackTrace())
							.replace(", ", "\r\n\t").replace("[", "")
							.replace("]", "") + "</pre>";

		} finally {
			t.sendResponseHeaders(200, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
	}

}
