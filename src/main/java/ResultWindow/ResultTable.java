package ResultWindow;

import ij.io.SaveDialog;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Map;


public class ResultTable extends JFrame implements ActionListener, ItemListener {

    protected Button buttonSaveCSV;
    protected JTable table;
    protected Label processingLabel;
    protected TableListenerInterface listener;
    protected MyTableModel tableModel;
    private int lastSelectedRow = -1;

    ResultTable(String windowName, Map<Float, Double> pixelsSize, ArrayList<Integer> groupIndex){
        super(windowName);

        System.out.println("new table size : " + pixelsSize.size());
        tableModel = new MyTableModel(pixelsSize, groupIndex);
        listener = null;

        table = new JTable(tableModel);
        //table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor());
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setPreferredSize(new Dimension(400, 500));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        this.setEnabled(true);
        this.setState(0);

        table.setCellSelectionEnabled(true);
        ListSelectionModel cellSelectionModel = table.getSelectionModel();
        cellSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        cellSelectionModel.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting())
                    return;

                int selectedRow = table.getSelectedRow();
                int selectedColumn = table.getSelectedColumn();

                if (selectedRow >=0){
                    if (selectedColumn == 2) {
                        //System.out.println("Selected: " + selectedRow + "  " + selectedColumn);
                        Boolean flagValue = (Boolean) table.getValueAt(selectedRow, selectedColumn);
                        //System.out.println(flagValue);
                        if (flagValue)
                            table.setValueAt(Boolean.FALSE, selectedRow, selectedColumn);
                        else
                            table.setValueAt(Boolean.TRUE, selectedRow, selectedColumn);

                        if (listener != null) {
                            Float value = tableModel.getFloatValueByIndex((Integer) table.getValueAt(selectedRow, 0));
                            //System.out.println("Selected: " + value);
                            listener.setCellVisible(value, !flagValue);
                        }
                        //table.clearSelection();
                    }
                    if (selectedColumn == 0){
                        if (listener != null) {
                            if (lastSelectedRow == selectedRow){
                                listener.deselectCell();
                                lastSelectedRow = -1;
                            }
                            else {
                                Float value = tableModel.getFloatValueByIndex((Integer) table.getValueAt(selectedRow, 0));
                                //System.out.println("Selected: " + value);
                                listener.selectCell(value);
                            }
                        }
                        lastSelectedRow = selectedRow;
                    }
                    table.clearSelection();
                    //System.out.println("Done.");
                }

            }

        });



        add(tableScrollPane);

        Panel panel = new Panel();
        panel.setLayout(new BorderLayout());
        Panel leftMenu = new Panel();
        leftMenu.setLayout(new GridLayout(1, 1));
        Panel rightMenu = new Panel();
        rightMenu.setLayout(new GridLayout(1, 1));
        Panel centerMenu = new Panel();
        centerMenu.setLayout(new FlowLayout());
        Panel centerPanel = new Panel();
        centerPanel.setLayout(new GridLayout(1, 1, 5, 5));
        Panel botPanel = new Panel();
        botPanel.setLayout(new GridLayout());
        panel.add(leftMenu, BorderLayout.WEST);
        panel.add(rightMenu, BorderLayout.EAST);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(botPanel, BorderLayout.SOUTH);
        centerPanel.add(centerMenu, BorderLayout.NORTH);
        add(panel, BorderLayout.SOUTH);

        buttonSaveCSV = new Button(" Save ");
        buttonSaveCSV.addActionListener(this);
        //buttonSaveCSV.setSize(50, 30);
        centerMenu.add(buttonSaveCSV);

        processingLabel = new Label("         ");
        botPanel.add(processingLabel);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        requestFocus();
    }

    public void itemStateChanged(ItemEvent e)
    {

    }

    public void actionPerformed(ActionEvent e) {
        Object b = e.getSource();
        if (b == buttonSaveCSV){
            try {

                SaveDialog openDialog = new SaveDialog("Save as CSV", this.getTitle(), ".csv");
                String directory = openDialog.getDirectory();
                String name = openDialog.getFileName();
                if (name == null) return;

                String path = directory + name;
                if (name.split(".").length == 1) {
                    path += ".csv";
                }
                System.out.println(path);
                BufferedWriter writer = new BufferedWriter(new FileWriter(path));
                writer.write("Number; Size(Microns); Group Index; Color index\n");
                ArrayList<ObjectInfo> info = tableModel.getInfo();
                //System.out.println(info.get(0).getSize());
                for (ObjectInfo myObject: info){
                    BigDecimal format_size = new BigDecimal(myObject.getSize()).setScale(5, RoundingMode.HALF_UP);
                    //Math.round(size * 100000000) / 1000.0;
                    writer.write(Integer.toString(myObject.getIndex()) + ";"
                            + (format_size.toString().replace('.', ',')) + ";"
                            + Integer.toString(myObject.getGroupIndex()) + ";"
                            + (Float.toString(myObject.getColor()).replace('.', ',')) + "\n");
                }
                writer.close();
                //System.out.println("SAVED");
                processingLabel.setText("SAVED");
            }
            catch (IOException err) {
                err.printStackTrace();
            }
        }
    }

    public void addListener(TableListenerInterface newListener){
        listener = newListener;
    }

    public void clearTable(){
        tableModel.clearModel();
    }

}