package MainWindow;


import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
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
    private ImageStack image3DStack;

    public ImagePlus calculateSurface(ImageStack stack, int latticeValue, int maxDiff){
        image3DStack = stack;
        h = stack.getProcessor(1).getHeight();
        w = stack.getProcessor(1).getWidth();
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
        for (int z = 1; z <= stack.getSize(); z++){
            ImageProcessor curProc = stack.getProcessor(z);
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
                                if (z != stack.getSize()) {
                                    diff = stack.getProcessor(z + 1).get(x, y) - stack.getProcessor(z).get(x, y);
                                    if (diff > maxDiff) {
                                        isFinalMax[i][j] = true;
                                        int curZ = z + 1;
                                        while (curZ != stack.getSize()) {

                                            if (stack.getProcessor(curZ + 1).get(x, y) < stack.getProcessor(curZ).get(x, y)){
                                                values[i][j] = stack.getProcessor(curZ).get(x, y);
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
        return constructProcessor(lattice, stack.getSize());
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
        int zIndex;
        for (int y = 0; y < maskProc.getHeight(); y++)
            for (int x = 0; x < maskProc.getWidth(); x++){
                zIndex = maskProc.get(x, y) + diveValue;
                if (zIndex <= 0) zIndex = 1;
                if (zIndex > image3DStack.getSize()) zIndex = image3DStack.getSize();
                curProc.set(x, y, image3DStack.getProcessor(zIndex).get(x, y));
            }
        return curProc;
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
        this.maskProc = newMaskProc;
    }

}
