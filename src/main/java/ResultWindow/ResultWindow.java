package ResultWindow;

import LsmReader.CZLSMInfo;
import MainWindow.CustomCanvas;
import MainWindow.ImageListenerInterface;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.StackWindow;
import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class ResultWindow extends StackWindow implements ActionListener, ItemListener, TableListenerInterface
        , ImageListenerInterface {

    private Button buttonSaveTiff, buttonCountPixels, buttonDelete, buttonDeleteObject, buttonGroup, buttonClear, buttonCheck, buttonCancel, showOrigImage;
    private Label processingLabel, BadPixelLabel;
    private TextField badPixelParametr;
    private Checkbox isCores, ConcateCores;
    private ResultTable resTable;
    private ImagePlus orig_image;
    private ImagePlus newFragmnetOrigImage;
    //protected int channelsCount;
    protected CheckboxGroup cbg;
    //protected ResultImageProcessor rip;
    protected TableInteractionProcessor tip;
    protected ImagePlus thisImg;
    protected CustomCanvas ic;
    protected HashSet<Float> selectedColors;
    protected CZLSMInfo info;
    protected Float allocationColor;


    public ResultWindow(String windowName, ImagePlus resultImp, CustomCanvas ic, CZLSMInfo info, ImagePlus orig_image) {
        super(resultImp, ic);
        this.ic = ic;
        this.info = info;
        this.orig_image = orig_image;
        this.allocationColor = Float.parseFloat("2000");
        if (orig_image != null)
            this.newFragmnetOrigImage = orig_image.duplicate();
        else
            this.newFragmnetOrigImage = null;
        thisImg = resultImp;
        resTable = null;
        //System.out.println(windowName);
        this.setName(windowName);

        tip = new TableInteractionProcessor(resultImp, info);
        this.ic.addListener(this);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                //System.exit(0);
            }
        });

        addPanel();
    }

    void addPanel() {
        Panel panel = new Panel();
        panel.setLayout(new BorderLayout());
        Panel leftMenu = new Panel();
        leftMenu.setLayout(new GridLayout(2, 1));
        Panel rightMenu = new Panel();
        rightMenu.setLayout(new GridLayout(2, 1));
        Panel centerMenu = new Panel();
        centerMenu.setLayout(new FlowLayout());
        Panel centerPanel = new Panel();
        centerPanel.setLayout(new GridLayout(2, 2, 5, 5));
        Panel botPanel = new Panel();
        botPanel.setLayout(new GridLayout());
        panel.add(leftMenu, BorderLayout.WEST);
        panel.add(rightMenu, BorderLayout.EAST);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(botPanel, BorderLayout.SOUTH);
        centerPanel.add(centerMenu, BorderLayout.NORTH);
        add(panel, BorderLayout.SOUTH);

        //cbg = new CheckboxGroup();
        isCores = new Checkbox("Nucleus image");
        isCores.setState(false);
        rightMenu.add(isCores);
        ConcateCores = new Checkbox("Concat nucleus");
        ConcateCores.setState(false);
        rightMenu.add(ConcateCores);
        ConcateCores.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                //System.out.println("q");
                if (ConcateCores.getState()){
                    processingLabel.setText("Processing...");
                    thisImg.setStack(tip.combineCores());
                    processingLabel.setText("Done");
                    buttonCheck.setEnabled(false);
                    buttonDelete.setEnabled(false);
                }
                else{
                    thisImg.setStack(tip.getOriginalImageStack());
                    buttonCheck.setEnabled(true);
                    buttonDelete.setEnabled(true);
                    if (resTable != null) {
                        resTable.clearTable();
                    }
                }
            }
        });

        buttonSaveTiff = new Button(" Save as TIFF");
        buttonSaveTiff.addActionListener(this);
        rightMenu.add(buttonSaveTiff);

        BadPixelLabel = new Label("Pixels in bad region:");
        centerMenu.add(BadPixelLabel);
        badPixelParametr = new TextField("5", 6);
        centerMenu.add(badPixelParametr);
        buttonDelete = new Button("Delete Boundary Cells");
        buttonDelete.addActionListener(this);
        centerMenu.add(buttonDelete);
        buttonCountPixels = new Button("Count pixels");
        buttonCountPixels.addActionListener(this);
        centerMenu.add(buttonCountPixels);
        showOrigImage = new Button("Show original image");
        showOrigImage.addActionListener(this);
        centerMenu.add(showOrigImage);
        if (this.orig_image == null)
            showOrigImage.setEnabled(false);
        buttonClear = new Button("Clear");
        buttonClear.addActionListener(this);
        centerMenu.add(buttonClear);
        processingLabel = new Label("         ");
        botPanel.add(processingLabel);

        buttonCheck = new Button("Select cells");
        buttonCheck.addActionListener(this);
        leftMenu.add(buttonCheck);
        buttonDeleteObject = new Button("Delete");
        buttonDeleteObject.addActionListener(this);
        leftMenu.add(buttonDeleteObject);
        buttonGroup = new Button("Group");
        buttonGroup.addActionListener(this);
        leftMenu.add(buttonGroup);
        buttonCancel = new Button("Cancel");
        buttonCancel.addActionListener(this);
        leftMenu.add(buttonCancel);
        buttonCancel.setEnabled(false);
        buttonDeleteObject.setEnabled(false);
        buttonGroup.setEnabled(false);

        pack();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Point loc = getLocation();
        Dimension size = getSize();
        if (loc.y+size.height>screen.height)
            getCanvas().zoomOut(0, 0);
    }

    public void itemStateChanged(ItemEvent e)
    {

    }

    public void actionPerformed(ActionEvent e) {
        Object b = e.getSource();
        ImagePlus imp = this.getImagePlus();

        if (b==buttonSaveTiff){
            (new FileSaver(imp)).saveAsTiff();
        }


        if (b==buttonCountPixels){
            tip.setBadPixelsGroup(Integer.parseInt(badPixelParametr.getText()));
            thisImg.setStack(tip.calculatePixelsSize());
            HashMap<Float, Double> result = tip.getPixelsInfo();
            if (resTable != null) {
                resTable.setVisible(false);
                this.remove(resTable);
            }
            resTable = new ResultTable("Res table " + this.getName(), result, tip.getGroupIndex());
            //allocationColor = rip.getAllocationColor();
            resTable.addListener(this);
        }

        if (b==showOrigImage){
            Rectangle r = this.getImagePlus().getRoi().getBounds();
            int xstart = (int) r.getX();
            int ystart = (int) r.getY();
            int width = (int) r.getWidth();
            int height = (int) r.getHeight();
            int xloc = 0;
            int yloc = 0;
            ImageStack base_stack = this.orig_image.getStack();
            ImageStack newStack = new ImageStack(width, height);
            ImageProcessor impr_target;
            for (int i=1; i < base_stack.getSize() + 1; i++){
                if (base_stack.getBitDepth() == 8) {
                    impr_target = new ByteProcessor(width, height);
                }
                else{
                    impr_target = new ShortProcessor(width, height);
                }
                ImageProcessor impr_base = base_stack.getProcessor(i);

                //check impr_base ROI
                if ((xstart + width >= impr_base.getWidth())||(ystart + height >= impr_base.getHeight())){
                    width = impr_base.getWidth() - xstart;
                    height = impr_base.getHeight() - ystart;
                }
                //check impr_target ROI
                if ((xloc + width >= impr_target.getWidth())||(yloc + height >= impr_target.getHeight())){
                    width = impr_target.getWidth() - xloc;
                    height = impr_target.getHeight() - yloc;
                }
                int pix;
                for (int y = ystart; y < ystart + height; y++){
                    for (int x = xstart; x < xstart + width; x++){
                        try {
                            pix = impr_base.getPixel(x, y);

                            impr_target.putPixel(xloc + x - xstart, yloc + y - ystart, pix);
                        }
                        catch (IndexOutOfBoundsException e1){
                            System.out.println(x + " : " + y);
                            throw e1;
                        }
                    }
                }
                //impr_target.setColorModel(this.orig_image.getStack().getColorModel());
                newStack.addSlice(base_stack.getSliceLabel(i), impr_target);
            }
            this.newFragmnetOrigImage.resetStack();
            this.newFragmnetOrigImage.setStack(newStack);
            this.newFragmnetOrigImage.repaintWindow();
            this.newFragmnetOrigImage.show();
            this.newFragmnetOrigImage.getCanvas().requestFocus();
            this.newFragmnetOrigImage.getWindow().setFocusableWindowState(true);
            this.newFragmnetOrigImage.getWindow().requestFocus();
            //new ImagePlus("orig fragment", newStack).show();
        }

        if (b==buttonDelete){
            processingLabel.setText("Processing...");
            thisImg.setStack(tip.deleteBoundaryColors());
            /*
            if (resTable != null) {
                thisImg.setStack(tip.calculatePixelsSize());
                resTable.setVisible(false);
                this.remove(resTable);
                resTable = new ResultTable("Res table " + this.getName(), tip.getPixelsInfo());
                resTable.addListener(this);
            }
            */
            processingLabel.setText("Done");
        }

        if (b==buttonClear){
            thisImg.setStack(tip.getOriginalImageStack());
            if (resTable != null) {
                resTable.clearTable();
            }
        }

        if (b==buttonGroup){
            int indexGroup = tip.addNewGroup(selectedColors);
            selectedColors.clear();
            buttonCheck.setLabel("Select cells");
            processingLabel.setText("Created new group with index: " + String.valueOf(indexGroup));
            buttonCancel.setEnabled(false);
            buttonDeleteObject.setEnabled(false);
            buttonGroup.setEnabled(false);
        }

        if (b==buttonDeleteObject) {
            for (Float color: selectedColors){
                thisImg.setStack(tip.turnOff(color));
            }
            this.getImagePlus().updateAndRepaintWindow();
            selectedColors.clear();
            buttonCheck.setLabel("Select cells");
            processingLabel.setText("Deleted");
            buttonCancel.setEnabled(false);
            buttonDeleteObject.setEnabled(false);
            buttonGroup.setEnabled(false);
        }

        if (b==buttonCheck){
            if (!buttonCheck.getLabel().equals("Combine")) {
                selectedColors = new HashSet<Float>();
                processingLabel.setText("Selecting...");
                buttonCancel.setEnabled(true);
                buttonDeleteObject.setEnabled(true);
                buttonGroup.setEnabled(true);
                buttonCheck.setLabel("Combine");
            }
            else{
                thisImg.setStack(tip.combineCells(selectedColors, 0));
                this.getImagePlus().updateAndRepaintWindow();
                selectedColors.clear();
                buttonCheck.setLabel("Select cells");
                processingLabel.setText("Combined");
                buttonCancel.setEnabled(false);
                buttonDeleteObject.setEnabled(false);
                buttonGroup.setEnabled(false);
            }
        }

        if (b==buttonCancel){
            selectedColors.clear();
            buttonCheck.setLabel("Select cells");
            processingLabel.setText("Canceled");
            buttonCancel.setEnabled(false);
        }

        //ImageCanvas ic = imp.getCanvas();
        //if (ic!=null)
        //    ic.requestFocus();
    }

    public boolean isCoresWindow(){
        return isCores.getState();
    }

    public void setCellVisible(Float index, Boolean flag) {
        if (flag) {
            thisImg.setStack(tip.turnOn(index));
        }
        else {
            thisImg.setStack(tip.turnOff(index));
        }
        this.getImagePlus().updateAndRepaintWindow();
        //repaint();
    }

    public void selectCell(Float index){
        if (colorIsObject(index)) {
            thisImg.setStack(tip.selectCell(index, allocationColor));
            this.getImagePlus().updateAndRepaintWindow();
        }
    }

    public void deselectCell(){
        thisImg.setStack(tip.turnOff(Float.valueOf("-1")));
        this.getImagePlus().updateAndRepaintWindow();
    }

    public void imageClicked(Float color){
        if ((resTable != null) && (colorIsObject(color))) {
            int index = resTable.tableModel.getRowByValue(color);
            System.out.println(index);
            //resTable.table.setRowSelectionInterval(index, index + 1);
            index += 1;
            if (buttonCheck.getLabel() == "Combine"){
                selectedColors.add(color);
                processingLabel.setText("Selected colors: " + selectedColors);
            }
            else {
                processingLabel.setText("Selected color index: " + index);
            }
            this.selectCell(color);
        }
        else{
            if (buttonCheck.getLabel() == "Combine"){
                selectedColors.add(color);
                processingLabel.setText("Selected colors: " + selectedColors);
            }
        }
    }

    public ArrayList<ObjectInfo> getTableInfo(){
        if (resTable != null) {
            return resTable.tableModel.getInfo();
        }
        else{
            return null;
        }
    }

    private boolean colorIsObject(Float color) {
        if (color > 1.0) {
            return true;
        } else {
            return false;
        }
    }
}
