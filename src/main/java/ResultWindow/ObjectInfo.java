package ResultWindow;


public class ObjectInfo {

    private Float color;
    private int index;
    private Double size;
    private Integer groupIndex;

    ObjectInfo(int index, Float color, Double size){
        this.index = index;
        this.color = color;
        this.size = size;
        this.groupIndex = 0;
    }

    ObjectInfo(int index, Float color, Double size, Integer groupIndex){
        this.index = index;
        this.color = color;
        this.size = size;
        this.groupIndex = groupIndex;
    }

    public Float getColor() {
        return color;
    }

    public Double getSize() {
        return size;
    }

    public int getIndex() {
        return index;
    }

    public Integer getGroupIndex() { return groupIndex; }
}
