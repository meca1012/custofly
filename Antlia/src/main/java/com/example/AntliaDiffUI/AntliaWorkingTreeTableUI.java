package com.example.AntliaDiffUI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.annotation.WebServlet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.example.window.MySubWindow;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.FilesystemContainer;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.data.util.TextFileProperty;
import com.vaadin.event.Action;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;

@SuppressWarnings("serial")
@Theme("antlia")
public class AntliaWorkingTreeTableUI extends UI implements Action.Handler {

	@WebServlet(value = "/*", asyncSupported = true)
	@VaadinServletConfiguration(productionMode = false, ui = AntliaWorkingTreeTableUI.class)
	public static class Servlet extends VaadinServlet {
	}

	// public static final String SPECIFIC = "mark_as_specific";
	public static final String PROPERTYID_NAME = "Name";
	public static final String PROPERTYID_VALUE = "Value";
	public static final String CONFIG_PROPERTY_ROOTELEMENT = "rootElements";
	public static final String CONFIG_PROPERTY_ELEMENT = "elements";
	public static final String CONFIG_PROPERTY_ID = "ids";

	public final String filePath = "C:\\DEV\\wildfly\\bin\\tmp\\Channels.xml";
	public final String configurationProperties = "C:\\DEV\\wildfly\\bin\\tmp\\configuration.properties";
	public final static String specificFilePath = "C:\\DEV\\wildfly\\bin\\tmp\\specific.xml";

	final HierarchicalContainer container = new HierarchicalContainer();
	TreeTable treeTable = new TreeTable("MyTreeTable", container);
	// Label docView = new Label("DocViewer", ContentMode.HTML);
	DocEditor docView = new DocEditor();

	File fXmlFile = new File(filePath);
	FilesystemContainer docContainer = new FilesystemContainer(fXmlFile);
	Document doc;

	private String xpath = null;

	String rootElements[];
	String[] elements;
	String[] ids;

	boolean editable = false;

	@Override
	protected void init(VaadinRequest request) {

		HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
		setContent(splitPanel);

		splitPanel.addComponent(treeTable);
		splitPanel.addComponent(docView);

		treeTable.setSizeFull();
		treeTable.setSelectable(true);
		treeTable.setImmediate(true);

		this.addActionHandler(treeTable);
		treeTable.addContainerProperty(PROPERTYID_NAME, String.class, "");
		treeTable.addContainerProperty(PROPERTYID_VALUE, String.class, "");

		docView.setPropertyDataSource(new TextFileProperty(fXmlFile));
		docView.setSizeFull();

		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			// dbFactory.setIgnoringElementContentWhitespace(true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(fXmlFile);
			Element root = doc.getDocumentElement();
			Object rootItem = treeTable.addItem(new Object[] { root.getNodeName(), "" }, null);
			addChildrenToTreeTable(treeTable, root.getChildNodes(), rootItem);
			readProperties();
		} catch (Exception e) {
			System.out.println(e);
		}

	}

	private void addChildrenToTreeTable(TreeTable treeTable, NodeList children, Object parent) {
		// exist children?
		if (children.getLength() > 0) {
			// iterate over children
			for (int i = 0; i < children.getLength(); i++) {
				Node node = children.item(i);
				// is it an element? <element></element>
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					// add row to table, only name, no value, generated id
					Object childItemId = treeTable.addItem(new Object[] { node.getNodeName(), "" }, null);
					treeTable.setChildrenAllowed(childItemId, true);
					// set rootElement as parent of this one
					treeTable.setParent(childItemId, parent);
					// recursive call
					addChildrenToTreeTable(treeTable, node.getChildNodes(), childItemId);
					// is it a node <node>value</node>
				} else if (node.getNodeType() == Node.COMMENT_NODE) {
				} else if (node.getNodeType() == Node.TEXT_NODE) {
					// TODO: \r\n und whitespaces vor Bearbeitung aus dem XML
					// entfernen entweder mit XSLT oder einer library
					if (node.getNodeName() == ("#text") && node.getTextContent().contains("\n")) {
						System.out.println("skip note mit Content: -" + node.getTextContent() + "-");
					} else {
						// if it's a node, add the value to the treetable
						treeTable.getContainerProperty(parent, "Value").setValue(node.getTextContent());
						treeTable.setChildrenAllowed(parent, false);
						// treeTable.setCollapsed(parent, false);
					}
				}
			}
		} else {
			System.out.println("else");
		}
	}

	private void addActionHandler(final TreeTable tt) {
		tt.addActionHandler(this);
	}

	// Have the unmodified Enter key cause an event
	private static final Action ADD = new Action("Add item");
	private static final Action DELETE = new Action("Delete item");
	private static final Action EDIT = new Action("Edit item");
	private static final Action DECLARE_AS_SPECIFIC = new Action("Item is environmentspecific");

	Action[] actions = new Action[] { EDIT, DECLARE_AS_SPECIFIC, ADD, DELETE };

	public Action[] getActions(Object target, Object sender) {

		if (target instanceof Integer) {
			Integer id = (Integer) target;
			String element = this.getItemProperty(id, PROPERTYID_NAME);
			for (String s : this.elements) {
				if (s.equalsIgnoreCase(element)) {
					return new Action[] { DECLARE_AS_SPECIFIC };
				}
			}
			// when parent = element, then it's editable
			Object parentId = this.treeTable.getParent(id);
			element = this.getItemProperty(parentId, PROPERTYID_NAME);
			for (String s : this.elements) {
				if (s.equalsIgnoreCase(element)) {
					return new Action[] { EDIT };
				}
			}
			return null;
		} else {
			return new Action[] { ADD, EDIT };
		}
	}

	public void handleAction(Action action, Object sender, Object target) {

		if (action == DELETE) {
			System.out.println("not yet implemented");
			// treeTable.removeItem(target);
		} else if (action == ADD) {
			System.out.println("not yet implemented");
			// final Object id = addCaptionedItem("New Item", target);
			// treeTable.expandItem(target);
			// treeTable.setValue(id);
			// editor.focus();
		} else if (action == EDIT) {
			// System.out.println("not yet implemented");
			// if (!editable) {
			// editable = true;
			// treeTable.setEditable(editable);
			// } else {
			// editable = false;
			// treeTable.setEditable(editable);
			// }
			String value = this.getItemProperty(target, PROPERTYID_VALUE);
			MySubWindow sub = new MySubWindow(value);
			// Add it to the root component
			UI.getCurrent().addWindow(sub);
		} else if (action == DECLARE_AS_SPECIFIC) {
			// System.out.println("Sender: " + sender + "& Target: " + target);
			generateXPath(target);
			System.out.println(this.xpath);
			try {
				this.xpathdemo(doc, this.xpath);
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
		}
	}

	private void generateXPath(Object target) {
		// target = id des ausgewählten value
		Item chosenItem = this.treeTable.getItem(target);

		// get the value of the item for xpath identification
		String name = (String) chosenItem.getItemProperty(PROPERTYID_NAME).getValue();
		String value = (String) chosenItem.getItemProperty(PROPERTYID_VALUE).getValue();

		// get the id of the chosen element
		Collection<?> coll = this.container.getChildren(target);
		// hashmap for elements with double idelement
		HashMap<String, String> hm = new HashMap<String, String>();

		for (Object o : coll) {
			if (o instanceof Integer) {
				Item i = this.container.getItem(o);
				// System.out.println(i.getItemProperty(PROPERTYID_NAME).getValue()
				// + " "
				// + i.getItemProperty(PROPERTYID_VALUE).getValue());
				String id = (String) i.getItemProperty(PROPERTYID_NAME).getValue();
				String content = (String) i.getItemProperty(PROPERTYID_VALUE).getValue();

				for (String s : this.ids) {
					// add ids to hashmap
					if (s.equalsIgnoreCase(id)) {
						hm.put(id, content);
					}
				}
			}
		}
		String path = "/" + name;
		// =+ "/text()" + " + "\"" + value + "\"
		generatePath(target, path);

		String toadd = null;
		// map must have
		if (hm.size() >= 1) {
			int i = 0;
			for (String s : hm.keySet()) {
				if (i == 0) {
					// only the first element
					toadd = String.format("[%s/text()='%s' ", s, hm.get(s));
				} else {
					// and for further elements
					toadd = toadd + String.format("and %s/text()='%s' ", s, hm.get(s));
				}
				i++;
			}
		}
		// close with ]
		this.xpath = this.xpath + toadd + "]";
		System.out.println("Hashmap: " + hm.size());
		// System.out.println(name + " " + value);
	}

	private void generatePath(Object target, String path) {

		if (hasParents(target)) {
			// get parentID
			Object parentItemId = treeTable.getParent(target);
			// get the item because we need the name property
			String parent = this.getItemProperty(parentItemId, PROPERTYID_NAME);
			path = "/" + parent + path;
			// recursive call until no parentId exists anymore
			this.generatePath(parentItemId, path);
		} else {
			this.xpath = path;
		}
	}

	private boolean hasParents(Object itemId) {
		if (treeTable.getParent(itemId) != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Gets the item for an itemId and returns the matching value
	 * 
	 * @param itemId
	 * @param id
	 * @return
	 */
	private String getItemProperty(Object itemId, String propertyId) {
		String result = null;
		try {
			Item item = treeTable.getItem(itemId);
			result = (String) item.getItemProperty(propertyId).getValue();
		} catch (Exception e) {
			System.out.println("No result for itemId: " + itemId + " propertyId: " + propertyId);
		}
		return result;
	}

	private void xpathdemo(Document document, String path) throws XPathExpressionException {
		XPath xpath = XPathFactory.newInstance().newXPath();
		// XPath Query for showing all nodes value
		XPathExpression expr = xpath.compile(path);
		// /text()
		NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			System.out.println(node.getNodeName() + " " + node.getTextContent());
			System.out.println("ParentNode: "+node.getParentNode().getNodeName());
			try {
				writeSpecificCustomizing(node);
			} catch (TransformerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void writeSpecificCustomizing(Node node) throws TransformerException, ParserConfigurationException,
			SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document specificDocument = dBuilder.parse("C:\\DEV\\wildfly\\bin\\tmp\\aaa.xml");
		Element rootelement = specificDocument.getDocumentElement();
		if (rootelement.getNodeName().equalsIgnoreCase(node.getParentNode().getNodeName())) {
			// TODO: check if node exists, if true replace and remove in basedoc
			Node newNode = specificDocument.importNode(node, true);
			specificDocument.getDocumentElement().appendChild(newNode);
		} else {
			specificDocument.createElement(node.getParentNode().getNodeName());
			Node newNode = specificDocument.importNode(node, true);
			specificDocument.getDocumentElement().appendChild(newNode);
		}
		// XML Write
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(specificDocument);
		StreamResult streamResult = new StreamResult(new File("C:\\DEV\\wildfly\\bin\\tmp\\aaa.xml"));
		transformer.transform(source, streamResult);
	}

	private void readProperties() throws FileNotFoundException, IOException {
		// load propertyfile
		Properties prop = new Properties();
		prop.load(new FileInputStream(this.configurationProperties));
		// get the possible rootElements, elements and their ids
		this.rootElements = StringUtils.split(prop.getProperty(CONFIG_PROPERTY_ROOTELEMENT), ",");
		this.elements = StringUtils.split(prop.getProperty(CONFIG_PROPERTY_ELEMENT), ",");
		this.ids = StringUtils.split(prop.getProperty(CONFIG_PROPERTY_ID), ",");
	}

	public String getXpath() {
		return xpath;
	}

	public void setXpath(String xpath) {
		this.xpath = xpath;
	}

}
