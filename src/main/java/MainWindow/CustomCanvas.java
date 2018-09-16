package MainWindow;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.process.ImageProcessor;

import java.awt.*;
import java.awt.event.MouseEvent;


public class CustomCanvas extends ImageCanvas {

    private ImageListenerInterface listener;
    private boolean isConstructGraph;
    private PlotWindow plotWindow;
    private int maxDiff;

    public CustomCanvas(ImagePlus imp) {
        super(imp);
        listener = null;
        plotWindow = null;
        isConstructGraph = false;
    }

    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        if (cursorOverImage()) {
            Point clickPoint = getCursorLoc();
            Float color = getImage().getStack().getProcessor(getImage().getCurrentSlice()).getPixelValue(clickPoint.x, clickPoint.y);
            //System.out.println(color);
            if (listener != null){
                listener.imageClicked(color);
            }
            if (isConstructGraph){
                int x = clickPoint.x;
                int y = clickPoint.y;
                int size = getImage().getStack().getSize();
                /*
                File file = new File("data.txt");
                try {
                    PrintWriter out = new PrintWriter(file.getAbsoluteFile());
                    for (int x=0; x < getImage().getWidth(); x++){
                        for (int y=0; y < getImage().getHeight(); y++){
                            //int[] pixValues = new int[size];
                            for (int i=1; i <= size; i++){
                                out.print( (getImage().getStack().getProcessor(i).getPixel(x, y)) + " ");
                            }
                            out.print('\n');
                        }
                        System.out.println(x);
                    }
                    out.close();
                }
                catch(IOException ee) {
                    throw new RuntimeException(ee);
                }
                */
                float[] pixValues = new float[size];
                float[] pixIndexes = new float[size];
                float curMax, max = 0;
                for (int i=1; i <= size; i++){
                    pixValues[i-1] = (getImage().getStack().getProcessor(i).getPixelValue(x, y));
                    pixIndexes[i-1] = ((float) i);
                    curMax = (float) getImage().getStack().getProcessor(i).getMax();
                    if (curMax > max) max = curMax;
                }
                if (plotWindow != null)
                    plotWindow.close();
                Plot plot = new Plot("pix " + x + ":"+ y, "Frame", "Intensity", pixIndexes, pixValues);
                //plot.setLimits(1, getImage().getStack().getSize(), 0, max*8);
                plot.draw();
                int z = getMaxPoint(getImage().getStack(), x, y);
                plot.setColor(new Color(255, 0, 0));
                plot.drawLine(z, pixValues[z-1] - 50, z, pixValues[z-1] + 50);
                plotWindow = plot.show();
            }
        }
    }

    private int getMaxPoint(ImageStack imageStack, int x, int y){
        int size = imageStack.getSize();
        int diff;
        int maxInPoint = 0;
        int maxZ = 0;
        int maxDiff = this.maxDiff;
        boolean isFinalMax = false;
        for (int i=1; i <= size; i++){
            ImageProcessor curProc = imageStack.getProcessor(i);
            if (!isFinalMax) {
                if (i == 1) {
                    maxInPoint = curProc.get(x, y);
                    isFinalMax = false;
                    maxZ = i;
                } else {
                    if ((curProc.get(x, y) > maxInPoint) && (!isFinalMax)) {
                        maxInPoint = curProc.get(x, y);
                        maxZ = i;
                        if (i != imageStack.getSize()) {
                            diff = imageStack.getProcessor(i + 1).get(x, y) - imageStack.getProcessor(i).get(x, y);
                            if (diff > maxDiff) {
                                isFinalMax = true;
                                int curZ = i + 1;
                                while (curZ != imageStack.getSize()) {

                                    if (imageStack.getProcessor(curZ + 1).get(x, y) < imageStack.getProcessor(curZ).get(x, y)){
                                        maxInPoint = imageStack.getProcessor(curZ).get(x, y);
                                        maxZ = curZ;
                                        break;
                                    }
                                    curZ++;
                                }
                            }
                        }
                    }
                }
            }

        }
        return maxZ;
    }

    public void addListener(ImageListenerInterface listener){
        this.listener = listener;
    }

    public boolean isConstructGraph() {

        return isConstructGraph;
    }

    public void setIsConstructGraph(boolean isConstructGraph, int maxDiff) {
        this.maxDiff = maxDiff;
        this.isConstructGraph = isConstructGraph;
    }
}
