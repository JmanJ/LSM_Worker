package MainWindow;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;

import java.awt.image.ColorModel;
import java.io.File;
import java.util.HashMap;
import java.util.Random;

public class Normalization{

    ImagePlus imp;

    public Normalization(ImagePlus imp){
        this.imp = imp;
    }

    class Replacements {

        HashMap<Integer, Long> replacements;
        long totalReplacements;
        int minReplacement = Integer.MAX_VALUE;
        int maxReplacement = Integer.MIN_VALUE;
        Random rng;
        int quantile;

        public Replacements(int possibleValues) {
            replacements = new HashMap<Integer, Long>();
            rng = new Random();
        }

        public void addSomeReplacements(long howManyToReplace, int replacement) {
            if (replacement < minReplacement)
                minReplacement = replacement;
            if (replacement > maxReplacement)
                maxReplacement = replacement;
            if (!replacements.containsKey(replacement)) {
                replacements.put(replacement, 0L);
            }
            long previousValue = replacements.get(replacement);
            replacements.put(replacement, previousValue + howManyToReplace);
            totalReplacements += howManyToReplace;
        }

        public int getRandomReplacement() {
            if (totalReplacements == 0) {
                return -1;
            }

            long index = Math.abs(rng.nextLong()) % totalReplacements;

            long replacementsSkipped = 0;

            for (int r = minReplacement; r <= maxReplacement; ++r) {

                if (!replacements.containsKey(r))
                    continue;

                long indexInThisSlot = index - replacementsSkipped;

                long numberOfReplacements = replacements.get(r);
                if (indexInThisSlot < numberOfReplacements) {
                    // Then we remove one of these and return
                    // the value of r.
                    replacements.put(r, numberOfReplacements - 1);
                    --totalReplacements;
                    return r;
                } else {
                    replacementsSkipped += numberOfReplacements;
                }
            }
            return -1;
        }

        @Override
        public String toString() {
            if (totalReplacements == 0)
                return "No replacements left.";
            else {
                String result = "" + totalReplacements + " replacements left (in";
                for (int i = minReplacement; i <= maxReplacement; ++i) {
                    long numberOfReplacements = replacements.get(i);
                    if (numberOfReplacements > 0)
                        result += " " + i + " (" + numberOfReplacements + ")";
                }
                return result;
            }

        }

    }

    public class Mask {
        public boolean[][] inMask;
        int width, height, depth;
        long pointsInMask;

        public Mask(File maskFile) {

            ImagePlus maskImagePlus = imp.duplicate();
            ImageStack maskStack = maskImagePlus.getStack();
            width = maskImagePlus.getWidth();
            height = maskImagePlus.getHeight();
            depth = maskImagePlus.getStackSize();
            inMask = new boolean[depth][width * height];
            pointsInMask = 0;
            for (int z = 0; z < depth; ++z) {
                byte[] pixels = (byte[]) maskStack.getPixels(z + 1);
                for (int y = 0; y < height; ++y) {
                    for (int x = 0; x < width; ++x) {
                        if ((pixels[y * width + x] & 0xFF) > 127) {
                            inMask[z][y * width + x] = true;
                            ++pointsInMask;
                        }
                    }
                }
            }
            maskImagePlus.close();
        }
    }

    public void divideIntoQuantiles(int numberOfQuantiles,
                                    long frequencies[],
                                    long pointsInImage,
                                    long[] resultSumValuesInQuantile,
                                    long[] resultNumberOfValuesInQuantile) {

        if (numberOfQuantiles != resultNumberOfValuesInQuantile.length)
            throw new RuntimeException("BUG: numberOfQuantiles didn't match resultNumberOfValuesInQuantile.length");
        if (numberOfQuantiles != resultSumValuesInQuantile.length)
            throw new RuntimeException("BUG: numberOfQuantiles didn't match resultSumValuesInQuantile.length");

        for (int q = 0; q < numberOfQuantiles; ++q) {

            long indexStartThisQuantile = (int) (q * pointsInImage / numberOfQuantiles);
            long indexStartNextQuantile = (int) (((q + 1) * pointsInImage) / numberOfQuantiles);

            long pointsInQuantile = indexStartNextQuantile - indexStartThisQuantile;

            // If this is the last quantile, make sure we actually
            // include everything...
            if (q == numberOfQuantiles - 1) {
                indexStartNextQuantile = pointsInImage;
            }

            // Keep track of the sum of the values
            long cumulativeIncluding = 0;
            long cumulativeBefore = 0;

            resultSumValuesInQuantile[q] = 0;
            resultNumberOfValuesInQuantile[q] = 0;

            for (int value = 0; value < frequencies.length; ++value) {

                cumulativeIncluding += frequencies[value];

                if ((cumulativeIncluding < indexStartThisQuantile) || (cumulativeBefore >= indexStartNextQuantile)) {

                    // Then there's no overlap...

                } else {

                    long startInValues = 0;

                    if (indexStartThisQuantile > cumulativeBefore) {
                        startInValues = indexStartThisQuantile - cumulativeBefore;
                    }

                    // This is the end inclusive...
                    long endInValues = frequencies[value] - 1;

                    if (indexStartNextQuantile < cumulativeIncluding) {
                        endInValues = (indexStartNextQuantile - cumulativeBefore) - 1;
                    }
                    long pointsInOverlap = (endInValues - startInValues) + 1;
                    // System.out.println("points in overlap: "+pointsInOverlap);
                    resultNumberOfValuesInQuantile[q] += pointsInOverlap;
                    resultSumValuesInQuantile[q] += value * pointsInOverlap;
                }

                cumulativeBefore += frequencies[value];
            }
        }
    }

    public void generateReplacements(
            ImagePlus imagePlus,
            int numberOfQuantiles,
            long pointsInImage,
            long[] frequencies,
            long[] sumValuesInQuantile,
            long[] numberOfValuesInQuantile,
            double[] quantileMeans,
            Replacements[] resultRankReplacements,
            Replacements[] resultMeanReplacements) {

        int possibleImageValues = frequencies.length;

        for (int q = 0; q < numberOfQuantiles; ++q) {

            long[] replacementsInThisQuantile = new long[possibleImageValues];

            long indexStartThisQuantile = (int) (q * pointsInImage / numberOfQuantiles);
            long indexStartNextQuantile = (int) (((q + 1) * pointsInImage) / numberOfQuantiles);

            long pointsInQuantile = indexStartNextQuantile - indexStartThisQuantile;

            // If this is the last quantile, make sure we actually
            // include everything...
            if (q == numberOfQuantiles - 1) {
                indexStartNextQuantile = pointsInImage;
            }

            // Keep track of the sum of the values
            long cumulativeIncluding = 0;
            long cumulativeBefore = 0;

            for (int value = 0; value < frequencies.length; ++value) {

                cumulativeIncluding += frequencies[value];

                if ((cumulativeIncluding < indexStartThisQuantile) || (cumulativeBefore >= indexStartNextQuantile)) {

                    // Then there's no overlap...

                } else {

                    long startInValues = 0;

                    if (indexStartThisQuantile > cumulativeBefore) {
                        startInValues = indexStartThisQuantile - cumulativeBefore;
                    }

                    // This is the end inclusive...
                    long endInValues = frequencies[value] - 1;

                    if (indexStartNextQuantile < cumulativeIncluding) {
                        endInValues = (indexStartNextQuantile - cumulativeBefore) - 1;
                    }
                    long pointsInOverlap = (endInValues - startInValues) + 1;
                    numberOfValuesInQuantile[q] += pointsInOverlap;
                    sumValuesInQuantile[q] += value * pointsInOverlap;
                    replacementsInThisQuantile[value] = pointsInOverlap;
                }

                cumulativeBefore += frequencies[value];
            }

            double mean = quantileMeans[q];

            int byteLowerThanMean = (int) Math.floor(mean);
            int byteHigherThanMean = (int) Math.ceil(mean);

            double proportionLower = Math.ceil(mean) - mean;
            int lowerBytes = (int) Math.round(proportionLower * (indexStartNextQuantile - indexStartThisQuantile));
            int higherBytes = (int) (numberOfValuesInQuantile[q] - lowerBytes);

            long replacementsAddedAlready = 0;

            for (int i = 0; i < possibleImageValues; ++i) {

                long r = replacementsInThisQuantile[i];

                if (r == 0)
                    continue;

                long howManyLowerToAdd = 0;
                long howManyHigherToAdd = 0;

                if (replacementsAddedAlready >= lowerBytes) {
                    howManyHigherToAdd = r;
                } else if (replacementsAddedAlready + r >= lowerBytes) {
                    howManyLowerToAdd = lowerBytes - replacementsAddedAlready;
                    howManyHigherToAdd = r - howManyLowerToAdd;
                } else {
                    howManyLowerToAdd = r;
                }

                resultMeanReplacements[i].addSomeReplacements(howManyLowerToAdd, byteLowerThanMean);
                resultMeanReplacements[i].addSomeReplacements(howManyHigherToAdd, byteHigherThanMean);

                resultRankReplacements[i].addSomeReplacements(r, q);

                replacementsAddedAlready += r;
            }
        }
    }

    ImagePlus remapImage(ImagePlus imagePlus,
                         int numberOfQuantiles,
                         boolean replaceWithRankInstead,
                         boolean rescaleRanks,
                         Mask mask,
                         Replacements[] rankReplacements,
                         Replacements[] meanReplacements) {

        if (mask != null) {
            System.out.println("remapping with mask: " + mask);
        }

        int originalImageType = imagePlus.getType();
        int width = imagePlus.getWidth();
        int height = imagePlus.getHeight();
        ImageStack stack = imagePlus.getStack();
        int depth = stack.getSize();
        ImageStack newStack = new ImageStack(width, height);
        for (int z = 0; z < depth; ++z) {
            byte[] oldPixelsByte = null, newPixelsByte = null;
            short[] oldPixelsShort = null, newPixelsShort = null;
            if (originalImageType == ImagePlus.GRAY16) {
                oldPixelsShort = (short[]) stack.getPixels(z + 1);
                newPixelsShort = new short[width * height];
            } else {
                oldPixelsByte = (byte[]) stack.getPixels(z + 1);
                newPixelsByte = new byte[width * height];
            }
            for (int y = 0; y < height; ++y)
                for (int x = 0; x < width; ++x) {
                    if ((mask != null) && !mask.inMask[z][y * width + x])
                        continue;
                    int oldValue;
                    if (originalImageType == ImagePlus.GRAY16)
                        oldValue = oldPixelsShort[y * width + x] & 0xFFFF;
                    else
                        oldValue = oldPixelsByte[y * width + x] & 0xFF;
                    int replacement;
                    if (replaceWithRankInstead) {
                        replacement = rankReplacements[oldValue].getRandomReplacement();
                        if (rescaleRanks)
                            replacement = (255 * replacement) / (numberOfQuantiles - 1);
                    } else {
                        replacement = meanReplacements[oldValue].getRandomReplacement();
                    }
                    if (replacement < 0) {
                        System.out.println("BUG: ran out of replacements for " + oldValue);
                        replacement = oldValue;
                    }
                    if (originalImageType == ImagePlus.GRAY16)
                        newPixelsShort[y * width + x] = (short) replacement;
                    else
                        newPixelsByte[y * width + x] = (byte) replacement;
                }
            if (originalImageType == ImagePlus.GRAY16) {
                ShortProcessor sp = new ShortProcessor(width, height);
                sp.setPixels(newPixelsShort);
                newStack.addSlice("", sp);
            } else {
                ByteProcessor bp = new ByteProcessor(width, height);
                bp.setPixels(newPixelsByte);
                newStack.addSlice("", bp);
            }

            IJ.showProgress(z / (double) depth);
        }

        IJ.showProgress(1.0);

        if (ImagePlus.COLOR_256 == imagePlus.getType()) {
            ColorModel cm = null;
            cm = stack.getColorModel();
            if (cm != null) {
                newStack.setColorModel(cm);
            }
        }

        ImagePlus newImage = new ImagePlus("normalized " + imagePlus.getTitle(), newStack);
        newImage.setCalibration(imagePlus.getCalibration());

        return newImage;
    }

    File getMaskFileFromImageFile(File imageFile) {
        String fileLeafName = imageFile.getName();
        int indexOfLastDot = fileLeafName.lastIndexOf(".");
        if (indexOfLastDot < 0)
            throw new RuntimeException(
                    "Tried to find the corresponding mask file for an image file with an extension: " +
                            imageFile.getAbsolutePath());
        String fileWithoutExtension = fileLeafName.substring(0, indexOfLastDot);
        String extension = fileLeafName.substring(indexOfLastDot);
        return new File(
                imageFile.getParent(),
                fileWithoutExtension + ".mask.tif");
    }

    static final int POSSIBLE_8_BIT_VALUES = 256;
    static final int POSSIBLE_16_BIT_VALUES = 65536;

    public ImagePlus process(int numberOfQuantiles) {

            ImagePlus imagePlus = imp.duplicate();
            int width = imagePlus.getWidth();
            int height = imagePlus.getHeight();
            int depth = imagePlus.getStackSize();
            int n = 1;
            int b = 0;
            int possibleImageValues = -1;

            long frequencies[][] = null;
            long pointsInImage[] = new long[n];

            long [][] sumValuesInQuantile = new long[n][numberOfQuantiles];
            long [][] numberOfValuesInQuantile = new long[n][numberOfQuantiles];

            int type = imagePlus.getType();
            if (type == ImagePlus.GRAY8) {
                possibleImageValues = POSSIBLE_8_BIT_VALUES;
            }
            if (type == ImagePlus.GRAY16 ){
                possibleImageValues = POSSIBLE_16_BIT_VALUES;
            }
            //if (possibleImageValues == -1) {
            //    throw new Exception("Incorrect format");
            //}
            frequencies = new long[n][possibleImageValues];

            // If we're using a mask they all have to be the right
            // dimensions.

            ImageStack stack = imagePlus.getStack();
            Mask mask = null;

            //IJ.showStatus("Calculating frequencies and quantiles for " + imagePlus.getShortTitle() + " ...");

            for (int z = 0; z < depth; ++z) {
                if (possibleImageValues == POSSIBLE_8_BIT_VALUES) {
                    byte[] pixels = (byte[]) stack.getPixels(z + 1);
                    for (int y = 0; y < height; ++y)
                        for (int x = 0; x < width; ++x) {
                            if ((mask == null) || mask.inMask[z][y * width + x]) {
                                int value = pixels[y * width + x] & 0xFF;
                                ++frequencies[b][value];
                            }
                        }
                } else {
                    short[] pixels = (short[]) stack.getPixels(z + 1);
                    for (int y = 0; y < height; ++y)
                        for (int x = 0; x < width; ++x) {
                            if ((mask == null) || mask.inMask[z][y * width + x]) {
                                int value = pixels[y * width + x] & 0xFFFF;
                                ++frequencies[b][value];
                            }
                        }
                }
            }

            pointsInImage[b] = (mask == null) ? width * height * depth : mask.pointsInMask;

            //System.out.println("Proportion of points to consider: " + ((double) pointsInImage[b] / (width * height * depth)));

            divideIntoQuantiles(numberOfQuantiles,
                    frequencies[b],
                    pointsInImage[b],
                    sumValuesInQuantile[b],
                    numberOfValuesInQuantile[b]);

            imagePlus.close();

        //System.out.println("Now going on to calculate the mean in each quantile.");

        // Calculate the mean in each quantile (even if we're
        // not going to use it)...

        double[] quantileMeans = new double[numberOfQuantiles];

        for (int q = 0; q < numberOfQuantiles; ++q) {
            long sum = 0;
            long values = 0;
            for (int bb = 0; bb < n; ++bb) {
                sum += sumValuesInQuantile[bb][q];
                values += numberOfValuesInQuantile[bb][q];
            }
            quantileMeans[q] = sum / (double) values;
        }

        // Now we go through each image again, remap the
        // values according to the options chosen and write
        // the new image out to the output directory....


            imagePlus = imp.duplicate();

			/* meanReplacements or rankReplacements are
			   the arrays that are ultimately used to get
			   replacement values for the image, as in:
			     rankReplacements[oldValue].getRandomReplacement()
			     The Replacment class stores a particular
			     number of replacment values, and tracks
			     how many are left after removing a random
			     one.
			*/

            Replacements[] meanReplacements = new Replacements[possibleImageValues];
            for (int value = 0; value < possibleImageValues; ++value)
                meanReplacements[value] = new Replacements(possibleImageValues);

            Replacements[] rankReplacements = new Replacements[possibleImageValues];
            for (int value = 0; value < possibleImageValues; ++value)
                rankReplacements[value] = new Replacements(numberOfQuantiles);

            width = imagePlus.getWidth();
            height = imagePlus.getHeight();
            depth = imagePlus.getStackSize();

            //IJ.showStatus("Replacing values in: " + imagePlus.getShortTitle() + " ...");

            generateReplacements(imagePlus,
                    numberOfQuantiles,
                    pointsInImage[b],
                    frequencies[b],
                    sumValuesInQuantile[b],
                    numberOfValuesInQuantile[b],
                    quantileMeans,
                    rankReplacements,
                    meanReplacements);

            //IJ.showProgress(0);


            boolean replaceWithRankInstead = false;
            boolean rescaleRanks = false;

            ImagePlus newImage = remapImage(imagePlus,
                    numberOfQuantiles,
                    replaceWithRankInstead,
                    rescaleRanks,
                    mask,
                    rankReplacements,
                    meanReplacements);

            //newImage.show();

            imagePlus.close();

            return newImage;
        }

    }