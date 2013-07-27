package mine.dt;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class DecisionTree {

    private boolean isLeaf; // 是否是叶子节点
    private String resultValue; // 如果是叶子节点，该值表示最终分类（decideAttr的取值之一）

    private static HashSet<Attribute> AllAttrs = new HashSet<Attribute>(); // 所有的分类属性
    private HashSet<Attribute> availableAttrs = new HashSet<Attribute>(); // 可用的分类属性（祖先节点没有用过）

    private List<String[]> data = new LinkedList<String[]>(); // 节点上的数据，非叶子节点为空
    private static Attribute DecideAttr; // 决策属性
    private Attribute splitAttr; // 当前节点的分裂属性如果分裂

    private DecisionTree parent; // 指向父节点，如果当前节点是root节点，则为空
    private HashMap<String, DecisionTree> children = new HashMap<String, DecisionTree>();

    public DecisionTree() {
	this.parent = null;
	this.isLeaf = false;
    }

    // set as leaf and make a tag as result
    public void setAsLeaf() {
	isLeaf = true;
	if (!data.isEmpty()) {
	    // TODO much refine work to do
	    resultValue = data.get(0)[DecideAttr.getIndex()];
	}
    }

    public void split() {
	Attribute spliteAttr = selectSplitAttr();
	this.setSplitAttr(spliteAttr);
	for (String attrVal : spliteAttr.getValues()) {
	    children.put(attrVal, new DecisionTree());
	}
	this.getAvailableAttrs().remove(spliteAttr);
	for (String[] adata : data) {
	    DecisionTree child = children.get(adata[spliteAttr.getIndex()]);
	    child.addData(adata);
	    child.setParent(this);
	    child.addAvailableAttrs(this.getAvailableAttrs());
	}
	this.getAvailableAttrs().clear();
	data.clear();
    }

    /**
     * 选择不纯度下降最大的属性（作为分裂属性）
     * 
     * @return
     */
    public Attribute selectSplitAttr() {
	Attribute splitAttr = null;
	double maxGain = Double.MIN_VALUE;
	for (Attribute attr : availableAttrs) {

	    double gain = Impurity.calcGiniGainRatio(data, attr.getIndex(),
		    DecideAttr.getIndex());
	    if (gain > maxGain) {
		maxGain = gain;
		splitAttr = attr;
	    }
	}
	return splitAttr;
    }

    public boolean isPure() {
	HashSet<String> ResultValueSet = new HashSet<String>();
	for (String[] adata : data) {
	    ResultValueSet.add(adata[DecideAttr.getIndex()]);
	    if (ResultValueSet.size() > 1)
		return false;
	}
	return true;
    }

    public void addChild(String attrVal, DecisionTree child) {
	this.children.put(attrVal, child);
    }

    public HashMap<String, DecisionTree> getChildren() {
	return children;
    }

    public boolean isLeaf() {
	return isLeaf;
    }

    public void setLeaf(boolean isLeaf) {
	this.isLeaf = isLeaf;
    }

    public DecisionTree getParent() {
	return parent;
    }

    public void setParent(DecisionTree parent) {
	this.parent = parent;
    }

    public String getResultValue() {
	return resultValue;
    }

    public void setResultValue(String resultValue) {
	this.resultValue = resultValue;
    }

    public static void addAttr(Attribute attr) {
	AllAttrs.add(attr);
    }

    public void addAvailableAttr(Attribute attr) {
	availableAttrs.add(attr);
    }

    public void addAvailableAttrs(Collection<Attribute> attrNames) {
	availableAttrs.addAll(attrNames);
    }

    public Set<Attribute> getAvailableAttrs() {
	return availableAttrs;
    }

    public static Set<Attribute> getAllAttrs() {
	return AllAttrs;
    }

    public void addData(String[] adata) {
	data.add(adata);
    }

    public Attribute getDecideAttr() {
	return DecideAttr;
    }

    public void setDecideAttr(Attribute decideAttr) {
	DecisionTree.DecideAttr = decideAttr;
    }

    public Attribute getSplitAttr() {
	return splitAttr;
    }

    public void setSplitAttr(Attribute splitAttr) {
	this.splitAttr = splitAttr;
    }

}