package MainWindow;


import ResultWindow.ObjectInfo;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class CoresResultModel extends AbstractTableModel {

    ArrayList<ObjectInfo> cellInfo;
    ArrayList<ObjectInfo> coreInfo;
    HashMap<Float, HashMap<Float, Double>> info;
    ArrayList<TableElement> elements;
    private double EPS = 0.01;

    CoresResultModel(ArrayList<ObjectInfo> cellInfo, ArrayList<ObjectInfo> coreInfo, HashMap<Float, HashMap<Float, Double>> inf){
        this.cellInfo = cellInfo;
        this.coreInfo = coreInfo;
        this.info = inf;

        System.out.println(info);
        System.out.println(info.size());

        elements = new ArrayList<TableElement>();
        HashSet<Float> markedColors = new HashSet<Float>();

        for (Float cellColor: info.keySet()){
            HashMap<Float, Double> curCellInfo = info.get(cellColor);
            ObjectInfo curCellobj = null;
            for (ObjectInfo cellobj: cellInfo){
                if (Math.abs(cellobj.getColor() - cellColor) < EPS){
                    curCellobj = cellobj;
                    break;
                }
            }
            for (Float coreColor: curCellInfo.keySet()) {
                for (ObjectInfo coreobj : coreInfo) {
                    if (Math.abs(coreobj.getColor() - coreColor) < EPS) {
                        elements.add(new TableElement(curCellobj.getIndex(), curCellobj.getSize(),
                                coreobj.getIndex(), coreobj.getSize(), curCellInfo.get(coreColor)));
                        break;
                    }
                }

            }
        }

        /*
        for (ObjectInfo Cellobj: cellInfo){
            if (info.containsKey(Cellobj.getColor())){
                if (info.get(Cellobj.getColor()).size() > 1){
                    elements.add(new TableElement(Cellobj.getIndex(), Cellobj.getSize(), -1, 0.0));
                }
                else{
                    Float cellColor = (Float) info.get(Cellobj.getColor()).toArray()[0];
                    for (ObjectInfo coreObj: coreInfo){
                        if (Math.abs(coreObj.getColor() - cellColor) < 0.01){
                            elements.add(new TableElement(Cellobj.getIndex(), Cellobj.getSize(),
                                    coreObj.getIndex(), coreObj.getSize()));
                            break;
                        }
                    }
                }
            }
            else{
                elements.add(new TableElement(Cellobj.getIndex(), Cellobj.getSize(), 0, 0.0));
            }
        }
        */

    }

    public int getRowCount() {
        return elements.size();
    }

    public int getColumnCount() {
        return 5;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return elements.get(rowIndex).getCellIndex();
            case 1:
                return elements.get(rowIndex).getCellSize();
            case 2:
                return elements.get(rowIndex).getCoreIndex();
            case 3:
                return elements.get(rowIndex).getCoreSize();
            case 4:
                return elements.get(rowIndex).getProbability();
            default:
                return null;
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "Cell Index";
            case 1:
                return "Cell size";
            case 2:
                return "Core index";
            case 3:
                return "Core size";
            case 4:
                return "Probability";
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
                return Integer.class;
            case 3:
                return Double.class;
            case 4:
                return Double.class;
            default:
                return null;
        }
    }

    public  ArrayList<TableElement> getElements(){
        return elements;
    }
}
