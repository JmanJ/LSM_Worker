package MainWindow;

import LsmReader.CZLSMInfo;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.*;

import java.awt.*;
import java.util.ArrayList;

import static Utilities.ColorUtilities.getNormalizedColorValue;

public class MyLSMImage{

    protected ImagePlus imp;
    protected ImagePlus curImp;
    private ImageStack coloredChannel;
    protected ArrayList<ImageStack> imageStacks;
    public CZLSMInfo iminfo;
    private int MAX_OFFSET = 15;
    private int CUT_SIZE = 15;
    private int DEFAULT_OFFSET = 0;
    private int DEFAULT_CHANEL = 0;
    private String imageTitle = "Current image";
    protected int offsetX = 0;
    protected int offsetY = 0;
    protected int offsetZ = 0;
    protected int newChannelIndex = -1;
    private int chanel = 0;
    private int normalizationP = -1;
    private boolean isDivided = false;
    private boolean isCuted = false;
    private Rectangle cutRectangle;
    protected Image2DProcessor im2Dproc;
    protected boolean isGRAY8;
    private int maxDiff;
    private int loadedImagesCount = 0;


    public MyLSMImage(ImagePlus imp, CZLSMInfo iminfo, int scale){
        if (iminfo.isOptimized) {
            new ImageConverter(imp).convertToGray8();
        }
        this.imp = imp;
        this.iminfo = iminfo;
        this.im2Dproc = null;
        this.coloredChannel = null;
        //this.isOptimized = iminfo.isOptimized;

        isGRAY8 = ImagePlus.GRAY8 == this.imp.getType();
        resizeImage(imp.getWidth() / scale, imp.getHeight() / scale);
        imageTitle = imp.getShortTitle();
        constructImage();
        setChanel(DEFAULT_CHANEL);

    }


    public void resizeImage(int width, int height){
        ImageStack stack = imp.getStack();
        ImageStack newStack = new ImageStack(width, height, stack.getColorModel());
        for (int i=1; i <= imp.getStackSize(); i++) {
            ImageProcessor p = stack.getProcessor(i);
            newStack.addSlice(stack.getSliceLabel(i), p.resize(width));
        }
        imp.setStack(newStack);
    }

    public void constructImage(){

        splitChanels();
        newChannelIndex = this.imageStacks.size();
        imageStacks.add(new ImageStack(1,1));
    }

    public void constructColoredChannel(int redColorChannelIndex, int greenColorChannelIndex, int blueColorChannelIndex) {
        int w = imageStacks.get(0).getWidth();
        int h = imageStacks.get(0).getHeight();
        ImageStack coloredStack = new ImageStack(w, h);
        double maxValue = imp.getDisplayRangeMax();
        for (int sliceIndex=1; sliceIndex <= imageStacks.get(0).getSize(); sliceIndex++){
            ImageProcessor imProc = new ColorProcessor(w, h);
            for (int y=0; y < imProc.getHeight(); y++){
                for (int x=0; x < imProc.getWidth(); x++){
                    int r = redColorChannelIndex != -1 ? getNormalizedColorValue(imageStacks.get(redColorChannelIndex).getProcessor(sliceIndex).get(x, y), maxValue) : 0;
                    int g = greenColorChannelIndex != -1 ? getNormalizedColorValue(imageStacks.get(greenColorChannelIndex).getProcessor(sliceIndex).get(x, y), maxValue) : 0;
                    int b = blueColorChannelIndex != -1 ? getNormalizedColorValue(imageStacks.get(blueColorChannelIndex).getProcessor(sliceIndex).get(x, y), maxValue) : 0;
                    imProc.set(x, y, new Color(r, g, b).getRGB());
                }
            }
            coloredStack.addSlice(imProc);
        }

        coloredChannel = coloredStack;
        curImp.setStack(coloredChannel);
    }

    protected ImageStack calculateDiffrenceStack(ArrayList<Double> coefs){
        // calculate new stack like chanle1 - chanel2
        int w = imageStacks.get(0).getWidth();
        int h = imageStacks.get(0).getHeight();
        ImageStack newDifStack = new ImageStack(w, h, imp.getStack().getColorModel());
        ArrayList<ImageProcessor> processorsOnCurrentLayer;
        for (int i=1; i <= imageStacks.get(0).getSize(); i++){
            processorsOnCurrentLayer = new ArrayList<>();
            for (int chanelIndex=0; chanelIndex < coefs.size(); chanelIndex++)
                processorsOnCurrentLayer.add(imageStacks.get(chanelIndex).getProcessor(i));
            ImageProcessor imProc;
            if (this.isGRAY8) {
                imProc = new ByteProcessor(w, h);
            }
            else{
                imProc = new ShortProcessor(w, h);
            }
            for (int y=0; y < processorsOnCurrentLayer.get(0).getHeight(); y++){
                for (int x=0; x < processorsOnCurrentLayer.get(0).getWidth(); x++){
                    double accumulatedValue = 0;
                    for (int chanelIndex=0; chanelIndex < coefs.size(); chanelIndex++)
                        accumulatedValue += processorsOnCurrentLayer.get(chanelIndex).get(x, y) * coefs.get(chanelIndex);
                    int pix = (int) Math.round(accumulatedValue);
                    if (pix < 0) pix = 0;
                    imProc.set(x, y, pix);
                }
            }
            newDifStack.addSlice(imProc);
        }

        //newDifStack.setColorModel(new IndexColorModel(8, 256, getColorPalette(imageColor, 256), 0, false));

        return newDifStack;
    }

    protected void splitChanels(){
        int n = (int) iminfo.DimensionChannels;
        int index;
        ImageStack stack = imp.getStack();
        imageStacks = new ArrayList<ImageStack>(n);
        for (int i=0; i < n; i++){
            imageStacks.add(new ImageStack(stack.getWidth(), stack.getHeight(), stack.getColorModel()));
        }
        for (int i=0; i < imp.getStackSize(); i++) {
            index = i % n;
            imageStacks.get(index).addSlice(stack.getProcessor(i + 1));
        }
        constructGoodStack();
    }

    protected void constructGoodStack(){
        for (int i=0; i < iminfo.DimensionChannels; i++){
            try {
                imageStacks.set(i, joinImages(imageStacks.get(i)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //need to cut image
        if (isCuted){
            cutImage((int) cutRectangle.getX(), (int) cutRectangle.getY(),
                    (int) cutRectangle.getWidth(), (int) cutRectangle.getHeight());

        }

        //only one frame
        /*
        for (int i=0; i < imageStacks.length; i++){
            try {
                ImageStack empty_imst = new ImageStack(imageStacks[i].getWidth(), imageStacks[i].getHeight());
                empty_imst.addSlice(imageStacks[i].getProcessor(20));
                imageStacks[i] = empty_imst;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        */

    }

    protected ImageStack joinImages(ImageStack stack) throws Exception {
        //Set size of image (count of frames)
        int p = (int) iminfo.DimensionP;  //w
        int m = (int) iminfo.DimensionM;  //h
        int z = (int) iminfo.DimensionZ;
        int w = (int) iminfo.RealDimensionX;
        int h = (int) iminfo.RealDimensionY;
        if (w + h > 0) {
            if (w * h != p * m) {
                throw new Exception("Incorrect parameters");
            }
        } else {
            w = m;
            h = p;
        }

        int realW, realH, overlapPixels = 0;
        int stackW = stack.getWidth();
        int stackH = stack.getHeight();
        if (iminfo.Overlap > 0){
            overlapPixels = (int) (stackH * iminfo.Overlap) / 100;
            realH = stackH + ((stackH - overlapPixels) * (h - 1));
            realW = stackW + ((stackW - overlapPixels) * (w - 1));
        }
        else{
            realH = stackH * h;
            realW = stackW * w;
        }


        ImageStack imS = new ImageStack(realW, realH, stack.getColorModel());

        //ArrayList<ImageProcessor> framesX = new ArrayList<ImageProcessor>();
        int frameIndexX, curentAverage, maxAverage, curOffsetY, zOffset;
        for (int zi=1; zi <= z - (w-1)*offsetZ; zi++){
            frameIndexX = -1;
            maxAverage = -1;
            //framesX.clear();
            zOffset = 0;
            ImageProcessor imProc;
            if (this.isGRAY8) {
                imProc = new ByteProcessor(realW, realH);
            }
            else{
                imProc = new ShortProcessor(realW, realH);
            }
            for (int wi=0; wi < w; wi++){
                //ImageProcessor imProcX = new ByteProcessor(stack.getWidth(), real);
                for (int hi=0; hi < h; hi++){
                    //join all frames in the column (oY)
                    int index = zi + zOffset + z * (wi + w*hi);
                    if (wi > 0)
                        curOffsetY = offsetY * hi;
                    else
                        curOffsetY = 0;

                    copyPixels(stack.getProcessor(index), offsetY * hi, 0,
                            stackW - offsetY * hi, stackH - offsetX * wi,
                            (stackW * wi - overlapPixels * wi) - curOffsetY, (stackH * hi - overlapPixels * hi) + offsetX * wi,
                            imProc);
                }
                //what frame better for calculating offset?
                //curentAverage = calcBorderAverage(imProcX);
                //if ((curentAverage > maxAverage) && (wi < w - 1)) {
                //    maxAverage = curentAverage;
                //    frameIndexX = wi;
                //}
                //framesX.add(imProcX);
                zOffset += offsetZ;
            }
            //all time offset must be same
            //offset = calcOffset(framesX.get(frameIndexX), framesX.get(frameIndexX + 1));
            //System.out.println(offset + " : " + maxAverage);
            //offset = 4;

            //MainWindow.Normalization Slaces is on
            //if (normalizationP != -1) {
            //    ArrayList<ImageProcessor> framesXnormalised = new ArrayList<ImageProcessor>();
            //    for (ImageProcessor sliceoY : framesX) {
            //        framesXnormalised.add(normalize(sliceoY));
            //    }
            //    framesX = framesXnormalised;
            //}

            //System.out.println(realW + " : " + realH);
            //imS.addSlice(imProc);
            //imS.addSlice(joinFramesOY(framesX, overlapPixels));
            imS.addSlice(imProc);
        }

        if (isDivided) {
            imS = divideSlices(imS, w, h, CUT_SIZE);
        }
        return imS;
    }

    private ImageProcessor normalize(ImageProcessor imp_norm){
        Normalization n = new Normalization(new ImagePlus("MainWindow.Normalization", imp_norm));
        return n.process(30).getProcessor();
    }

    //average operation: IMAGE_FRAME_SIZE_OY
    protected int calcBorderAverage(ImageProcessor imP){
        long average = 0;
        for (int i=0; i < imP.getHeight(); i++) {
            average += (long) imP.get(imP.getWidth() - 1, i);
        }
        return (int) average/imP.getHeight();

    }

    //average operation: 2*MAX_OFFSET*(IMAGE_FRAME_SIZE_OY - 2*MAX_OFFSET)
    protected int calcOffset(ImageProcessor imP1, ImageProcessor imP2){
        int betterOffset = 0;
        long curDiff, minDiff;
        minDiff = 1000000;
        for (int curOffset= -MAX_OFFSET; curOffset < MAX_OFFSET; curOffset++){
            curDiff = 0;
            //ignoring first and last MAX_OFFSET pixels
            for (int i=MAX_OFFSET; i < imP1.getHeight() - MAX_OFFSET; i++) {
                curDiff += (long) Math.abs(imP1.get(imP1.getWidth() - 1, i) - imP2.get(0, i + curOffset));
            }
            //System.out.println(imP1.getPixel(0, MAX_OFFSET + curOffset));
            if (curDiff < minDiff){
                minDiff = curDiff;
                betterOffset = curOffset;
            }
        }

        return betterOffset;
    }

    protected ImageProcessor joinFramesOY(ArrayList<ImageProcessor> frames, int overlap){
        ImageProcessor imProc = null;
        if (overlap == 0) {
            int w = frames.size();
            int h = frames.get(0).getHeight() / frames.get(0).getWidth();

            if (this.isGRAY8) {
                imProc = new ByteProcessor(frames.get(0).getWidth() * w, frames.get(0).getHeight());
            }
            else{
                imProc = new ShortProcessor(frames.get(0).getWidth() * w, frames.get(0).getHeight());
            }
            for (int i = 0; i < w; i++) {
                for (int j=0; j < h; j++) {
                    copyPixels(frames.get(i), 0, frames.get(0).getWidth() * j, frames.get(0).getWidth(), frames.get(0).getWidth(),
                            frames.get(0).getWidth() * i - offsetY * j, frames.get(0).getWidth() * j + offsetX * i, imProc);
                    //imProc.insert(frames.get(i), frames.get(i).getWidth() * i, offset * i);
                }
            }
        }
        else{
            if (this.isGRAY8) {
                imProc = new ByteProcessor(frames.get(0).getWidth() + (frames.get(0).getWidth() - overlap) * (frames.size() - 1),
                        frames.get(0).getHeight());
            }
            else{
                imProc = new ShortProcessor(frames.get(0).getWidth() + (frames.get(0).getWidth() - overlap) * (frames.size() - 1),
                        frames.get(0).getHeight());
            }
            for (int i = 0; i < frames.size(); i++) {
                if (i > 0){
                    imProc.insert(copyPixels(frames.get(i), overlap, 0, frames.get(i).getWidth() - overlap, frames.get(i).getHeight(),
                            0, 0, null), frames.get(i).getWidth() + (frames.get(i).getWidth() - overlap) * (i - 1), offsetX * i);
                }
                else {
                    imProc.insert(frames.get(0), 0, offsetX * i);
                }
            }
        }
        //System.out.println(imProc.getWidth() + " : " + imProc.getHeight());
        return imProc;
    }

    private void cutImage(int xstart, int ystart, int w, int h){
        System.out.println("x: " + xstart + " y: " + ystart + " w: " + w + " h: " + h);
        ArrayList<ImageStack> newImageStacks = new ArrayList<ImageStack>((int) iminfo.DimensionChannels);
        ImageProcessor imgProc;
        for (ImageStack ims: imageStacks){
            ImageStack newStack = new ImageStack(w, h, ims.getColorModel());
            for (int i=1; i < ims.getSize() + 1; i++){
                if (this.isGRAY8) {
                    imgProc = new ByteProcessor(w, h);
                }
                else{
                    imgProc = new ShortProcessor(w, h);
                }
                copyPixels(ims.getProcessor(i), xstart, ystart, w, h, 0, 0, imgProc);
                imgProc.setMinAndMax(ims.getProcessor(i).getMin(), ims.getProcessor(i).getMax());
                imgProc.setColorModel(ims.getColorModel());
                newStack.addSlice(ims.getSliceLabel(i), imgProc);
            }
            newImageStacks.add(newStack);
        }
        imageStacks = newImageStacks;
        //setChanel(chanel);
    }

    //copy pixels ROI from impr_base to impr_target starting with (xloc, yloc)
    private ImageProcessor copyPixels(ImageProcessor impr_base, int xstart, int ystart, int width, int height,
                              int xloc, int yloc, ImageProcessor impr_target){

        if (impr_target == null){
            if (this.isGRAY8) {
                impr_target = new ByteProcessor(width, height);
            }
            else{
                impr_target = new ShortProcessor(width, height);
            }
        }

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
        return impr_target;
    }

    private ImageStack divideSlices(ImageStack imstack, int xSlices, int ySlices, int lineWidth){
        int w = imstack.getWidth();
        int h = imstack.getHeight();
        int wSlice = w / xSlices;
        int hSlice = h / ySlices;
        ImageProcessor curNewP;
        ImageStack newStack = new ImageStack(w+(xSlices - 1)*lineWidth, h+(ySlices - 1)*lineWidth);
        for (int nSlices = 1; nSlices <= imstack.getSize(); nSlices++) {
            ImageProcessor curP = imstack.getProcessor(nSlices);
            if (this.isGRAY8) {
                curNewP = new ByteProcessor(w + (xSlices - 1) * lineWidth, h + (ySlices - 1) * lineWidth);
            }
            else{
                curNewP = new ShortProcessor(w + (xSlices - 1) * lineWidth, h + (ySlices - 1) * lineWidth);
            }
            for (int y = 0; y < ySlices; y++) {
                for (int x = 0; x < xSlices; x++) {
                    copyPixels(curP, x * wSlice, y * hSlice, wSlice, hSlice,
                            x * (wSlice + lineWidth), y * (hSlice + lineWidth), curNewP);
                }
            }
            newStack.addSlice(curNewP);
        }

        return newStack;
    }

    public void splitAndCalcAverageAndDispersion(){
        int w = (int) iminfo.RealDimensionX;
        int h = (int) iminfo.RealDimensionY;
        //System.out.println("!!! " + curImp.getImageStack());

        ImageProcessor base_p = curImp.getProcessor();
        int w_slice =  base_p.getWidth()/w;
        int h_slice =  base_p.getHeight()/h;
        ImageProcessor target_p;
        ArrayList<double[]> slices_info = new ArrayList<double[]>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (this.isGRAY8) {
                    target_p = new ByteProcessor(w_slice, h_slice);
                }
                else{
                    target_p = new ShortProcessor(w_slice, h_slice);
                }
                copyPixels(base_p, x*(w_slice), y*(h_slice), w_slice, h_slice, 0, 0, target_p);
                slices_info.add(calculateAverageAndDispersion(target_p));
            }
        }

        int i = 1;
        for (double[] info: slices_info){
            System.out.println(i + ". Average: " + String.format("%.2f", info[0]) + " Disp: " + String.format("%.2f", info[1]));
            i++;
        }

    }

    protected double[] calculateAverageAndDispersion(ImageProcessor improc){
        double[] average_and_diffusion = {0, 0};
        double str_sum = 0;
        for (int y = 0; y < improc.getHeight(); y++){
            for (int x = 0; x < improc.getWidth(); x++){
                str_sum += improc.get(x,y);
            }
            average_and_diffusion[0] += (str_sum / (improc.getWidth() * improc.getHeight()));
            str_sum = 0;
        }
        str_sum = 0;
        for (int y = 0; y < improc.getHeight(); y++){
            for (int x = 0; x < improc.getWidth(); x++){
                str_sum += Math.pow((improc.get(x, y) - average_and_diffusion[0]), 2);
            }
            average_and_diffusion[1] += (str_sum / (improc.getWidth() * improc.getHeight()));
            str_sum = 0;
        }


        return average_and_diffusion;
    }

    public void setNewChannel(ArrayList<Double> coefs){
        ImageStack newStack = this.calculateDiffrenceStack(coefs);
        imageStacks.set(newChannelIndex, newStack);
    }

    public void setChanel(int n){
        if (n == -1){
            curImp.setTitle(imageTitle);
            curImp.repaintWindow();
            curImp.getWindow().repaint();
        }
        else {
            imageTitle = this.imp.getShortTitle() + " ch" + String.valueOf(n + 1);
            chanel = n;
        }
        if (curImp != null) {
            //this.curImp.flush();
            curImp.setTitle(imageTitle);
            curImp.setStack(imageStacks.get(chanel));
            //this.curImp.updateAndRepaintWindow();
        }
        else {
            curImp = new ImagePlus(imageTitle, imageStacks.get(chanel));
            curImp.repaintWindow();
        }
    }

    public ImagePlus getImp() {

        return curImp;
    }

    public void setImp(ImagePlus imp) {
        imageStacks.set(chanel, imp.getStack());
        setChanel(chanel);
    }

    public void setImp(ImagePlus imp, boolean isNewImage) {
        loadedImagesCount++;
        if (loadedImagesCount == 1){
            imageStacks.set(0, imp.getStack());
            imageStacks.set(1, imp.getStack());

        }
        if (loadedImagesCount == 2){
            imageStacks.set(chanel, imp.getStack());
            //imageStacks.set(2, calculateDiffrenceStack(0, 1));

        }
        if (loadedImagesCount > 2){
            return;
        }
        if (isNewImage) {
            imageTitle = imp.getShortTitle();
            setChanel(-1);
        }

    }

    public void setImpStack(ImageStack imstack) {
        imageStacks.set(chanel, imstack);
        setChanel(chanel);
    }

    public void setCurrentStack(ImageStack imstack) {
        imageStacks.set(chanel, imstack);
        setChanel(chanel);
    }

    public void makeSliceNormalization(int normalizationParametr){
        normalizationP = normalizationParametr;
        refreshImage();
    }

    public void cleanImage(){
        //this.curImp = null;
        offsetX = DEFAULT_OFFSET;
        offsetY = DEFAULT_OFFSET;
        normalizationP = -1;
        loadedImagesCount = 0;
        this.isCuted = false;
        //chanel = DEFAULT_CHANEL;
        refreshImage();
    }

    public void setOffset(int newOffsetX, int newOffsetY, int newOffsetZ){
        this.offsetX = newOffsetY;
        this.offsetY = newOffsetX;
        this.offsetZ = newOffsetZ;
        refreshImage();
    }

    public void changeDivided(){
        isDivided = !isDivided;
        refreshImage();
    }

    public void refreshImage(){
        constructImage();
        setChanel(chanel);
    }

    public int getOffsetX() {
        return this.offsetX;
    }

    public int getOffsetY() {
        return this.offsetY;
    }

    public void setCuted(Rectangle r){
        this.cutRectangle = r;
        this.isCuted = true;
        refreshImage();
    }

    public ImagePlus convertTo2DImage(int latticeValue, int maxDiff){
        if (coloredChannel == null) {
            int r = 0;
            int g = getOriginalChannelCount() > 1 ? 1 : -1;
            int b = getOriginalChannelCount() > 2 ? 2 : -1;
            constructColoredChannel(r, g, b);
        }
        im2Dproc = new Image2DProcessor(imageStacks.get(chanel), coloredChannel);
        this.maxDiff = maxDiff;
        return im2Dproc.calculateSurface(latticeValue, maxDiff);
    }

    public int getOriginalChannelCount() {
        return imageStacks.size() - 1; // one channel reserved for new channel
    }

    public Image2DProcessor get2DProc(){
        return im2Dproc;
    }

    public void cutImportImage(){

    }

    public int[][] get2DImageMap(){
        if (this.im2Dproc == null)
            return null;
        else
            return im2Dproc.getImageMap();
    }

    public ImagePlus getOriginalImage(){
        return this.imp;
    }

    public int getChannelCount(){
        return imageStacks.size();
    }

    public ImageProcessor getTestProcessor(){
        return im2Dproc.getMaskProc();
    }

    public int getMaxDiff(){
        return maxDiff;
    }
}
