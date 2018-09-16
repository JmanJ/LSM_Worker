package ResultWindow;

import LsmReader.CZLSMInfo;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


public class ResultImageProcessor {

    protected int DEFAULT_PIXEL_VALUE = 255;
    protected int badPixelsGroup = 5;

    private ImageProcessor pixelMatrix;
    private int[] markerColor;
    private Map<Float, Double> PixelsSize;
    private float markerColorFloat;
    private int marker;
    private int markerPixelsCount;
    private ImageProcessor impr;
    private CZLSMInfo info;
    private ImagePlus img;


    public ResultImageProcessor(ImagePlus img, CZLSMInfo info){
        this.img = img;
        this.info = info;
    }

    public Map<Float, Double> calculatePixelsSize(){
        PixelsSize = new HashMap<Float, Double>();
        for (int i=1; i <= img.getStack().getSize(); i++){
            //FloatProcessor?
            calculateRelatedFloatPixels((FloatProcessor) img.getStack().getProcessor(i));
        }
        return PixelsSize;
    }

    public HashSet<Float> getBoundaryColors(ImageStack imStack){
        int w = imStack.getWidth();
        int h = imStack.getHeight();
        HashSet<Float> boundaryColor = new HashSet<Float>();
        for (int i=1; i <= imStack.getSize(); i++) {
            FloatProcessor fp = (FloatProcessor) imStack.getProcessor(i);
            for (int x=0, y=0; x < w; x++){
                boundaryColor.add(Float.intBitsToFloat(fp.getPixel(x, y)));
            }
            for (int x=0, y=h-1; x < w; x++){
                boundaryColor.add(Float.intBitsToFloat(fp.getPixel(x, y)));
            }
            for (int y=0, x=0; y < h; y++){
                boundaryColor.add(Float.intBitsToFloat(fp.getPixel(x, y)));
            }
            for (int y=0, x=w-1; y < h; y++){
                boundaryColor.add(Float.intBitsToFloat(fp.getPixel(x, y)));
            }
        }
        //System.out.println(boundaryColor);
        return boundaryColor;
    }

    public Float getAllocationColor(){
        Float maxSize = Float.MAX_VALUE;
        for (Float size: PixelsSize.keySet()){
            if (size > maxSize){
                maxSize = size;
            }
        }
        return (maxSize + maxSize/2);
    }

    public void setBadPixelsGroup(int badPixelsGroup){
        this.badPixelsGroup = badPixelsGroup;
    }

    //----------------------------------------------------------------------------------------!
    //float realization

    private void calculateRelatedFloatPixels(FloatProcessor improc){
        this.impr = improc;
        int w = impr.getWidth();
        int h = impr.getHeight();
        pixelMatrix = new ByteProcessor(w, h);
        marker = DEFAULT_PIXEL_VALUE;
        int badMarkedRegions = 0;
        int markerIndex = 0;
        boolean stackCrached;
        double voxelSize = info.VoxelSizeX * info.VoxelSizeY * info.VoxelSizeZ;
        for (int st=0; st<18; st++){
            voxelSize *= 10;
        }
        for (int y=0; y < h; y++){
            for (int x=0; x < w; x++){
                if ((isNotBlack(x, y))&&(pixelMatrix.getPixel(x, y) == 0)&&(haveSomeFloatNeighbor(x, y))){
                    markerColorFloat = impr.getPixelValue(x, y);
                    markerPixelsCount = 0;
                    stackCrached = true;
                    while (stackCrached) {
                        try {
                            markFloatPixel(x, y);
                            stackCrached = false;
                        } catch (StackOverflowError e1) {
                            stackCrached = true;
                            //markPixel(x, y);
                            //throw e1;
                        }
                    }

                    if (markerPixelsCount > badPixelsGroup) {
                        //esli net v spiske to sozdat'
                        if (PixelsSize.get(markerColorFloat) == null) {
                            PixelsSize.put(markerColorFloat, markerPixelsCount * voxelSize);
                        } else {
                            Double newValue = PixelsSize.get(markerColorFloat) + (markerPixelsCount * voxelSize);
                            PixelsSize.put(markerColorFloat, newValue);
                        }
                    }
                    else{
                        badMarkedRegions++;
                    }
                    markerIndex++;
                }
                //pixelMatrix.putPixel(x, y, DEFAULT_PIXEL_VALUE);
            }
        }
        //System.out.println("Finished. Bad regions (pixels <" + badPixelsGroup +"): " + badMarkedRegions);
        //new ImagePlus("Market image", pixelMatrix).show();
    }

    private void markFloatPixel(int x, int y){
            if ((pixelMatrix.getPixel(x, y) == 0) && (compareFloatPixels(markerColorFloat, impr.getPixelValue(x, y)))) {
                pixelMatrix.putPixel(x, y, marker);
                ++markerPixelsCount;
                markFloatPixel(x + 1, y);
                markFloatPixel(x - 1, y);
                markFloatPixel(x, y + 1);
                markFloatPixel(x, y - 1);
                markFloatPixel(x + 1, y - 1);
                markFloatPixel(x + 1, y + 1);
                markFloatPixel(x - 1, y - 1);
                markFloatPixel(x + 1, y + 1);
            }
    }

    private boolean haveSomeFloatNeighbor(int x, int y){
        float p = impr.getPixelValue(x, y);
        //System.out.println(p[0] + ' ' + p[1] + ' ' + p[2]);
        if ((compareFloatPixels(impr.getPixelValue(x + 1, y), p))||
                (compareFloatPixels(impr.getPixelValue(x, y - 1), p))||
                (compareFloatPixels(impr.getPixelValue(x + 1, y + 1), p))||
                (compareFloatPixels(impr.getPixelValue(x + 1, y - 1), p))||
                (compareFloatPixels(impr.getPixelValue(x - 1, y), p))||
                (compareFloatPixels(impr.getPixelValue(x - 1, y + 1), p))||
                (compareFloatPixels(impr.getPixelValue(x - 1, y - 1), p))||
                (compareFloatPixels(impr.getPixelValue(x, y + 1), p))){
            //System.out.println("Some pixels founded!");
            //System.out.println(p);
            return true;
        }
        return false;

    }

    private boolean isNotBlack(int x, int y){
        int p = (int) impr.getPixelValue(x, y);
        //System.out.println(p);

        //point it is no border or background  (1 or 0)
        if ((p != 1)&&(p != 0)){
            return true;
        }
        else{
            return false;
        }
    }

    private boolean compareFloatPixels(float pix1, float pix2){
        if (pix1 == pix2){
            return true;
        }
        else{
            return false;
        }
    }

    //-----------------------------------------------------------------------------------------

    public void calculateRelatedPixels(ColorProcessor improc){
        this.impr = improc;
        int w = impr.getWidth();
        int h = impr.getHeight();
        pixelMatrix = new ByteProcessor(w, h);
        marker = DEFAULT_PIXEL_VALUE;
        for (int y=0; y < h; y++){
            for (int x=0; x < w; x++){
                if ((isNotBlackAndDifferent(x, y))&&(pixelMatrix.getPixel(x, y) == 0)&&(haveSomeNeighbor(x, y))){
                    markerColor = impr.getPixel(x, y, null);
                    markerPixelsCount = 0;
                    markPixel(x, y);
                    System.out.println("Some Pixels count: " + markerPixelsCount);
                }
                //pixelMatrix.putPixel(x, y, DEFAULT_PIXEL_VALUE);
            }
        }
        new ImagePlus("Market image", pixelMatrix).show();
    }

    private void markPixel(int x, int y){
        if ((pixelMatrix.getPixel(x, y) == 0)&&(comparePixels(markerColor, impr.getPixel(x, y, null)))){
            pixelMatrix.putPixel(x, y, marker);
            ++markerPixelsCount;
            markPixel(x+1, y);
            markPixel(x-1, y);
            markPixel(x, y+1);
            markPixel(x, y-1);
            markPixel(x+1, y-1);
            markPixel(x+1, y+1);
            markPixel(x-1, y-1);
            markPixel(x+1, y+1);
        }
    }

    private boolean haveSomeNeighbor(int x, int y){
        int[] p = impr.getPixel(x, y, null);
        //System.out.println(p[0] + ' ' + p[1] + ' ' + p[2]);
        if ((comparePixels(impr.getPixel(x+1, y, null), p))||
                (comparePixels(impr.getPixel(x, y-1, null), p))||
                (comparePixels(impr.getPixel(x+1, y+1, null), p))||
                (comparePixels(impr.getPixel(x+1, y-1, null), p))||
                (comparePixels(impr.getPixel(x-1, y, null), p))||
                (comparePixels(impr.getPixel(x-1, y+1, null), p))||
                (comparePixels(impr.getPixel(x-1, y-1, null), p))||
                (comparePixels(impr.getPixel(x, y+1, null), p))){
            System.out.println("Some pixels founded!");
            System.out.println(p[0] + " : " + p[1] + " : " + p[2]);
            return true;
        }
        return false;

    }

    private boolean isNotBlackAndDifferent(int x, int y){
        int[] p = impr.getPixel(x, y, null);
        if ((p[0] + p[1] + p[2] != 0)&&(p[0] != p[1])&&(p[1] != p[2])){
            return true;
        }
        else{
            return false;
        }
    }

    private boolean comparePixels(int[] pix1, int[] pix2){
        if ((pix1[0] == pix2[0])&&(pix1[1] == pix2[1])&&(pix1[2] == pix2[2])){
            return true;
        }
        else{
            return false;
        }
    }
}
