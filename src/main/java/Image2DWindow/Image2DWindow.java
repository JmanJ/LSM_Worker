package Image2DWindow;


import LsmReader.CZLSMInfo;
import MainWindow.Anisotropic_Diffusion_2D;
import MainWindow.CustomCanvas;
import MainWindow.Image2DProcessor;
import ResultWindow.ResultWindow;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import inra.ijpb.plugins.MorphologicalSegmentation;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class Image2DWindow extends ImageWindow implements ActionListener, ItemListener {

    private Button buttonMorfSegmentation, buttonSaveTiff, buttonLoadTiff, buttonDiffusion, buttonSmooth, buttonGet2DImage;
    private Label processingLabel, filterSizeLabel, timesLabel, diveLabel;
    private TextField filterSizeField, timesField, diveField;
    private Checkbox isMaskImage, isOriginalImage, isColoredImage;
    private CZLSMInfo info;
    protected MorphologicalSegmentation morphologicalSegmentation;
    protected ResultWindow resultWindow;
    protected Anisotropic_Diffusion_2D diffusion2D;
    protected Image2DProcessor im2dproc;

    public Image2DWindow(ImagePlus imp, CZLSMInfo info, Image2DProcessor im2dproc) {
        super(imp);
        this.info = info;
        this.im2dproc = im2dproc;
        addPanel();
    }

    public void addPanel(){
        Panel panel = new Panel();
        panel.setLayout(new BorderLayout());

        Panel changeImagePanel = new Panel();
        changeImagePanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        Panel optionPanel = new Panel();
        optionPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        CheckboxGroup cbg = new CheckboxGroup();
        isOriginalImage = new Checkbox("2D Surface", cbg, true);
        isOriginalImage.addItemListener(this);
        isMaskImage = new Checkbox("Image Mask", cbg, false);
        isMaskImage.addItemListener(this);
        isColoredImage = new Checkbox("Colored Image", cbg, false);
        isColoredImage.addItemListener(this);
        changeImagePanel.add(isOriginalImage);
        changeImagePanel.add(isMaskImage);
        changeImagePanel.add(isColoredImage);

        diveLabel = new Label("Diving value");
        changeImagePanel.add(diveLabel);
        diveField = new TextField("0");
        changeImagePanel.add(diveField);
        buttonGet2DImage = new Button("Update");
        buttonGet2DImage.addActionListener(this);
        changeImagePanel.add(buttonGet2DImage);

        buttonMorfSegmentation = new Button("MorfSegmentation");
        buttonMorfSegmentation.addActionListener(this);
        optionPanel.add(buttonMorfSegmentation);

        buttonDiffusion = new Button("Diffusion");
        buttonDiffusion.addActionListener(this);
        optionPanel.add(buttonDiffusion);
        buttonDiffusion.setEnabled(false);

        filterSizeLabel = new Label("Filter size (0, 1, 2):");
        optionPanel.add(filterSizeLabel, BorderLayout.SOUTH);
        filterSizeField = new TextField("1");
        optionPanel.add(filterSizeField, BorderLayout.SOUTH);
        timesLabel = new Label("Times:");
        optionPanel.add(timesLabel, BorderLayout.SOUTH);
        timesField = new TextField("5");
        optionPanel.add(timesField, BorderLayout.SOUTH);

        buttonSmooth = new Button("Smooth");
        buttonSmooth.addActionListener(this);
        optionPanel.add(buttonSmooth, BorderLayout.SOUTH);
        buttonSmooth.setEnabled(false);

        buttonSaveTiff = new Button("Save");
        buttonSaveTiff.addActionListener(this);
        optionPanel.add(buttonSaveTiff);

        buttonLoadTiff = new Button("Load");
        buttonLoadTiff.addActionListener(this);
        optionPanel.add(buttonLoadTiff);

        processingLabel = new Label("         ");

        panel.add(changeImagePanel, BorderLayout.NORTH);
        panel.add(optionPanel, BorderLayout.CENTER);
        panel.add(processingLabel, BorderLayout.SOUTH);

        add(panel, BorderLayout.CENTER);
        pack();
    }

    public void itemStateChanged(ItemEvent e)
    {
        updateImage();
    }

    private void updateImage() {
        if (isMaskImage.getState()){
            getImagePlus().setProcessor(im2dproc.getMaskProc());
            buttonSmooth.setEnabled(true);
            buttonMorfSegmentation.setEnabled(false);
        } else if (isColoredImage.getState()) {
            getImagePlus().setProcessor(im2dproc.getColored2dProc());
            buttonSmooth.setEnabled(false);
            buttonMorfSegmentation.setEnabled(false);
        } else if (isOriginalImage.getState()) {
            getImagePlus().setProcessor(im2dproc.getCur2DProc());
            buttonSmooth.setEnabled(false);
            buttonMorfSegmentation.setEnabled(true);
        }
        repaint();
        requestFocus();
    }

    public void actionPerformed(ActionEvent e) {

        Object b = e.getSource();

        /*
        if (b==buttonSaveJpg){
            SaveDialog openDialog = new SaveDialog("Save as Jpeg", imp.getShortTitle(), ".jpg");
            String directory = openDialog.getDirectory();
            String name = openDialog.getFileName();
            if (name == null) return;

            String path = directory + name;
            if (name.split(".").length == 1) {
                path += ".jpg";
            }
            (new FileSaver(imp)).saveAsJpeg(path);
        }
        */
        if (b==buttonGet2DImage) {
            im2dproc.setDiveValue(Integer.parseInt(diveField.getText()));
            updateImage();
        }
        if (b==buttonSaveTiff){
            (new FileSaver(im2dproc.getMaskImage())).saveAsTiff();
        }

        if (b==buttonLoadTiff) {
            OpenDialog od = new OpenDialog("Open image mask...");
            String name = od.getFileName();
            if (name==null)
                return;
            String dir = od.getDirectory();
            String path = dir + name;
            ImagePlus new_imp = new ImagePlus(path);
            im2dproc.setImageMask(new_imp.getProcessor());
            updateImage();
        }

        if (b==buttonSmooth) {
            processingLabel.setText("Processing...");
            im2dproc.smooth2DImage(Integer.parseInt(filterSizeField.getText()), Integer.parseInt(timesField.getText()));
            updateImage();
            processingLabel.setText("Done");
        }

        if (b==buttonDiffusion) {
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
                    setImage(inputImage);
                    //myimp.setImp(inputImage);
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
                    setImage(inputImage);
                    processingLabel.setText("Diffusion: finished");
                    buttonDiffusion.setLabel("Diffusion");
                    //diffusion2D.interrupt();
                    //diffusion2D = null;
                }
            }
        }

        if (b == buttonMorfSegmentation) {
            if (buttonMorfSegmentation.getLabel() != "Get result") {
                morphologicalSegmentation = new MorphologicalSegmentation();
                morphologicalSegmentation.run("");
                buttonMorfSegmentation.setLabel("Get result");
            }
            else{

                //System.out.println(WindowManager.getImageCount());
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
                inputImage.setTitle(imp.getShortTitle() + " segm ch 1");
                resultWindow = new ResultWindow(imp.getShortTitle() + " segm ch 1", inputImage, cc, info, new ImagePlus("2D orig image", im2dproc.getCur2DProc()));
                cc.requestFocus();
                //myimp.setImp(inputImage);
                morphologicalSegmentation = null;
                //inputImage.getWindow().close();
                buttonMorfSegmentation.setLabel("MorfSegmentation");
            }
        }
    }

    public ResultWindow getResultWindow(){
        return resultWindow;
    }
}
