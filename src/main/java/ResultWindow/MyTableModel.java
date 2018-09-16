package ResultWindow;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Map;

public class MyTableModel extends AbstractTableModel {

    ArrayList<Float> color_index;
    ArrayList<Double> size;
    ArrayList<Boolean> flags;
    ArrayList<Integer> groupIndex;

    MyTableModel(Map<Float, Double> pixelsSize, ArrayList<Integer> groupIndex){
        color_index = new ArrayList<Float>();
        size = new ArrayList<Double>();
        flags = new ArrayList<Boolean>();
        this.groupIndex = groupIndex;
        for(Map.Entry<Float, Double> entry : pixelsSize.entrySet()) {
            Float key = entry.getKey();
            Double value = entry.getValue();
            color_index.add(key);
            size.add(value);
            flags.add(Boolean.TRUE);
        }
    }

    @Override
    public int getRowCount() {
        return color_index.size();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "Index";
            case 1:
                return "Size (microns)";
            case 2:
                return "Is checked";
            case 3:
                return "Group index";
            case 4:
                return "Color index";
            default:
                return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return Integer.class;
            case 1:
                return Double.class;
            case 2:
                return Boolean.class;
            case 3:
                return Integer.class;
            case 4:
                return Float.class;
            default:
                return null;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return rowIndex + 1;
            case 1:
                return size.get(rowIndex);
            case 2:
                return flags.get(rowIndex);
            case 3:
                return groupIndex.get(rowIndex);
            case 4:
                return color_index.get(rowIndex);
            default:
                return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex){
        if (columnIndex == 3)
            return true;
        else
            return false;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex == 2){
                flags.set(rowIndex, (Boolean) value);
                fireTableCellUpdated(rowIndex, columnIndex);

        }
        if (columnIndex == 3){
            groupIndex.set(rowIndex, (Integer) value);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    public Float getFloatValueByIndex(int index){
        return color_index.get(index - 1);
    }

    public void clearModel(){
        for (int i=0; i < flags.size(); i++){
            if (!flags.get(i)){
                setValueAt(Boolean.TRUE, i, 2);
            }
        }
    }

    public ArrayList<Float> getColorsIndex(){
        ArrayList<Float> resColorsIndex = new  ArrayList<Float>();
        for (int i=0; i < color_index.size(); i++){
            if (flags.get(i)){
                resColorsIndex.add(color_index.get(i));
            }
        }
        return resColorsIndex;
    }

    public int getRowByValue(Float value){
        for (int i=0; i < color_index.size(); i++) {
            if (Math.abs(value - color_index.get(i)) < 0.001) {
                return i;
            }
        }
        return -1;
    }

    public ArrayList<ObjectInfo> getInfo(){
        ArrayList<ObjectInfo> info = new ArrayList<ObjectInfo>();
        for (int i=0; i < color_index.size(); i++){
            info.add(new ObjectInfo(i+1, color_index.get(i), size.get(i), groupIndex.get(i)));
        }

        return info;
    }

}