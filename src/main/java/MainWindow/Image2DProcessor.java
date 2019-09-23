package MainWindow;


import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class Image2DProcessor{
    private int h;
    private int w;
    private int latticeH;
    private int latticeW;
    private int step;
    private int diveValue = 0;
    private ImageProcessor maskProc = null;
    private ImageProcessor curProc = null;
    private ImageProcessor coloredProc;
    private ImageStack image3DStack;
    private ImageStack coloredImageStack;

    public Image2DProcessor(ImageStack originalStack, ImageStack coloredImageStack) {
        this.image3DStack = originalStack;
        this.coloredImageStack = coloredImageStack;
        this.coloredProc = new ColorProcessor(coloredImageStack.getWidth(), coloredImageStack.getHeight());
    }

    public ImagePlus calculateSurface(int latticeValue, int maxDiff){
        h = image3DStack.getProcessor(1).getHeight();
        w = image3DStack.getProcessor(1).getWidth();
        if (latticeValue > 0)
            step = latticeValue;
        else
            step = 1;
        latticeH = h/step + 1;
        latticeW = w/step + 1;
        int[][] lattice = new int[latticeW][latticeH];
        int[][] values = new int[latticeW][latticeH];
        boolean[][] isFinalMax = new boolean[latticeW][latticeH];
        int diff;
        for (int z = 1; z <= image3DStack.getSize(); z++){
            ImageProcessor curProc = image3DStack.getProcessor(z);
            for (int y = 0, j = 0; y < h; y+= step, j++) {
                for (int x = 0, i = 0; x < w; x+= step, i++) {
                    if (!isFinalMax[i][j]) {
                        if (z == 1) {
                            values[i][j] = curProc.get(x, y);
                            isFinalMax[i][j] = false;
                            lattice[i][j] = z;
                        } else {
                            if ((curProc.get(x, y) > values[i][j]) && (!isFinalMax[i][j])) {
                                values[i][j] = curProc.get(x, y);
                                lattice[i][j] = z;
                                if (z != image3DStack.getSize()) {
                                    diff = image3DStack.getProcessor(z + 1).get(x, y) - image3DStack.getProcessor(z).get(x, y);
                                    if (diff > maxDiff) {
                                        isFinalMax[i][j] = true;
                                        int curZ = z + 1;
                                        while (curZ != image3DStack.getSize()) {

                                            if (image3DStack.getProcessor(curZ + 1).get(x, y) < image3DStack.getProcessor(curZ).get(x, y)){
                                                values[i][j] = image3DStack.getProcessor(curZ).get(x, y);
                                                lattice[i][j] = curZ;
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
            }
        }
        //System.out.print(Arrays.deepToString(lattice));
        return constructProcessor(lattice, image3DStack.getSize());
    }

    private ImagePlus constructProcessor(int[][] lattice, int stackSize) {
        maskProc = new ByteProcessor(w, h);
        if (image3DStack.getBitDepth() == ImagePlus.GRAY8)
            curProc = new ByteProcessor(w, h);
        else
            curProc = new ShortProcessor(w, h);
        curProc.setColorModel(image3DStack.getColorModel());
        float dys1, dys2, dxs;
        int z1, z2, curz;
        for (int y = 0, j = 0; j < latticeH - 1; y+= step, j++) {
            for (int x = 0, i = 0; i < latticeW - 1; x+= step, i++) {
                dys1 = (lattice[i][j+1] -  lattice[i][j])/(float) step;
                dys2 = (lattice[i+1][j+1] -  lattice[i+1][j])/(float) step;
                z1 = lattice[i][j];
                z2 = lattice[i+1][j];
                //System.out.println(z1 + " " + z2);

                for (int y1 = y, it1 = 1; y1 < y + step; y1++, it1++){
                    dxs = (z2 - z1)/(float) step;
                    curz = z1;
                    for (int x1 = x, it2 = 1; x1 < x + step; x1++, it2++){
                        curProc.putPixel(x1, y1, image3DStack.getProcessor(curz).get(x1, y1));
                        maskProc.putPixel(x1, y1, curz);
                        curz = Math.round(z1 + dxs*it2);
                    }
                    z1 = Math.round(lattice[i][j] + dys1*it1);
                    z2 = Math.round(lattice[i+1][j] + dys2*it1);
                }
            }
        }

        //new ImagePlus("test image", maskProc).show();

        //smooth2DImage(2, 5).show();

        return new ImagePlus("2D image", curProc);
    }

    public int[][] getImageMap(){

        return maskProc.getIntArray();
    }

    public void smooth2DImage(int radius, int ntimes){
        maskProc = new Median_2DFilter(maskProc).Hybrid2dMedianizer(radius, ntimes);

        //new ImagePlus("smoothing test image", smoothig_mask).show();
    }

    public ImageProcessor getCur2DProc(){
        update2DProcessor(curProc, image3DStack);
        return curProc;
    }

    public ImageProcessor getColored2dProc() {
        update2DProcessor(coloredProc, coloredImageStack);
        return coloredProc;
    }

    private void update2DProcessor(ImageProcessor proc, ImageStack original3DImageStack) {
        int zIndex;
        for (int y = 0; y < maskProc.getHeight(); y++)
            for (int x = 0; x < maskProc.getWidth(); x++){
                zIndex = maskProc.get(x, y) + diveValue;
                if (zIndex <= 0) zIndex = 1;
                if (zIndex > original3DImageStack.getSize()) zIndex = original3DImageStack.getSize();
                proc.set(x, y, original3DImageStack.getProcessor(zIndex).get(x, y));
            }

    }

    public ImageProcessor getMaskProc(){
        return maskProc;
    }

    public ImagePlus getMaskImage(){
        return new ImagePlus("Mask image", maskProc);
    }

    public void setDiveValue(int diveValue) {
        this.diveValue = diveValue;
    }

    public void setImageMask(ImageProcessor newMaskProc){
        maskProc = newMaskProc;
    }

}
