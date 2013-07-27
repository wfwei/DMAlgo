package mine.dt;
public class Attribute {
    private String attrName;
    private String[] values;
    private Integer index;
    private Boolean available;

    public Attribute(String attrName, String[] values, int index,
	    boolean available) {
	this.attrName = attrName;
	this.values = values;
	this.index = index;
	this.available = available;
    }

    public String getAttrName() {
	return attrName;
    }

    public void setAttrName(String attrName) {
	this.attrName = attrName;
    }

    public String[] getValues() {
	return values;
    }

    public void setValues(String[] values) {
	this.values = values;
    }

    public boolean isAvailable() {
	return available;
    }

    public void setAvailable(boolean available) {
	this.available = available;
    }

    public Integer getIndex() {
	return index;
    }

    public void setIndex(Integer index) {
	this.index = index;
    }

}