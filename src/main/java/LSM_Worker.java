import LsmReader.CZLSMInfo;
import LsmReader.Reader;
import MainWindow.MyDialogWindow;
import MainWindow.MyLSMImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;


public class LSM_Worker implements PlugIn {

    public void run(final String arg){
        JFileChooser chooser = new JFileChooser(OpenDialog.getLastDirectory());
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileFilter(new FileNameExtensionFilter("LSM File(*.lsm)", "lsm"));
        int returnVal = chooser.showOpenDialog(null);

        if (returnVal != JFileChooser.APPROVE_OPTION)
            return;

        String filePath = chooser.getSelectedFile().getAbsolutePath();
        IJ.showStatus("Opening: " + filePath);
        Reader r = new Reader();
        ImagePlus imp = r.open(filePath);
        CZLSMInfo iminfo = r.getLsmInfo();

        MyDialogWindow d = new MyDialogWindow("LSM_Worker", iminfo);
        iminfo.RealDimensionY = d.getHeight();
        iminfo.RealDimensionX = d.getWidth();
        iminfo.Overlap = d.getOverlap();
        iminfo.isOptimized = d.getIsOptimized();
        int compression = d.getComression();

        MyLSMImage myimp = null;
        if (iminfo.isOptimized) {
            while (myimp == null) {
                try {
                    myimp = new MyLSMImage(imp, iminfo, compression);
                } catch (OutOfMemoryError e) {
                    //String freeMemory = IJ.freeMemory();
                    compression *= 2;
                }
            }
        }
        else{
            myimp = new MyLSMImage(imp, iminfo, compression);
        }

        MainWindow.CustomCanvas cc = new MainWindow.CustomCanvas(myimp.getImp());
        new MainWindow.MyWindow(myimp, cc);
        cc.requestFocus();

}

    private ImagePlus convertToFloatImage(ImagePlus img){
        ImageStack ims = new ImageStack(img.getWidth(), img.getHeight());
        for (int i=1; i <= img.getStack().getSize(); i++) {
            FloatProcessor flp = img.getStack().getProcessor(i).convertToFloatProcessor();
            ims.addSlice(flp);
        }
        return new ImagePlus("FloatImage", ims);
    }
}
