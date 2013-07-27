package mine.dt;

import java.util.HashMap;
import java.util.List;

/**
 * 不纯度计算类，包括熵不纯度和Gini不纯度
 * 
 * @author WangFengwei
 */
public class Impurity {

    /**
     * 计算使用targetColumn分类后在decideColumn上的信息熵增益
     */
    public static double calcInfoGain(List<String[]> data, int targetColumn,
	    int decideColumn) {
	double curImpurity = calcEntropyImpurity(data, decideColumn);
	double newImpurity = calcEntropyImpurity(data, targetColumn,
		decideColumn);
	return curImpurity - newImpurity;
    }

    /**
     * 计算使用targetColumn分类后在decideColumn上的信息熵增益率
     */
    public static double calcInfoGainRatio(List<String[]> data,
	    int targetColumn, int decideColumn) {

	double curImpurity = calcEntropyImpurity(data, decideColumn);
	double newImpurity = calcEntropyImpurity(data, targetColumn,
		decideColumn);

	return curImpurity / (newImpurity + MIN_VALUE);
    }

    /**
     * 通过Gini不纯度计算使用targetColumn分类而产生的不纯度增加
     */
    public static double calcGiniGain(List<String[]> data, int targetColumn,
	    int decideColumn) {
	double curImpurity = calcGiniImpurity(data, decideColumn);
	double newImpurity = calcGiniImpurity(data, targetColumn, decideColumn);
	return curImpurity - newImpurity;
    }

    /**
     * 通过Gini不纯度计算使用targetColumn分类而产生的不纯度增加比例
     */
    public static double calcGiniGainRatio(List<String[]> data,
	    int targetColumn, int decideColumn) {
	double curImpurity = calcGiniImpurity(data, decideColumn);
	double newImpurity = calcGiniImpurity(data, targetColumn, decideColumn);
	return curImpurity / (newImpurity + MIN_VALUE);
    }

    /**
     * 预处理数据。
     * 
     * targetColumn的值作为外层map的key；decideColumn的值作为内层map的key，个数作为value
     */
    private static HashMap<String, HashMap<String, Integer>> prepare(
	    List<String[]> data, int targetColumn, int decideColumn) {

	HashMap<String, HashMap<String, Integer>> attrCountMap = new HashMap<String, HashMap<String, Integer>>();
	for (String[] dataEntry : data) {

	    String splitAttrVal = dataEntry[targetColumn];
	    String decAttrVal = dataEntry[decideColumn];

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
	return attrCountMap;
    }

    /** 根据column列的值计算信息熵 */
    private static double calcEntropyImpurity(List<String[]> data, int column) {
	double impurity = 0d;
	HashMap<String, Integer> attrCount = new HashMap<String, Integer>();
	for (String[] adata : data) {
	    Integer count = attrCount.get(adata[column]);
	    if (count == null)
		count = 1;
	    else
		count += 1;
	    attrCount.put(adata[column], count);
	}

	for (Integer count : attrCount.values()) {
	    double perc = count.doubleValue() / data.size();
	    impurity -= perc * Math.log(perc);
	}

	return impurity;
    }

    /** 计算使用targetColumn分类后在decideColumn上的信息熵 */
    private static double calcEntropyImpurity(List<String[]> data,
	    int targetColumn, int decideColumn) {
	double impurity = 0d;
	HashMap<String, HashMap<String, Integer>> attrCountMap = prepare(data,
		targetColumn, decideColumn);

	for (HashMap<String, Integer> countMap : attrCountMap.values()) {
	    Integer tot = 0;
	    for (Integer count : countMap.values()) {
		tot += count;
	    }
	    double tmp = 0d;
	    for (Integer count : countMap.values()) {
		double perc = count.doubleValue() / tot;
		tmp -= perc * Math.log(perc);
	    }
	    impurity += (tot.doubleValue() / data.size()) * tmp;
	}
	return impurity;
    }

    /** 根据column列的值计算信息熵 */
    private static double calcGiniImpurity(List<String[]> data, int column) {
	double impurity = 0d;
	HashMap<String, Integer> attrCount = new HashMap<String, Integer>();
	for (String[] adata : data) {
	    Integer count = attrCount.get(adata[column]);
	    if (count == null)
		count = 1;
	    else
		count += 1;
	    attrCount.put(adata[column], count);
	}
	double sum = 0;
	for (Integer count : attrCount.values()) {
	    sum += count * count;
	}
	impurity = 1 - sum / (data.size() * data.size());

	return impurity;
    }

    /** 计算使用targetColumn分类后在decideColumn上的信息熵 */
    private static double calcGiniImpurity(List<String[]> data,
	    int targetColumn, int decideColumn) {

	HashMap<String, HashMap<String, Integer>> attrCountMap = prepare(data,
		targetColumn, decideColumn);

	double impurity = 0;
	for (HashMap<String, Integer> countMap : attrCountMap.values()) {
	    Integer tot = 0;
	    for (Integer count : countMap.values()) {
		tot += count;
	    }
	    double totSquare = 0d;
	    for (Integer count : countMap.values()) {
		double perc = count.doubleValue() / tot;
		totSquare += perc * perc;
	    }
	    impurity += (tot.doubleValue() / data.size()) * (1 - totSquare);
	}
	return impurity;
    }

    private static final double MIN_VALUE = 0.0000001d;
}