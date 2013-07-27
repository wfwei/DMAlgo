package mine.dt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * ID3决策树
 * 
 * <pre>
 * 1. 只能处理‘语义（无序）数据’，因此如果数据是连续实数，可以装填到整数格子中，当作无序语义数据处理 
 * 2. 实际中，很少只用二分树，所以使用‘信息增益比不纯度’ 
 * 3. 生长算法持续进行，直到所有叶子点都为纯，或者没有其他待分支的变量
 * </pre>
 * 
 * @TODO 考虑属性丢失问题，剪枝
 * 
 * @ref http://www.cnblogs.com/zhangchaoyang/articles/2196631.html
 * 
 * @author WangFengwei
 * 
 */
public class ID3 {

    private static final Pattern attrPatt = Pattern
	    .compile("@attribute(?<name>[^{]+)\\{(?<values>[^}]+)\\}");
    private String dataFile, resultFile;
    private DecisionTree tree;
    private String decAttrName, rootNode = "ID3DecisionTree";

    public ID3(String dataFile, String decAttrName, String resultFile) {
	this.decAttrName = decAttrName;
	this.dataFile = dataFile;
	this.resultFile = resultFile;
    }

    public static void main(String[] args) {
	ID3 id3dt = new ID3("data/weather.nominal.arff", "play",
		"data/result.xml");
	id3dt.buildTree();
    }

    public void buildTree() {

	initTree();
	buildTree(tree);
	writeXML();

    }

    private void initTree() {
	tree = new DecisionTree();
	Attribute decideAttr = null;
	// 读取arff文件，给attribute、attributevalue、data赋值
	try {
	    BufferedReader br = new BufferedReader(new FileReader(dataFile));
	    String line;
	    int idx = 0;
	    while ((line = br.readLine()) != null) {
		Matcher matcher = attrPatt.matcher(line);
		if (matcher.find()) {
		    String attrName = matcher.group("name").trim();
		    String values[] = matcher.group("values").split(",");
		    for (int i = 0; i < values.length; i++) {
			values[i] = values[i].trim();
		    }
		    Attribute attr = new Attribute(attrName, values, idx, true);
		    DecisionTree.addAttr(attr);
		    if (decAttrName.equals(attrName))
			decideAttr = attr;
		    idx++;
		} else if (line.startsWith("@data")) {
		    while ((line = br.readLine()) != null) {
			String[] row = line.split(",");
			if (row.length == DecisionTree.getAllAttrs().size())
			    tree.addData(row);
		    }
		}
	    }
	    br.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}

	if (decideAttr == null) {
	    System.err.println("Decide Attr Not Found:" + decAttrName);
	    System.exit(2);
	}
	tree.setDecideAttr(decideAttr);
	tree.addAvailableAttrs(DecisionTree.getAllAttrs());
	tree.getAvailableAttrs().remove(decideAttr);
    }

    private void buildTree(DecisionTree tree) {

	// 如果不可再分（当前数据全都属于一类，或者当前属性没有区分能力），则结束
	if (tree.getAvailableAttrs().isEmpty() || tree.isPure()) {
	    tree.setAsLeaf();
	    return;
	} else {
	    tree.split();
	    for (DecisionTree subTree : tree.getChildren().values()) {
		buildTree(subTree);
	    }
	}

    }

    private void cloneTree(DecisionTree from, Element to) {
	if (from != null) {
	    HashMap<String, DecisionTree> children = from.getChildren();
	    for (String attrVal : children.keySet()) {
		Element ele = DocumentHelper.createElement(from.getSplitAttr()
			.getAttrName());
		ele.addAttribute("value", attrVal);
		to.add(ele);
		DecisionTree child = children.get(attrVal);
		if (child.isLeaf()) {
		    ele.addText(child.getResultValue());
		} else {
		    cloneTree(child, ele);
		}
	    }
	}
    }

    // 把xml写入文件
    public void writeXML() {

	Document xmldoc = DocumentHelper.createDocument();
	Element root = xmldoc.addElement(rootNode);
	cloneTree(tree, root);

	try {
	    File file = new File(resultFile);
	    if (!file.exists())
		file.createNewFile();
	    FileWriter fw = new FileWriter(file);
	    OutputFormat format = OutputFormat.createPrettyPrint(); // 美化格式
	    XMLWriter output = new XMLWriter(fw, format);
	    output.write(xmldoc);
	    output.close();
	} catch (IOException e) {
	    System.out.println(e.getMessage());
	}

    }
}
