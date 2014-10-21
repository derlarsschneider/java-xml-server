package lars.simplehttpserver.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.StringReader;
import java.net.URI;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;

import lars.simplehttpserver.helper.URIUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class XSLTProcessor implements HttpHandler {
	@Override
	public void handle(HttpExchange t) throws IOException {

		URI uri = t.getRequestURI();
		HashMap<String, String> parameter = URIUtils.parseQuery(uri);

		String response = "";
		try {
			XSLTProcessor p = new XSLTProcessor();
			if (parameter.get("xml") == null) {
				throw new Exception("URL parameter xml missing.");
			}
			if (parameter.get("xsl") == null) {
				throw new Exception("URL parameter xsl missing.");
			}
			p.setXmlFile(new File(parameter.get("xml")));
			p.setXslFile(new File(parameter.get("xsl")));
			p.applyXSL();
			p.replaceSqlWithResult();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			Source source = new DOMSource(p.root);
			Result target = new StreamResult(out);
			transformer.transform(source, target);
			response = out.toString();
			p.printRoot();
		} catch (Exception e) {
			e.printStackTrace();
			response = "<pre>" + e.getMessage() + "\r\n\t"
					+ Arrays.toString(e.getStackTrace()).replace(", ", "\r\n\t").replace("[", "").replace("]", "")
					+ "</pre>";

		} finally {
			t.sendResponseHeaders(200, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
	}

	public static void main(String[] args) throws Exception {
		XSLTProcessor p = new XSLTProcessor();
		p.setXmlFile(new File("test/cd-catalogue.xml"));
		p.setXslFile(new File("test/artists.xsl"));
		// p.find("/CATALOG/CD/TITLE");
		// System.out.println(p.readXML());
		p.applyXSL();
		p.replaceSqlWithResult();
		p.printRoot();
	}

	private Element root;
	private Transformer transformer;
	private File xmlFile;

	private File xslFile;
	private DocumentBuilder documentBuilder;

	public XSLTProcessor() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		documentBuilder = factory.newDocumentBuilder();

	}

	@SuppressWarnings("unused")
	private void writeStream(InputStream inputStream) throws IOException {
		int i = 0;
		while ((i = inputStream.read()) != -1) {
			System.out.print((char) i);
		}
	}

	private void parseStream(InputStream inputStream) throws SAXException, IOException, ParserConfigurationException {
		Document doc = documentBuilder.parse(inputStream);
		// optional, but recommended
		// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		doc.getDocumentElement().normalize();
		root = doc.getDocumentElement();

	}

	// @SuppressWarnings("unused")
	// private String find(String xpathExpression) throws
	// XPathExpressionException, SAXException, IOException,
	// ParserConfigurationException {
	// XPathFactory xPathfactory = XPathFactory.newInstance();
	// XPath xpath = xPathfactory.newXPath();
	// XPathExpression expr = xpath.compile(xpathExpression);
	// String result = expr.evaluate(root);
	// return result;
	// }
	//
	// private NodeList findAll(String xpathExpression) throws
	// XPathExpressionException, SAXException, IOException,
	// ParserConfigurationException {
	// XPathFactory xPathfactory = XPathFactory.newInstance();
	// XPath xpath = xPathfactory.newXPath();
	// XPathExpression expr = xpath.compile(xpathExpression);
	// NodeList result = (NodeList) expr.evaluate(root, XPathConstants.NODESET);
	// return result;
	// }

	private void printTree(Node node, int depth) {
		for (int i = 0; i < depth; i++) {
			System.out.print(" ");
		}

		if (node.hasChildNodes()) {
			System.out.println(node.getNodeName());
			NodeList childNodes = node.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node child = childNodes.item(i);
				printTree(child, depth + 1);
			}
		} else {
			System.out.println(node.getNodeValue());
		}
	}

	private void printTree(Node node) {
		printTree(node, 0);
	}

	private void printRoot() {
		printTree(root, 0);
	}

	public void applyXSL() throws TransformerException, IOException, SAXException, ParserConfigurationException {

		TransformerFactory factory = TransformerFactory.newInstance();
		Source xslt = new StreamSource(xslFile);
		transformer = factory.newTransformer(xslt);
		final Source xml = new StreamSource(xmlFile);

		final PipedOutputStream output = new PipedOutputStream();
		PipedInputStream input = new PipedInputStream(output);
		new Thread(new Runnable() {
			public void run() {
				try {
					transformer.transform(xml, new StreamResult(output));
				} catch (TransformerException e) {
					e.printStackTrace();
				} finally {
					try {
						output.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();

		parseStream(input);
	}

	private void replaceSqlWithResult() throws XPathExpressionException, SAXException, IOException,
			ParserConfigurationException, SQLException {
		DatabaseConnection connection = new DatabaseConnection();
		// NodeList nodeList = findAll("//sql-select");
		NodeList nodeList = root.getOwnerDocument().getElementsByTagName("sql-select");
		while (nodeList.getLength() > 0) {

			Node sqlQueryNode = nodeList.item(0);
			String sqlQuery = sqlQueryNode.getTextContent();
			for (int i = 10; i > 1; i--) {
				String regex = "encode\\(([^']*)";
				String replacement = "$1";
				for (int j = 2; j < i; j++) {
					regex += "'(.*)";
					replacement += "''$" + j;
				}
				regex += "\\)";
				sqlQuery = sqlQuery.replaceAll(regex, replacement);
			}
			sqlQuery = sqlQuery.replaceAll("encode\\((.*)'(.*)'(.*)\\)", "$1''$2''$3");
			sqlQuery = sqlQuery.replaceAll("encode\\((.*)'(.*)\\)", "$1''$2");
			sqlQuery = sqlQuery.replaceAll("encode\\((.*)\\)", "$1");
			String sqlResult = connection.select(sqlQuery);
			// Node node = new Node();
			if ("".equals(sqlResult)) {
				sqlResult = "<tr style=\"background:#ee1111\"><td colspan=\"999\">" + sqlQuery + "</td></tr>";
			}
			// Node sqlResult =
			// p.documentBuilder.newDocument().createTextNode(result);
			Document doc = sqlQueryNode.getOwnerDocument();
			Node sqlResultNode = documentBuilder.parse(new InputSource(new StringReader(sqlResult)))
					.getDocumentElement();
			sqlResultNode = doc.importNode(sqlResultNode, true);

			sqlQueryNode = sqlQueryNode.getParentNode().replaceChild(sqlResultNode, sqlQueryNode);
		}
	}

	public File getXmlFile() {
		return xmlFile;
	}

	public void setXmlFile(File xmlFile) {
		this.xmlFile = xmlFile;
	}

	public File getXslFile() {
		return xslFile;
	}

	public void setXslFile(File xslFile) {
		this.xslFile = xslFile;
	}

}
