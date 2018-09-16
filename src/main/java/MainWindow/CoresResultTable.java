package MainWindow;


import ResultWindow.ObjectInfo;
import ij.io.SaveDialog;

import javax.swing.*;
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
import java.util.HashMap;

public class CoresResultTable extends JFrame implements ActionListener, ItemListener {

    protected Button buttonSaveCSV;
    protected JTable table;
    protected CoresResultModel tableModel;
    protected Label processingLabel;

    CoresResultTable(ArrayList<ObjectInfo> cellInfo, ArrayList<ObjectInfo> coreInfo, HashMap<Float, HashMap<Float, Double>> info) {
        super();

        tableModel = new CoresResultModel(cellInfo, coreInfo, info);

        table = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setPreferredSize(new Dimension(400, 500));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);


        table.setCellSelectionEnabled(true);
        ListSelectionModel cellSelectionModel = table.getSelectionModel();
        cellSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

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
                //System.out.println(path);
                BufferedWriter writer = new BufferedWriter(new FileWriter(path));
                writer.write("Cell Index; Cell Size (microns); Core Index; Core Size (microns)\n");
                ArrayList<TableElement> info = tableModel.getElements();
                //System.out.println(info.get(0).getCellSize());
                for (TableElement object: info){
                    BigDecimal format_cell_size = new BigDecimal(object.getCellSize()).setScale(5, RoundingMode.HALF_UP);
                    BigDecimal format_core_size = new BigDecimal(object.getCoreSize()).setScale(5, RoundingMode.HALF_UP);
                    //Math.round(size * 100000000) / 1000.0;
                    writer.write(Integer.toString(object.getCellIndex()) + ";" + (format_cell_size.toString().replace('.', ',')) +
                            ";" + Integer.toString(object.getCoreIndex()) + ";" + (format_core_size.toString().replace('.', ',')) +
                            ";" + Double.toString(object.getProbability()) + "\n");
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

}
