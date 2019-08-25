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
    private Checkbox isMaskImage;
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
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));

        isMaskImage = new Checkbox("Image Mask");
        isMaskImage.setState(false);
        isMaskImage.addItemListener(this);
        panel.add(isMaskImage);

        diveLabel = new Label("Diving value");
        panel.add(diveLabel, BorderLayout.SOUTH);
        diveField = new TextField("0");
        panel.add(diveField, BorderLayout.SOUTH);
        buttonGet2DImage = new Button("Update");
        buttonGet2DImage.addActionListener(this);
        panel.add(buttonGet2DImage);

        buttonMorfSegmentation = new Button("MorfSegmentation");
        buttonMorfSegmentation.addActionListener(this);
        panel.add(buttonMorfSegmentation);

        buttonDiffusion = new Button("Diffusion");
        buttonDiffusion.addActionListener(this);
        panel.add(buttonDiffusion);
        buttonDiffusion.setEnabled(false);

        filterSizeLabel = new Label("Filter size (0, 1, 2):");
        panel.add(filterSizeLabel, BorderLayout.SOUTH);
        filterSizeField = new TextField("1");
        panel.add(filterSizeField, BorderLayout.SOUTH);
        timesLabel = new Label("Times:");
        panel.add(timesLabel, BorderLayout.SOUTH);
        timesField = new TextField("5");
        panel.add(timesField, BorderLayout.SOUTH);

        buttonSmooth = new Button("Smooth");
        buttonSmooth.addActionListener(this);
        panel.add(buttonSmooth, BorderLayout.SOUTH);
        buttonSmooth.setEnabled(false);

        buttonSaveTiff = new Button("Save");
        buttonSaveTiff.addActionListener(this);
        panel.add(buttonSaveTiff);

        buttonLoadTiff = new Button("Load");
        buttonLoadTiff.addActionListener(this);
        panel.add(buttonLoadTiff);

        processingLabel = new Label("         ");
        panel.add(processingLabel);

        add(panel, BorderLayout.CENTER);
        pack();
    }

    public void itemStateChanged(ItemEvent e)
    {
        if (isMaskImage.getState()){
            getImagePlus().setProcessor(im2dproc.getMaskProc());
            buttonSmooth.setEnabled(true);
        }
        else{
            im2dproc.setImageMask(getImagePlus().getProcessor());
            getImagePlus().setProcessor(im2dproc.getCur2DProc());
            buttonSmooth.setEnabled(false);
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
            if (!isMaskImage.getState()){
                getImagePlus().setProcessor(im2dproc.getCur2DProc());
                repaint();
            }
        }
        if (b==buttonSaveTiff){
            //FileInfo fi = myimp.getOriginalImage().getFileInfo();
            //imp.setProperty("jmanj", "haha");
            //imp.setCalibration(myimp.getOriginalImage().getCalibration());
            //imp.setTitle(imp.getShortTitle() + " ch" + cbg.getSelectedCheckbox().getName());
            //imp.setFileInfo(fi);
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
            if (isMaskImage.getState()) {
                getImagePlus().setProcessor(im2dproc.getMaskProc());
            }
            else{
                getImagePlus().setProcessor(im2dproc.getCur2DProc());
            }
            repaint();
            requestFocus();
        }

        if (b==buttonSmooth) {
            processingLabel.setText("Processing...");
            im2dproc.smooth2DImage(Integer.parseInt(filterSizeField.getText()), Integer.parseInt(timesField.getText()));
            if (isMaskImage.getState()){
                getImagePlus().setProcessor(im2dproc.getMaskProc());
            }
            else{
                im2dproc.setImageMask(getImagePlus().getProcessor());
                getImagePlus().setProcessor(im2dproc.getCur2DProc());
            }
            repaint();
            requestFocus();
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
