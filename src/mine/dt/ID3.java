package mine.dt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

class TreeNode {
    private boolean isLeaf;
    private String splitAttr; // 当前节点的分裂属性名称，如果分裂
    private String resultValue;
    private TreeNode parent;
    private HashMap<String, TreeNode> children = new HashMap<String, TreeNode>();
//    private List<String[]> data = new LinkedList<String[]>();

    public TreeNode() {
	this.parent = null;
	this.isLeaf = false;
    }

    public void addChild(String attrVal, TreeNode child) {
	this.children.put(attrVal, child);
    }

    public HashMap<String, TreeNode> getChildren() {
	return children;
    }

    public boolean isLeaf() {
	return isLeaf;
    }

    public void setLeaf(boolean isLeaf) {
	this.isLeaf = isLeaf;
    }

    public TreeNode getParent() {
	return parent;
    }

    public void setParent(TreeNode parent) {
	this.parent = parent;
    }

    public String getResultValue() {
	return resultValue;
    }

    public void setResultValue(String resultValue) {
	this.resultValue = resultValue;
    }

    public String getSplitAttr() {
	return splitAttr;
    }

    public void setSplitAttr(String splitAttr) {
	this.splitAttr = splitAttr;
    }

}

/**
 * ID3决策树
 * 
 * <pre>
 * 1. 只能处理‘语义（无序）数据’，因此如果数据是连续实数，可以装填到整数格子中，当作无序语义数据处理 
 * 2. 实际中，很少只用二分树，所以常用‘增益比不纯度’ 
 * 3. 生长算法持续进行，直到所有叶子点都为纯，或者没有其他待分支的变量
 * 4. 没有考虑确实属性问题
 * </pre>
 * 
 * @TODO 实现树结构，考虑属性丢失问题，剪枝
 * 
 * @ref http://www.cnblogs.com/zhangchaoyang/articles/2196631.html
 * 
 * @author WangFengwei
 * 
 */
public class ID3 {

    private static final Pattern attrPatt = Pattern
	    .compile("@attribute(?<name>[^{]+)\\{(?<values>[^}]+)\\}");
    private ArrayList<String> attrs = new ArrayList<String>();
    private ArrayList<String[]> attrVals = new ArrayList<String[]>();
    private ArrayList<String[]> data = new ArrayList<String[]>();
    private String dataFile, decAttrName, resultFile;
    private int decAttrIdx;

    private TreeNode tree;
    private String rootNode = "ID3DecisionTree";

    public ID3(String dataFile, String decAttrName, String resultFile) {
	this.dataFile = dataFile;
	this.decAttrName = decAttrName;
	this.resultFile = resultFile;
    }

    public static void main(String[] args) {
	ID3 id3dt = new ID3("data/weather.nominal.arff", "play",
		"data/result.xml");
	id3dt.buildTree();
    }

    public void buildTree() {
	init();

	ArrayList<Integer> attrIdxList = new ArrayList<Integer>();
	ArrayList<Integer> dataIdxList = new ArrayList<Integer>();
	for (int i = 0; i < attrs.size(); i++) {
	    if (decAttrName.equals(attrs.get(i))) {
		decAttrIdx = i;
	    } else {
		attrIdxList.add(i);
	    }
	}
	for (int i = 0; i < data.size(); i++) {
	    dataIdxList.add(i);
	}

	buildTree(attrIdxList, dataIdxList, tree);
	writeXML();
    }

    private void init() {
	// 读取arff文件，给attribute、attributevalue、data赋值
	try {
	    BufferedReader br = new BufferedReader(new FileReader(dataFile));
	    String line;
	    while ((line = br.readLine()) != null) {
		Matcher matcher = attrPatt.matcher(line);
		if (matcher.find()) {
		    attrs.add(matcher.group("name").trim());
		    String values[] = matcher.group("values").split(",");
		    for (int i = 0; i < values.length; i++) {
			values[i] = values[i].trim();
		    }
		    attrVals.add(values);
		} else if (line.startsWith("@data")) {
		    while ((line = br.readLine()) != null) {
			String[] row = line.split(",");
			if (row.length == attrs.size())
			    data.add(row);
		    }
		}
	    }
	    br.close();
	} catch (IOException e1) {
	    e1.printStackTrace();
	}

	tree = new TreeNode();
    }

    private static final double MAX_ENTROPY = 1d;

    private boolean isPure(List<Integer> dataIdxList) {
	HashSet<String> ResultValueSet = new HashSet<String>();
	for (Integer dataIdx : dataIdxList) {
	    ResultValueSet.add(data.get(dataIdx)[decAttrIdx]);
	}
	return ResultValueSet.size() <= 1;
    }

    private void buildTree(List<Integer> attrIdxList,
	    List<Integer> dataIdxList, TreeNode tree) {

	// 如果不可再分（当前数据全都属于一类，或者当前属性没有区分能力），则结束
	if (attrIdxList.size() == 0 || isPure(dataIdxList)) {
	    tree.setLeaf(true);
	    tree.setResultValue(data.get(dataIdxList.get(0))[decAttrIdx]);
	    return;
	}

	// 选取信息增益最大的属性，作为分裂属性
	double minEntropy = MAX_ENTROPY;
	int minIdx = -1;
	for (int i = 0; i < attrIdxList.size(); i++) {
	    double entropy = calcEntropy(attrIdxList.get(i), dataIdxList);
	    if (entropy < minEntropy) {
		minEntropy = entropy;
		minIdx = i;
	    }
	}

	int splitAttrIdx = attrIdxList.remove(minIdx);
	String splitAttr = attrs.get(splitAttrIdx);
	String[] splitAttrVals = attrVals.get(splitAttrIdx);
	// 保存当前结果
	tree.setSplitAttr(splitAttr);
	for (String attrVal : splitAttrVals) {
	    TreeNode child = new TreeNode();
	    child.setParent(tree);
	    tree.addChild(attrVal, child);
	}

	// 按照当前切分，分发数据都不同属性
	HashMap<String, List<Integer>> dataIdxMap = new HashMap<String, List<Integer>>();
	for (Integer dataIdx : dataIdxList) {
	    String curAttrVal = data.get(dataIdx)[splitAttrIdx];
	    List<Integer> curDataIdxList = dataIdxMap.get(curAttrVal);
	    if (curDataIdxList == null) {
		curDataIdxList = new ArrayList<Integer>();
		dataIdxMap.put(curAttrVal, curDataIdxList);
	    }
	    curDataIdxList.add(dataIdx);
	}
	dataIdxList.clear();

	HashMap<String, TreeNode> children = tree.getChildren();
	for (String attrVal : children.keySet()) {
	    buildTree(attrIdxList, dataIdxMap.get(attrVal),
		    children.get(attrVal));
	}

    }

    private double calcEntropy(int splitAttrIdx, List<Integer> dataIdxList) {
	HashMap<String, HashMap<String, Integer>> attrCountMap = new HashMap<String, HashMap<String, Integer>>();
	double res = 0;
	for (Integer dataIdx : dataIdxList) {
	    String[] dataEntry = data.get(dataIdx);

	    String splitAttrVal = dataEntry[splitAttrIdx];
	    String decAttrVal = dataEntry[decAttrIdx];

	    // 跳过缺失值
	    if (splitAttrVal.length() == 0 || decAttrVal.length() == 0) {
		continue;
	    }

	    HashMap<String, Integer> countMap = attrCountMap.get(splitAttrVal);
	    if (countMap == null) {
		countMap = new HashMap<String, Integer>();
		attrCountMap.put(splitAttrVal, countMap);
	    }

	    Integer count = countMap.get(decAttrVal);
	    if (count == null)
		count = 1;
	    else
		count += 1;
	    countMap.put(decAttrVal, count);
	}

	res = 0;
	for (HashMap<String, Integer> countMap : attrCountMap.values()) {
	    Integer tot = 0;
	    for (Integer count : countMap.values()) {
		tot += count;
	    }
	    double p = tot.doubleValue() / dataIdxList.size();
	    double entropy = 0d;
	    for (Integer count : countMap.values()) {
		double perc = count.doubleValue() / tot;
		entropy -= perc * Math.log(perc);
	    }
	    res += p * entropy;
	}
	return res;
    }

    private void cloneTree(TreeNode from, Element to) {
	if (from != null) {
	    HashMap<String, TreeNode> children = from.getChildren();
	    for (String attrVal : children.keySet()) {
		Element ele = DocumentHelper.createElement(from.getSplitAttr());
		ele.addAttribute("value", attrVal);
		to.add(ele);
		TreeNode child = children.get(attrVal);
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
