package MainWindow;

import Image2DWindow.Image2DWindow;
import ResultWindow.ObjectInfo;
import ResultWindow.ResultWindow;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.MessageDialog;
import ij.gui.StackWindow;
import ij.gui.YesNoCancelDialog;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import inra.ijpb.plugins.MorphologicalSegmentation;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.stream.Collectors;


public class MyWindow extends StackWindow implements ActionListener, ItemListener {

    protected Button buttonSaveAsTif, buttonSaveJpg, buttonApplyOffset, buttonDiffusion, buttonNormalization, buttonClean,
            buttonOpenSegmentation, buttonOpenImage, buttonNormalizationSlices, buttonCalcInfo,
            buttonCutSlices, buttonMorfSegmentation, cutButton, buttonGetFinalResult, to2DImage, getPixGraph;
    private TextField curOffsetX, curOffsetY, curOffsetZ, normalizationParametr, MaxDiffField, latticeSizeField,
            cellSizeField;
    private Checkbox newChannelColorRed, newChannelColorGreen, newChannelColorBlue;
    private ArrayList<TextField> channelCoefFields;
    private Label processingLabel, labelOffsetX, labelOffsetY, labelOffsetZ, latticeLabel, maxDiffLabel, cellSizeLabel;
    protected int channelsCount;
    public CheckboxGroup cbg;
    public ArrayList<Checkbox> chs;
    protected MyLSMImage myimp;
    protected Anisotropic_Diffusion_2D diffusion2D;
    protected Normalization normalization;
    protected MorphologicalSegmentation morphologicalSegmentation;
    protected LinkedList<ResultWindow> resultWindows;
    protected Image2DWindow image2DWind;
    protected boolean isStandardMode;
    protected CustomCanvas ic;

    public MyWindow(MyLSMImage myimp, CustomCanvas ic) {
        super(myimp.getImp(), ic);
        this.myimp = myimp;
        this.ic = ic;
        this.image2DWind = null;
        resultWindows = new LinkedList<ResultWindow>();
        channelsCount = myimp.getChannelCount();
        isStandardMode = true;
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
        rightMenu.setLayout(new GridLayout(2, 1, 5, 5));
        Panel centerMenu = new Panel();
        //centerMenu.setLayout(new FlowLayout());
        Panel centerMenu2 = new Panel();
        //centerMenu2.setLayout(new FlowLayout());
        Panel centerPanel = new Panel();
        centerPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        Panel botPanel = new Panel();
        botPanel.setLayout(new GridLayout(1, 10));

        buttonSaveAsTif = new Button("Save image");
        buttonSaveAsTif.addActionListener(this);
        rightMenu.add(buttonSaveAsTif);
        buttonOpenSegmentation= new Button("Open segmentation");
        buttonOpenSegmentation.addActionListener(this);
        rightMenu.add(buttonOpenSegmentation);
        buttonSaveJpg = new Button("Save slice");
        buttonSaveJpg.addActionListener(this);
        rightMenu.add(buttonSaveJpg);
        buttonOpenImage = new Button("Open image");
        buttonOpenImage.addActionListener(this);
        rightMenu.add(buttonOpenImage);
        buttonClean = new Button("Get original image");
        buttonClean.addActionListener(this);
        centerMenu.add(buttonClean);
        labelOffsetX = new Label("X:");
        centerMenu.add(labelOffsetX);
        curOffsetX = new TextField("0");
        centerMenu.add(curOffsetX);
        labelOffsetY = new Label("Y:");
        centerMenu.add(labelOffsetY);
        curOffsetY = new TextField("0");
        centerMenu.add(curOffsetY);
        labelOffsetZ = new Label("Z:");
        centerMenu.add(labelOffsetZ);
        curOffsetZ = new TextField("0");
        centerMenu.add(curOffsetZ);
        buttonApplyOffset = new Button("Apply offset");
        buttonApplyOffset.addActionListener(this);
        centerMenu.add(buttonApplyOffset);
        //normalizationParametr = new TextField("30");
        //centerMenu.add(normalizationParametr);
        //buttonNormalization = new Button("Normalization");
        //buttonNormalization.addActionListener(this);
        //centerMenu.add(buttonNormalization);
        //buttonCutSlices = new Button("Split\\Join slices");
        //buttonCutSlices.addActionListener(this);
        //centerMenu.add(buttonCutSlices);
        //buttonNormalizationSlices = new Button("Slice Normalization");
        //buttonNormalizationSlices.addActionListener(this);
        //centerMenu.add(buttonNormalizationSlices);
        //buttonCalcInfo = new Button("GetSlicesInfo");
        //buttonCalcInfo.addActionListener(this);
        //centerMenu.add(buttonCalcInfo);
        cutButton = new Button("Cut Image");
        cutButton.addActionListener(this);
        centerMenu.add(cutButton);
        buttonDiffusion = new Button("Diffusion");
        buttonDiffusion.addActionListener(this);
        centerMenu.add(buttonDiffusion);
        buttonMorfSegmentation = new Button("MorfSegmentation");
        buttonMorfSegmentation.addActionListener(this);
        centerMenu.add(buttonMorfSegmentation);
        cellSizeLabel = new Label("Cell depth");
        centerMenu.add(cellSizeLabel, BorderLayout.SOUTH);
        cellSizeField = new TextField("8");
        centerMenu.add(cellSizeField, BorderLayout.SOUTH);
        buttonGetFinalResult= new Button("cell-nucleus");
        buttonGetFinalResult.addActionListener(this);
        centerMenu.add(buttonGetFinalResult);

        latticeLabel = new Label("Lattice size");
        centerMenu2.add(latticeLabel, BorderLayout.SOUTH);
        latticeSizeField = new TextField("5");
        centerMenu2.add(latticeSizeField, BorderLayout.SOUTH);
        maxDiffLabel = new Label("Threshold");
        centerMenu2.add(maxDiffLabel, BorderLayout.SOUTH);
        MaxDiffField = new TextField("0");
        centerMenu2.add(MaxDiffField, BorderLayout.SOUTH);
        to2DImage = new Button("get 2D image");
        to2DImage.addActionListener(this);
        centerMenu2.add(to2DImage, BorderLayout.SOUTH);

        getPixGraph = new Button("Pix Graph");
        getPixGraph.addActionListener(this);
        centerMenu.add(getPixGraph);

        //xstart = new TextField("");
        //centerMenu.add(xstart);
        //xstartLavel = new Label("Start x");
        //xend = new TextField("");
        //centerMenu.add(xend);
        //ystart = new TextField("");
        //centerMenu.add(ystart);
        //yend = new TextField("");
        //centerMenu.add(yend);

        cbg = new CheckboxGroup();
        chs = new ArrayList<Checkbox>(channelsCount);
        for (int i=1; i < channelsCount; i++) {
            Checkbox ch = new Checkbox("Ch" + Integer.toString(i), cbg, i == 1);
            ch.setName(Integer.toString(i - 1));
            ch.addItemListener(this);
            leftMenu.add(ch);
            chs.add(ch);
        }
        Checkbox ch = new Checkbox("New channel", cbg, false);
        ch.setName(Integer.toString(channelsCount - 1));
        ch.addItemListener(this);
        leftMenu.add(ch);
        chs.add(ch);

        channelCoefFields = new ArrayList<>();
        for (int i=1; i < channelsCount; i++) {
            Label chCoefLabel = new Label(String.format("ch%d coef", i));
            botPanel.add(chCoefLabel);
            TextField chCoefField = new TextField("1");
            botPanel.add(chCoefField);
            channelCoefFields.add(chCoefField);
        }

        CheckboxGroup newChannelColor = new CheckboxGroup();
        newChannelColorRed = new Checkbox("Red", newChannelColor, true);
        newChannelColorGreen = new Checkbox("Green", newChannelColor, false);
        newChannelColorBlue = new Checkbox("Blue", newChannelColor, false);

        botPanel.add(newChannelColorRed);
        botPanel.add(newChannelColorGreen);
        botPanel.add(newChannelColorBlue);

        processingLabel = new Label("         ");
        botPanel.add(processingLabel);

        centerPanel.add(centerMenu);
        //centerPanel.add(centerMenu2, BorderLayout.SOUTH);

        panel.add(leftMenu, BorderLayout.WEST);
        panel.add(rightMenu, BorderLayout.EAST);
        panel.add(centerPanel, BorderLayout.NORTH);
        panel.add(centerMenu2, BorderLayout.CENTER);
        panel.add(botPanel, BorderLayout.SOUTH);

        add(panel, BorderLayout.SOUTH);
        pack();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Point loc = getLocation();
        Dimension size = getSize();
        if (loc.y+size.height>screen.height)
            getCanvas().zoomOut(0, 0);
    }

    public void itemStateChanged(ItemEvent e)
    {
        int channel_index = Integer.parseInt(cbg.getSelectedCheckbox().getName());
        int r = newChannelColorRed.getState() ?  255 : 0;
        int g = newChannelColorGreen.getState() ?  255 : 0;
        int b = newChannelColorBlue.getState() ?  255 : 0;
        if (channel_index == channelsCount - 1) {
            ArrayList<Double> coefficients = channelCoefFields.stream()
                    .map(field -> Double.parseDouble(field.getText()))
                    .collect(Collectors.toCollection(ArrayList::new));
            myimp.setNewChannel(coefficients, new Color(r, g, b));
        }
        myimp.setChanel(channel_index);
    }

    public void actionPerformed(ActionEvent e) {

        Object b = e.getSource();
        imp = myimp.getImp();

        if (b==buttonSaveAsTif){
            //FileInfo fi = myimp.getOriginalImage().getFileInfo();
            //imp.setProperty("jmanj", "haha");
            imp.setCalibration(myimp.getOriginalImage().getCalibration());
            //imp.setTitle(imp.getShortTitle() + " ch" + cbg.getSelectedCheckbox().getName());
            //imp.setFileInfo(fi);
            (new FileSaver(imp)).saveAsTiff();
        }

        // now it is tif too
        if (b==buttonSaveJpg){
            /*
            SaveDialog openDialog = new SaveDialog("Save as Jpeg", imp.getShortTitle() + " Slice_"
                    + imp.getCurrentSlice() , ".jpg");
            String directory = openDialog.getDirectory();
            String name = openDialog.getFileName();
            if (name == null) return;

            String path = directory + name;
            if (name.split(".").length == 1) {
                path += ".jpg";
            }
            (new FileSaver(imp)).saveAsJpeg(path);
            */
            ImagePlus imtoSave = new ImagePlus(imp.getShortTitle() + " Slice_" + imp.getCurrentSlice(),
                    imp.getStack().getProcessor(imp.getCurrentSlice()));
            imp.setCalibration(myimp.getOriginalImage().getCalibration());
            (new FileSaver(imtoSave)).saveAsTiff();
        }

        if (b==buttonOpenImage){
            OpenDialog od = new OpenDialog("Open image...");
            String name = od.getFileName();
            if (name==null)
                return;
            String dir = od.getDirectory();
            String path = dir + name;
            ImagePlus new_imp = new ImagePlus(path);
            if (isStandardMode) {
                YesNoCancelDialog dialog = new YesNoCancelDialog(this, "Warning",
                        "All the changes on the current image will be lost.");
                if (dialog.yesPressed()) {
                    myimp.setImp(new_imp, true);
                    isStandardMode = false;
                    setViewMode();
                }
            }
            else{
                myimp.setImp(new_imp, true);
            }
        }

        if (b==buttonOpenSegmentation){
            OpenDialog od = new OpenDialog("Open segmentation image...");
            String name = od.getFileName();
            if (name==null)
                return;
            String dir = od.getDirectory();
            String path = dir + name;
            /*ImageOpener imgp = new ImageOpener();
            Reader red = new TIFFJAIFormat.Reader();

            FileInfo fi = new FileInfo();
            fi.fileFormat = FileInfo.TIFF;
            fi.fileName = name;
            fi.directory = dir;
            */
            ImagePlus new_imp = new ImagePlus(path);
            CustomCanvas cc = new CustomCanvas(new_imp);
            resultWindows.add(new ResultWindow(new_imp.getShortTitle(), new_imp, cc, myimp.iminfo, null));
            //cc.requestFocus();
        }

        if (b==buttonApplyOffset){
            processingLabel.setText("Processing...");
            myimp.setOffset(Integer.parseInt(curOffsetX.getText()), Integer.parseInt(curOffsetY.getText()), Integer.parseInt(curOffsetZ.getText()));
            processingLabel.setText("Done");
        }

        if (b==buttonDiffusion)
        {
            if (buttonDiffusion.getLabel() != "Get result") {
                diffusion2D = new Anisotropic_Diffusion_2D(processingLabel);
                diffusion2D.setup("", imp);
                if (diffusion2D.show_window(imp.getProcessor())) {
                    buttonDiffusion.setLabel("Get result");
                }
            }
            else{
                if (processingLabel.getText() == "Diffusion Finished"){
                    WindowManager.putBehind();
                    ImagePlus inputImage = WindowManager.getCurrentImage().duplicate();
                    //WindowManager.getCurrentWindow().close();
                    myimp.setImp(inputImage);
                    processingLabel.setText("Diffusion: finished");
                    buttonDiffusion.setLabel("Diffusion");
                }
                else {
                    diffusion2D.stopIteration();
                    //myimp.setImpStack(diffusion2D.getResultStack());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    ImagePlus inputImage = WindowManager.getCurrentImage().duplicate();
                    WindowManager.getCurrentWindow().close();
                    myimp.setImp(inputImage);
                    processingLabel.setText("Diffusion: finished");
                    buttonDiffusion.setLabel("Diffusion");
                    //diffusion2D.interrupt();
                    //diffusion2D = null;
                }
            }
        }

        if (b==buttonNormalization){
            normalization = new Normalization(imp);
            try {
                processingLabel.setText("Processing...");
                myimp.setImp(normalization.process(Integer.parseInt(normalizationParametr.getText())));
                processingLabel.setText("Done");
            }catch (Exception exp){
                //DOTO
            }
        }

        if (b==to2DImage){
            processingLabel.setText("Processing...");
            ImagePlus image2d = myimp.convertTo2DImage(
                    Integer.parseInt(latticeSizeField.getText()),
                    Integer.parseInt(MaxDiffField.getText())
            );
            image2DWind = new Image2DWindow(image2d, myimp.iminfo, myimp.get2DProc());
            image2DWind.setVisible(true);
            processingLabel.setText("Done");
        }

        if (b==buttonNormalizationSlices){
            processingLabel.setText("Processing...");
            myimp.makeSliceNormalization(Integer.parseInt(normalizationParametr.getText()));
            processingLabel.setText("Done");
        }

        if (b==getPixGraph){
            if (this.ic.isConstructGraph()) {
                this.ic.setIsConstructGraph(false, Integer.parseInt(MaxDiffField.getText()));
                processingLabel.setText("");
            }
            else {
                this.ic.setIsConstructGraph(true, Integer.parseInt(MaxDiffField.getText()));
                processingLabel.setText("Select pixel");
            }
        }

        if (b==buttonClean){
            myimp.cleanImage();
            curOffsetX.setText("0");
            curOffsetY.setText("0");
            curOffsetZ.setText("0");
            isStandardMode = true;
            setViewMode();
        }
        if (b==buttonCalcInfo){
            myimp.splitAndCalcAverageAndDispersion();
        }

        if (b==buttonCutSlices){
            myimp.changeDivided();
        }

        if (b==buttonMorfSegmentation){
            if (buttonMorfSegmentation.getLabel() != "Get result") {
                morphologicalSegmentation = new MorphologicalSegmentation();
                morphologicalSegmentation.run("");
                buttonMorfSegmentation.setLabel("Get result");
                }
            else{

                //System.out.println(WindowManager.getImageCount());
                //System.out.println("!!!");
                //System.out.println(WindowManager.getIDList());
                ImagePlus inputImage = null;
                int[] ids = WindowManager.getIDList();
                for (int i =0; i < ids.length; i++){
                    if (WindowManager.getImage(ids[i]).getTitle().contains("catchment-basins")) {
                        inputImage = WindowManager.getImage(ids[i]);
                    }
                    //System.out.println(WindowManager.getImage(ids[i]).getTitle());
                }

                CustomCanvas cc = new CustomCanvas(inputImage);
                assert inputImage != null;
                inputImage.setTitle(imp.getShortTitle() + " segm ch" + String.valueOf(Integer.parseInt(cbg.getSelectedCheckbox().getName()) + 1));
                resultWindows.add(new ResultWindow(imp.getShortTitle() + " segm ch" + String.valueOf(Integer.parseInt(cbg.getSelectedCheckbox().getName()) + 1),
                        inputImage, cc, myimp.iminfo, this.ic.getImage()));
                cc.requestFocus();
                //myimp.setImp(inputImage);

                morphologicalSegmentation = null;
                processingLabel.setText("MorfSegmentation: finished");
                buttonMorfSegmentation.setLabel("MorfSegmentation");
            }
        }

        if (b==cutButton){
            try {

                Rectangle r = this.getImagePlus().getRoi().getBounds();
                if (isStandardMode) {
                    myimp.setCuted(r);
                }
                else{
                    myimp.cutImportImage();
                }
            }
            catch (NullPointerException e1){

            }
        }

        //TODO dl9 neskolkih kanalov + obrabotka owibok
        if (b==buttonGetFinalResult){
            ResultWindow coresWindow = null, cellsWindow = null;
            int count = 0;
            if (this.image2DWind != null){
                resultWindows.add(this.image2DWind.getResultWindow());
            }
            for (ResultWindow resw: resultWindows){
                if ((resw != null)&&(resw.isShowing())){
                    count +=1;
                    if (resw.isCoresWindow()){
                        coresWindow = resw;
                    }
                    else{
                        cellsWindow = resw;
                    }
                }
            }
            if ((coresWindow != null)&&(cellsWindow != null)){
                processingLabel.setText("Processing...");
                CoresResultProcessor crp;
                ArrayList<ObjectInfo>  cellInfo;
                ArrayList<ObjectInfo> coreInfo;
                ImageStack CellImageStack = cellsWindow.getImagePlus().getImageStack();
                ImageStack CoreImageStack = coresWindow.getImagePlus().getImageStack();
                if (CellImageStack.getSize() == 1) {
                    int[][] image_mask = myimp.get2DImageMap();
                    if (image_mask == null) {
                        YesNoCancelDialog dialog = new YesNoCancelDialog(this, "Warning",
                                "Image mask for 2D image was not loaded. Do you want to do this?");
                        if (dialog.yesPressed()) {
                            OpenDialog od = new OpenDialog("Open image mask...");
                            String name = od.getFileName();
                            if (name == null)
                                return;
                            String dir = od.getDirectory();
                            String path = dir + name;
                            ImagePlus new_imp = new ImagePlus(path);
                            image_mask = new_imp.getProcessor().getIntArray();
                        }
                    }
                    int deep_value = Integer.parseInt(this.cellSizeField.getText());
                    crp = new CoresResultProcessor(CellImageStack.getProcessor(1), CoreImageStack, image_mask, deep_value);
                }
                else {
                    crp = new CoresResultProcessor(CellImageStack, CoreImageStack);
                }
                cellInfo = cellsWindow.getTableInfo();
                coreInfo = coresWindow.getTableInfo();
                HashMap<Float, HashMap<Float, Double>> info =  crp.getCoresInfo();
                CoresResultTable coresTable = new CoresResultTable(cellInfo, coreInfo, info);
                crp.getResultImage().show();
                processingLabel.setText("Done");
            }
            if (count != 2){
                new MessageDialog(this, "Error", "Opened windows:" + count + " (need 2)");
            }
            else{
                if (coresWindow == null){
                    new MessageDialog(this, "Error", "One of the windows must contain nucleus image");
                }
            }

        }

        //ImageCanvas ic = imp.getCanvas();
        //if (ic!=null)
        //    ic.requestFocus();
    }

    private void setViewMode(){
        //for (Checkbox ch: chs){
        //    ch.setEnabled(isStandardMode);
        //}
        //cbg.getSelectedCheckbox().setEnabled(isStandardMode);
        buttonApplyOffset.setEnabled(isStandardMode);
        //buttonNormalization.setEnabled(isStandardMode);
        cutButton.setEnabled(isStandardMode);
        curOffsetX.setEnabled(isStandardMode);
        curOffsetY.setEnabled(isStandardMode);
        curOffsetZ.setEnabled(isStandardMode);
        //normalizationParametr.setEnabled(isStandardMode);
    }

}

