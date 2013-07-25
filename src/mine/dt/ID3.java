package mine.dt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
	private Document xmldoc;
	private Element root;
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

		buildTree(attrIdxList, dataIdxList, root);
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

		xmldoc = DocumentHelper.createDocument();
		root = xmldoc.addElement(rootNode);
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
			List<Integer> dataIdxList, Element root) {

		// 如果不可再分（当前数据全都属于一类，或者当前属性没有区分能力），则结束
		if (attrIdxList.size() == 0 || isPure(dataIdxList)) {
			root.addText(data.get(dataIdxList.get(0))[decAttrIdx]);
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
		for (String attrVal : splitAttrVals) {
			root.addElement(splitAttr).addAttribute("value", attrVal);
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
		@SuppressWarnings("rawtypes")
		Iterator iter = root.elementIterator();
		while (iter.hasNext()) {
			Element curRoot = (Element) iter.next();
			String curAttrVal = curRoot.attributeValue("value");
			buildTree(attrIdxList, dataIdxMap.get(curAttrVal), curRoot);
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

	// 把xml写入文件
	public void writeXML() {
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
