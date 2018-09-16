package ResultWindow;


import LsmReader.CZLSMInfo;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class TableInteractionProcessor {
    private int w;
    private int h;
    private boolean isBorder;
    private double EPS = 0.01;
    protected int DEFAULT_PIXEL_VALUE = 255;
    protected int badPixelsGroup = 5;

    private ByteProcessor pixelMatrix;
    private FloatProcessor curCoresProc;
    private FloatProcessor previousCoresProc;
    private FloatProcessor curCellsProc;
    private int stackSize;
    protected final ImageStack originalImageStackCopy;
    protected ImageStack savedImageStackCopy;
    protected ImageStack currentImageStackCopy;
    protected HashSet<Float> cutOutColor;
    protected HashSet<Float> selectedColors;
    protected HashSet<Float> curCoresGroup;

    private HashMap<Float, Double> PixelsSize;
    private float markerColorFloat;
    private int marker;
    private int markerPixelsCount;

    private CZLSMInfo info;
    private HashMap<Float, Integer> groups;
    private int nextGroupIndex = 1;

    TableInteractionProcessor(ImagePlus Imp, CZLSMInfo info) {
        originalImageStackCopy = Imp.getImageStack();
        savedImageStackCopy = originalImageStackCopy.duplicate();
        currentImageStackCopy = savedImageStackCopy.duplicate();
        w = savedImageStackCopy.getWidth();
        h = savedImageStackCopy.getHeight();
        cutOutColor = new HashSet<Float>();
        groups = new HashMap<Float, Integer>();
        this.info = info;
    }

    public ImageStack deleteBoundaryColors(){
        ImageStack imStack = currentImageStackCopy;
        int w = imStack.getWidth();
        int h = imStack.getHeight();
        for (int i=1; i <= imStack.getSize(); i++) {
            FloatProcessor fp = (FloatProcessor) imStack.getProcessor(i);
            for (int x=0, y=0; x < w; x++){
                cutOutColor.add(Float.intBitsToFloat(fp.getPixel(x, y)));
            }
            for (int x=0, y=h-1; x < w; x++){
                cutOutColor.add(Float.intBitsToFloat(fp.getPixel(x, y)));
            }
            for (int y=0, x=0; y < h; y++){
                cutOutColor.add(Float.intBitsToFloat(fp.getPixel(x, y)));
            }
            for (int y=0, x=w-1; y < h; y++){
                cutOutColor.add(Float.intBitsToFloat(fp.getPixel(x, y)));
            }
        }

        if (cutOutColor.contains(Float.parseFloat("0.0"))) {
            cutOutColor.remove(Float.parseFloat("0.0"));
        }
        if (cutOutColor.contains(Float.parseFloat("1.0"))) {
            cutOutColor.remove(Float.parseFloat("1.0"));
        }

        return this.turnOff(Float.valueOf("-1"));
    }

    /*
    public Float getAllocationColor(){
        Float maxSize = Float.MAX_VALUE;
        for (Float size: PixelsSize.keySet()){
            if (size > maxSize){
                maxSize = size;
            }
        }
        return (maxSize + maxSize/2);
    }*/

    public void setBadPixelsGroup(int badPixelsGroup){
        this.badPixelsGroup = badPixelsGroup;
    }

    public ImageStack calculatePixelsSize(){
        PixelsSize = new HashMap<Float, Double>();
        ImageStack resultImp = currentImageStackCopy;
        double voxelSize;
        if (resultImp.getSize() == 1)
            voxelSize = info.VoxelSizeX * 1000000 * info.VoxelSizeY * 1000000;
        else
            voxelSize = info.VoxelSizeX * 1000000 * info.VoxelSizeY * 1000000 * info.VoxelSizeZ * 1000000;
        //for (int st=0; st<18; st++){
        //    voxelSize *= 10;
        //}
        for (int i = 1; i <= resultImp.getSize(); i++) {
            curCellsProc = (FloatProcessor) resultImp.getProcessor(i);
            pixelMatrix = new ByteProcessor(w, h);
            marker = DEFAULT_PIXEL_VALUE;
            int markerIndex = 0;
            for (int y=0; y < h; y++){
                for (int x=0; x < w; x++){
                    if ((isNotBlack(x, y))&&(pixelMatrix.getPixel(x, y) == 0)){
                        markerColorFloat = curCellsProc.getPixelValue(x, y);
                        markerPixelsCount = 0;
                        try{
                            markFloatPixel(x, y);
                        }
                        catch (StackOverflowError e1){
                            //System.out.println("StackOverflowError! Color: " + markerColorFloat);
                            //throw e1;
                        }

                        //esli net v spiske to sozdat'
                        if (PixelsSize.get(markerColorFloat) == null) {
                            PixelsSize.put(markerColorFloat, markerPixelsCount * voxelSize);
                        } else {
                            Double newValue = PixelsSize.get(markerColorFloat) + (markerPixelsCount * voxelSize);
                            PixelsSize.put(markerColorFloat, newValue);
                        }
                        markerIndex++;
                    }
                    //pixelMatrix.putPixel(x, y, DEFAULT_PIXEL_VALUE);
                }
            }
        }
        //delete bad regions
        int badMarkedRegions = 0;
        ArrayList<Float> colorForDeleting = new ArrayList<Float>();
        for (Float color: PixelsSize.keySet()){
            Double pixelsCount = PixelsSize.get(color)/voxelSize;
            if (pixelsCount.intValue() <= badPixelsGroup){
                badMarkedRegions++;
                cutOutColor.add(color);
                colorForDeleting.add(color);
            }
        }
        for (Float color: colorForDeleting){
            PixelsSize.remove(color);
        }
        System.out.println("Bad regions: " + badMarkedRegions);

        new ImagePlus("test image", pixelMatrix).show();

        return this.turnOff(Float.valueOf("-1"));
    }

    public HashMap<Float, Double> getPixelsInfo(){
        return PixelsSize;
    }

    private void markFloatPixel(int x, int y){
        if ((pixelMatrix.getPixel(x, y) == 0) && (compareFloatPixels(markerColorFloat, curCellsProc.getPixelValue(x, y)))) {
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
        float p = curCellsProc.getPixelValue(x, y);
        //System.out.println(p[0] + ' ' + p[1] + ' ' + p[2]);
        if ((compareFloatPixels(curCellsProc.getPixelValue(x + 1, y), p))||
                (compareFloatPixels(curCellsProc.getPixelValue(x, y - 1), p))||
                (compareFloatPixels(curCellsProc.getPixelValue(x + 1, y + 1), p))||
                (compareFloatPixels(curCellsProc.getPixelValue(x + 1, y - 1), p))||
                (compareFloatPixels(curCellsProc.getPixelValue(x - 1, y), p))||
                (compareFloatPixels(curCellsProc.getPixelValue(x - 1, y + 1), p))||
                (compareFloatPixels(curCellsProc.getPixelValue(x - 1, y - 1), p))||
                (compareFloatPixels(curCellsProc.getPixelValue(x, y + 1), p))){
            //System.out.println("Some pixels founded!");
            //System.out.println(p);
            return true;
        }
        return false;

    }

    private boolean isNotBlack(int x, int y){
        int p = (int) curCellsProc.getPixelValue(x, y);
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

    public ImageStack turnOff(Float index) {
        if (index > 1.0)
            cutOutColor.add(index);
        ImageStack resultImp = currentImageStackCopy;
        for (int i = 1; i <= resultImp.getSize(); i++) {
            FloatProcessor fp = (FloatProcessor) resultImp.getProcessor(i);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Float curIndex = Float.intBitsToFloat(fp.get(x, y));
                    //System.out.println("Comparison: " + index + " : " + curIndex);
                    if (cutOutColor.contains(curIndex)) {
                        fp.putPixelValue(x, y, 1.0);
                    }
                }
            }
        }
        if (index < 0.0){
            saveChanges();
        }
        return resultImp;
    }

    public ImageStack turnOn(Float index) {
        cutOutColor.remove(index);
        ImageStack resultImp = savedImageStackCopy.duplicate();
        for (int i = 1; i <= resultImp.getSize(); i++) {
            FloatProcessor fp = (FloatProcessor) resultImp.getProcessor(i);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Float curIndex = Float.intBitsToFloat(fp.get(x, y));
                    if (cutOutColor.contains(curIndex)) {
                        //FloatProcessor fpr = (FloatProcessor) resultImp.getProcessor(i);
                        fp.putPixelValue(x, y, 1.0);
                    }
                }
            }
        }
        //System.out.println(cutOutColor);
        currentImageStackCopy = resultImp;
        return resultImp;
    }

    public ImageStack selectCell(Float index, Float color) {
        ImageStack resultImp = currentImageStackCopy.duplicate();
        for (int i = 1; i <= resultImp.getSize(); i++) {
            FloatProcessor fp = (FloatProcessor) resultImp.getProcessor(i);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Float curIndex = Float.intBitsToFloat(fp.get(x, y));
                    if (Math.abs(index - curIndex) < EPS) {
                        fp.putPixelValue(x, y, color);
                    }
                    //viklu4it' vse 4to nyjno
                    if (cutOutColor.contains(curIndex)) {
                        fp.putPixelValue(x, y, 1.0);
                    }
                }
            }
        }
        return resultImp;
    }

    public ImageStack combineCells(HashSet<Float> selColors, int nSlice) {
        if (selColors.contains(Float.parseFloat("0.0"))){
            selColors.remove(Float.parseFloat("0.0"));
        }
        if (selColors.contains(Float.parseFloat("1.0"))){
            selColors.remove(Float.parseFloat("1.0"));
        }
        ImageStack resultImp = currentImageStackCopy;
        if (selColors.size() < 2){
            return resultImp;
        }
        int n;
        if (nSlice > 0){
            n = nSlice;
        }
        else{
            n = resultImp.getSize();
        }
        this.selectedColors = selColors;
        Float marker = (Float) selectedColors.toArray()[0];
        for (int i = 1; i <= n; i++) {
            FloatProcessor fp = (FloatProcessor) resultImp.getProcessor(i);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Float curIndex = Float.intBitsToFloat(fp.get(x, y));
                    if (cutOutColor.contains(curIndex)) {
                        fp.putPixelValue(x, y, 1.0);
                    }
                    if (selectedColors.contains(curIndex)){
                        fp.putPixelValue(x, y, marker);
                    }
                    if (curIndex == 0.0){
                        isBorder = false;
                        if ((checkPixel(Float.intBitsToFloat(fp.getPixel(x + 1, y))))&&
                                (checkPixel(Float.intBitsToFloat(fp.getPixel(x - 1, y))))&&
                                (checkPixel(Float.intBitsToFloat(fp.getPixel(x, y + 1))))&&
                                (checkPixel(Float.intBitsToFloat(fp.getPixel(x, y - 1))))&&
                                (checkPixel(Float.intBitsToFloat(fp.getPixel(x + 1, y - 1))))&&
                                (checkPixel(Float.intBitsToFloat(fp.getPixel(x + 1, y + 1))))&&
                                (checkPixel(Float.intBitsToFloat(fp.getPixel(x - 1, y - 1))))&&
                                (checkPixel(Float.intBitsToFloat(fp.getPixel(x - 1, y + 1))))
                                ){
                            //System.out.println("IS BORDER");
                            if (isBorder) {
                                fp.putPixelValue(x, y, marker);
                            }
                        }

                    }
                }
            }
        }
        return resultImp;
    }

    public ImageStack combineCores() {
        ImageStack resultImp = currentImageStackCopy;
        //HashSet<HashSet<Float>> coresColorsForSelect = new HashSet<HashSet<Float>>();
        //ImageStack imsb = new ImageStack(w, h);
        //System.out.println("cutcolors: " + cutOutColor);
        previousCoresProc = null;
        for (int i = 1; i <= resultImp.getSize(); i++) {
            curCoresProc = (FloatProcessor) resultImp.getProcessor(i);
            //pixelMatrix = new ByteProcessor(w, h);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Float curIndex = Float.intBitsToFloat(curCoresProc.get(x, y));
                    if (cutOutColor.contains(curIndex)) {
                        curCoresProc.putPixelValue(x, y, 1.0);
                    }
                    if (curIndex == 0.0){
                        //check border point
                        curCoresGroup = new HashSet<Float>();
                        stackSize = 0;
                        try {
                            passOnTheBorder(x, y);
                            if (curCoresGroup.size() > 1){
                                fillObjectPoint(x, y);
                            }
                        }
                        catch (StackOverflowError e){
                            e.getMessage();
                        }

                    }
                }
            }
            if (previousCoresProc != null){
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        getColorFromPreviousSlice(x, y);
                    }
                }
            }
            previousCoresProc = curCoresProc;
        }

        saveChanges();
        return resultImp;
    }


    private void passOnTheBorder(int x, int y){
        if (Float.intBitsToFloat(curCoresProc.getPixel(x, y)) < 1) {
            stackSize += 1;
            //esli net r9dom fonovogo cveta
            //dobovl9em cveta esli po9v9ts9
            checkCoresPixel(x, y);
            if ((isBorder)&&(curCoresGroup.size() > 1)){
                curCoresProc.putPixelValue(x, y, (Float) curCoresGroup.toArray()[0]);
                //System.out.println(curCoresGroup.toArray()[0]);
                passOnTheBorder(x + 1, y);
                passOnTheBorder(x - 1, y);
                passOnTheBorder(x, y + 1);
                passOnTheBorder(x, y - 1);
                passOnTheBorder(x + 1, y - 1);
                passOnTheBorder(x + 1, y + 1);
                passOnTheBorder(x - 1, y - 1);
                passOnTheBorder(x + 1, y + 1);
            }
        }
    }

    private void fillObjectPoint(int x, int y){
        Float color = Float.intBitsToFloat(curCoresProc.getPixel(x, y));
        if (color > 1.0) {
            for (int y1 = y-1; y1 <= y+1; y1++) {
                for (int x1 = x - 1; x1 <= x + 1; x1++) {
                    Float curColor = Float.intBitsToFloat(curCoresProc.getPixel(x1, y1));
                    if ((curColor > 1.0)&&(Math.abs(curColor - color) > EPS)) {
                        curCoresProc.putPixelValue(x1, y1, color);
                        fillObjectPoint(x1, y1);
                    }
                }
            }
        }
    }

    private void getColorFromPreviousSlice(int x, int y){
        if (previousCoresProc != null) {
            Float color = Float.intBitsToFloat(curCoresProc.getPixel(x, y));
            Float previousColor = Float.intBitsToFloat(previousCoresProc.getPixel(x, y));
            if ((color > 1.0)&&(previousColor > 1.0)&&(Math.abs(previousColor - color) > EPS)) {
                curCoresProc.putPixelValue(x, y, previousColor);
                fillObjectPoint(x, y);
            }
        }
    }

    private boolean checkPixel(Float color){
        //a 4to esli vse sosedi bydyt to4kami granici
        if (selectedColors.contains(color)){
            isBorder = true;
        }
        if ((selectedColors.contains(color))||(color < 1)){
            return true;
        }
        else {
            return false;
        }
    }

    private void checkCoresPixel(int x, int y){
        isBorder = false;
        for (int y1 = y-1; y1 <= y+1; y1++) {
            for (int x1 = x - 1; x1 <= x + 1; x1++) {
                Float color = Float.intBitsToFloat(curCoresProc.getPixel(x1, y1));
                if (color > 1.0)
                    curCoresGroup.add(color);
                if (color == 1.0)
                    isBorder = true;
            }
        }
    }

    public ImageStack getOriginalImageStack() {
        currentImageStackCopy = originalImageStackCopy.duplicate();
        saveChanges();
        cutOutColor = new HashSet<Float>();
        return currentImageStackCopy;
    }

    private void saveChanges(){
        savedImageStackCopy = currentImageStackCopy.duplicate();
    }

    public void setCutColors(HashSet<Float> colors) {
        if (colors.contains(Float.parseFloat("0.0"))) {
            colors.remove(Float.parseFloat("0.0"));
        }
        if (colors.contains(Float.parseFloat("1.0"))) {
            colors.remove(Float.parseFloat("1.0"));
        }
        cutOutColor = colors;
        //System.out.println(cutOutColor);
    }

    public int addNewGroup(HashSet<Float> colorForGrouping){
        for (Float color: colorForGrouping) {
            groups.put(color, this.nextGroupIndex);
        }
        this.nextGroupIndex++;
        return this.nextGroupIndex - 1;
    }

    public ArrayList<Integer> getGroupIndex(){
        ArrayList<Integer> groupIndex = new ArrayList<Integer>();
        for(Float colorIndex : PixelsSize.keySet()) {
            if (groups.containsKey(colorIndex)){
                groupIndex.add(groups.get(colorIndex));
            }
            else{
                groupIndex.add(0);
            }
        }
        return groupIndex;
    }

}
