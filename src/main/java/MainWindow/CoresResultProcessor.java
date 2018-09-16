package MainWindow;


import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.HashMap;
import java.util.HashSet;

public class CoresResultProcessor {

    private ImageStack borderImageStack;
    private ImageStack coreImageStack;
    //private ByteProcessor pixelMatrix;
    private Float curColor;
    private ImageProcessor curProc;
    private int curSlice;
    private Float cellColor;
    private boolean isOk;
    private HashSet<Float> pickedColoros;
    private int pixelsIn;
    private int pixelsOut;
    private int h;
    private int w;
    private int deep;
    HashMap<Float, HashMap<Float, Double>> info;

    CoresResultProcessor(ImageStack borderImageStack, ImageStack coreImageStack){
        this.borderImageStack = borderImageStack;
        this.coreImageStack = coreImageStack;
        w = coreImageStack.getWidth();
        h = coreImageStack.getHeight();
    }

    CoresResultProcessor(ImageProcessor borderImage, ImageStack coreImageStack, int[][] imageMap, int deep_value){
        this.coreImageStack = coreImageStack;
        w = coreImageStack.getWidth();
        h = coreImageStack.getHeight();
        this.deep = deep_value;
        this.borderImageStack = this.convert2Dto3D(borderImage, imageMap);
    }

    private ImageStack convert2Dto3D(ImageProcessor imsProc, int[][] imageMap){
        ImageStack newImStack = this.coreImageStack.duplicate();
        Float zero = Float.parseFloat("0.0");
        FloatProcessor proc;
        boolean is_empty_mask;
        is_empty_mask = imageMap == null;
        if (is_empty_mask)
            imageMap = new int[w][h];
        for (int i = 1; i <= newImStack.getSize(); i++) {
            proc = (FloatProcessor)newImStack.getProcessor(i);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    proc.setf(x, y, zero);
                    if (is_empty_mask)
                        imageMap[x][y] = 1;
                }
            }
        }

        int curZ, curDeep;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                curZ = imageMap[x][y];
                curDeep = 0;
                while (curDeep < this.deep) {
                    try {
                        newImStack.getProcessor(curZ).set(x, y, imsProc.get(x, y));
                        curZ++;
                        curDeep++;
                    } catch (IllegalArgumentException e){
                        curDeep = this.deep;
                    }
                }

            }
        }
        new ImagePlus("Test: converted 2D -> 3D", newImStack).show();
        return newImStack;
    }

    public ImagePlus getResultImage(){
        ImageStack newImStack = this.borderImageStack.duplicate();
        Float zero = Float.parseFloat("0.0");
        Float curColor;
        FloatProcessor proc;
        for (int i = 1; i <= newImStack.getSize(); i++) {
            proc = (FloatProcessor)newImStack.getProcessor(i);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    curColor = Float.intBitsToFloat(proc.get(x, y));
                    if (!this.info.containsKey(curColor))
                        proc.setf(x, y, zero);
                }
            }
        }

        HashSet<Float> coresColors = new HashSet<Float>();
        for (HashMap<Float, Double> value: this.info.values()) {
            for (Float color: value.keySet()) {
                coresColors.add(color);
            }
        }
        System.out.println(coresColors);

        Float coreColor = Float.parseFloat("30.0");
        FloatProcessor coreProc, cellProc;
        Float curCoreColor, curCellColor;
        for (int i = 1; i <= coreImageStack.getSize(); i++) {
            coreProc = (FloatProcessor)coreImageStack.getProcessor(i);
            cellProc = (FloatProcessor)borderImageStack.getProcessor(i);
            proc = (FloatProcessor)newImStack.getProcessor(i);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    curCoreColor = Float.intBitsToFloat(coreProc.get(x, y));
                    curCellColor = Float.intBitsToFloat(cellProc.get(x, y));
                    if (coresColors.contains(curCoreColor)){
                        if (this.isNotBlack(curCellColor)){
                            proc.setf(x, y, coreColor);
                        }
                    }

                }
            }
        }
        return new ImagePlus("Result image", newImStack);
    }

    public HashMap<Float, HashMap<Float, Double>> getCoresInfo(){
        // [???? ???? : [???? ?????? : ???. ???????? ; -1.0 : ???. ???????? ???? ?????]]
        HashMap<Float, HashMap<Float, Integer>> info = new HashMap<Float, HashMap<Float, Integer>>();
        //pixelMatrix = new ByteProcessor(w, h);
        Float curIndex;
        boolean stackCrached;

        for (int i = 1; i <= coreImageStack.getSize(); i++) {
            curProc = coreImageStack.getProcessor(i);
            curSlice = i;
            pickedColoros = new HashSet<Float>();
            for (int y = 1; y < h - 1; y++) {
                for (int x = 1; x < w - 1; x++) {
                    curIndex = Float.intBitsToFloat(curProc.get(x, y));
                    if (isNotBlack(curIndex)) {
                        curColor = curIndex;
                        cellColor = coreInCell(x, y);
                        pixelsIn = 0;
                        pixelsOut = 0;
                        if (cellColor > 0) {
                            if (info.containsKey(curColor)){
                                HashMap<Float, Integer> colors = info.get(curColor);
                                if (colors.containsKey(cellColor)){
                                    colors.put(cellColor, colors.get(cellColor) + 1);
                                }
                                else{
                                    colors.put(cellColor, 1);
                                }
                                colors.put(Float.parseFloat("-1.0"), colors.get(Float.parseFloat("-1.0")) + 1);
                                info.put(curColor, colors);
                            }
                            else{
                                HashMap<Float, Integer> c = new HashMap<Float, Integer>();
                                c.put(cellColor, 1);
                                c.put(Float.parseFloat("-1.0"), 1);
                                info.put(curColor, c);
                            }

                        }
                    }

                }
            }
        }

        //System.out.println(info);
        HashMap<Float, HashMap<Float, Double>> result = new HashMap<Float, HashMap<Float, Double>>();
        int coreSize, cellPart;
        Double probability;
        Float cellColor;
        for(HashMap.Entry<Float, HashMap<Float, Integer>> entry : info.entrySet()) {
            Float coreColor = entry.getKey();
            HashMap<Float, Integer> cellColors = entry.getValue();
            //????? ?????? ?????? ? ????? ????? (?????? ???????? ???. ???????? ??? ???? ?????? ? ???. ???????? ????)
            if (cellColors.size() == 2){
                coreSize = cellColors.remove(Float.parseFloat("-1.0"));
                cellColor = (Float) cellColors.keySet().toArray()[0];
                cellPart = cellColors.get(cellColor);
                probability = (cellPart * 1.0) / coreSize;
                if (result.containsKey(cellColor)) {
                    HashMap<Float, Double> c = result.get(cellColor);
                    c.put(coreColor, probability);
                    result.put(cellColor, c);
                }
                else{
                    HashMap<Float, Double> c = new HashMap<Float, Double>();
                    c.put(coreColor, probability);
                    result.put(cellColor, c);
                }
            }
            else{
                System.out.println("Detected bad nuclei " + String.valueOf(coreColor));
            }
        }

        this.info = result;
        //System.out.println(result);
        return result;
    }

    private boolean isNotBlack(Float p) {
        //point it is no border or background  (1 or 0)
        if ((p != Float.parseFloat("1.0")) && (p != Float.parseFloat("0.0"))) {
            return true;
        } else {
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

    private Float coreInCell(int x, int y){
        Float cellColor = Float.intBitsToFloat(borderImageStack.getProcessor(curSlice).getPixel(x, y));
        if (isNotBlack(cellColor))
            return cellColor;
        else
            return Float.parseFloat("0.0");
    }

}
