package lars.simplehttpserver.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XSLTProcessor {

	public void compareXmlWithDB() {
		NodeList rows = getRoot().getElementsByTagName("tr");
		for (int i = 0; i < rows.getLength(); i++) {
			Element row = (Element) rows.item(i);
			Node rowNameAttribute = row.getAttributes().getNamedItem("name");
			if (rowNameAttribute != null
					&& "xmlContent".equals(rowNameAttribute.getTextContent())) {
				row.setAttribute("class", "xmlContent");
				NodeList xmlElementList = row.getChildNodes();
				NodeList dbElementList = row.getNextSibling().getChildNodes();
				for (int j = 1; j < xmlElementList.getLength(); j++) {

					Element xmlElement = (Element) xmlElementList.item(j);
					Element dbElement = (Element) dbElementList.item(j);
					if (dbElement == null) {
						xmlElement.setAttribute("class", "NOK");
					} else {
						String xmlTextContent = xmlElement.getTextContent()
								.trim();
						String textContent = dbElement.getTextContent();
						System.out.println(textContent);
						String dbTextContent = textContent.trim();
						if (xmlTextContent.equals(dbTextContent)) {
							xmlElement.setAttribute("class", "OK");
							dbElement.setAttribute("class", "OK");
						} else {
							xmlElement.setAttribute("class", "NOK");
							dbElement.setAttribute("class", "NOK");
						}
					}
				}
			}

		}
	}

	public static void main(String[] args) throws Exception {
		XSLTProcessor p = new XSLTProcessor();
		p.setXmlFile(new File("test/cd-catalogue.xml"));
		p.setXslFile(new File("test/artists.xsl"));
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

	public void parseXML() throws SAXException, IOException,
			ParserConfigurationException {
		parseStream(new FileInputStream(xmlFile));
	}

	private void parseStream(InputStream inputStream) throws SAXException,
			IOException, ParserConfigurationException {
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
			System.out.println(node.getNodeName() + " "
					+ node.getAttributes().getNamedItem("class"));
			NodeList childNodes = node.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node child = childNodes.item(i);
				printTree(child, depth + 1);
			}
		} else {
			System.out.println(node.getNodeValue());
		}
	}

	@SuppressWarnings("unused")
	private void printTree(Node node) {
		printTree(node, 0);
	}

	public void printRoot() {
		printTree(getRoot(), 0);
	}

	public void applyXSL() throws TransformerException, IOException,
			SAXException, ParserConfigurationException {

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

	public void replaceSqlWithResult() throws XPathExpressionException,
			SAXException, IOException, ParserConfigurationException,
			SQLException {
		DatabaseConnection connection = new DatabaseConnection();
		try {
			// NodeList nodeList = findAll("//sql-select");
			NodeList nodeList = getRoot().getOwnerDocument().getElementsByTagName(
					"sql-select");
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
				sqlQuery = sqlQuery.replaceAll("LIKE '%'", "is null");
				System.out.println(sqlQuery);
				List<String> sqlResultList = connection.select(sqlQuery);
				if (sqlResultList == null || sqlResultList.size() == 0) {
					insertNodeBefore(sqlQueryNode,
							"<tr style=\"background:#ee1111\"><td colspan=\"999\">"
									+ sqlQuery + "</td></tr>");
				} else {
					for (String sqlResult : sqlResultList) {
						insertNodeBefore(sqlQueryNode, sqlResult);
					}
				}
				sqlQueryNode.getParentNode().removeChild(sqlQueryNode);
			}
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	private void insertNodeBefore(Node sqlQueryNode, String sqlResult)
			throws SAXException, IOException {
		Node sqlResultNode = documentBuilder.parse(
				new InputSource(new StringReader(sqlResult)))
				.getDocumentElement();
		sqlResultNode = sqlQueryNode.getOwnerDocument().importNode(
				sqlResultNode, true);

		sqlQueryNode.getParentNode().insertBefore(sqlResultNode, sqlQueryNode);
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

	public Element getRoot() {
		return root;
	}

}
