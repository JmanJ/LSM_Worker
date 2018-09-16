package MainWindow;

import LsmReader.CZLSMInfo;
import ij.IJ;
import ij.gui.GenericDialog;

import static java.lang.System.exit;

public class MyDialogWindow {

    static String title = "The number of parts in the layer";
    static int width = 0, height = 0, overlap = 0, compression = 1;
    static boolean isOptimized;

    public MyDialogWindow(String arg, CZLSMInfo info) {
        //super();
        GenericDialog gd = new GenericDialog(arg);
        gd.addMessage(title);
        width = (int) info.DimensionM;
        height = (int) info.DimensionP;
        //Runtime runtime = Runtime.getRuntime();
        //long allocatedMemory = runtime.totalMemory();
        String freeMemory = IJ.freeMemory();
        gd.addNumericField("X: ", width, 0);
        gd.addNumericField("Y: ", height, 0);
        gd.addNumericField("Overlap: ", overlap, 0);
        gd.addMessage("Free memory: " + freeMemory);
        //gd.addMessage("Allocated memory: " + String.valueOf(allocatedMemory));
        gd.addCheckbox("Memory optimization", false);
        gd.addNumericField("Compression ratio: ", compression, 0);
        gd.showDialog();
        if (gd.wasCanceled()) exit(0);
        width = (int) gd.getNextNumber();
        height = (int) gd.getNextNumber();
        overlap = (int) gd.getNextNumber();
        isOptimized = gd.getNextBoolean();
        compression = (int) gd.getNextNumber();
    }

    public int getWidth(){
        return width;
    }
    public int getHeight(){
        return height;
    }
    public int getOverlap(){
        return overlap;
    }
    public boolean getIsOptimized(){
        return isOptimized;
    }
    public int getComression(){
        return compression;
    }
}