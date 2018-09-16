package MainWindow;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.text.NumberFormat;
import java.util.Arrays;


public class Median_2DFilter{

    private ImagePlus impcopy;
    private ImageProcessor input_proc;
    private ImageStack tempstack;
    private ImageProcessor ip2;
    private boolean atebit = false;
    private boolean astack;
    private NumberFormat nf = NumberFormat.getInstance();
    private int nsize = 1;
    private int times = 1;
    private String otitle = "Smoothing 2D image";
    private String sizes[] = {"3x3","5x5","7x7"};
    public int status = 0;

    public Median_2DFilter(ImageProcessor input_proc){
        this.input_proc = input_proc;
        if(input_proc.getBitDepth()==8)atebit=true;
        astack = false;
    }

    public ImageProcessor Hybrid2dMedianizer(int size, int ntimes){
        if ((size >= 0) && (size < 3))
            this.nsize = size;
        if (ntimes > 0)
            this.times = ntimes;
        int m = input_proc.getWidth();
        int n = input_proc.getHeight();
        int dimension = m*n;
        short thisslice[];
        short newslice[];

        byte thisslice8[];
        byte newslice8[];

        if(atebit){//8bit scenario
            thisslice8 = new byte[dimension];
            newslice8 = new byte[dimension];
        }
        else{//16bit scenario
            thisslice = new short[dimension];
            newslice = new short[dimension];
        }
        double dubthisslice[] = new double[dimension];
        double filteredslice[] = new double[dimension];

        double marraythisP[] = new double[5];
        double marraythisX[] = new double[5];

        if(nsize==1){
            marraythisP = new double[9];
            marraythisX = new double[9];
        }
        if(nsize==2){
            marraythisP = new double[13];
            marraythisX = new double[13];
        }
        double medianarray[] = new double[3];

        if(atebit){
            thisslice8 = (byte[])input_proc.getPixels();
            dubthisslice = byte2double(thisslice8);
        }
        else{
            thisslice = (short[])input_proc.getPixels();
            dubthisslice = short2double(thisslice);
        }

        if(nsize==0){
            for(int k=0;k<times;++k){
                for(int j=0;j<dimension;++j){
                    try{marraythisP[0] = dubthisslice[(j-m)];}catch(Exception e){marraythisP[0]=dubthisslice[j];}
                    try{marraythisP[1] = dubthisslice[(j-1)];}catch(Exception e){marraythisP[1]=dubthisslice[j];}
                    try{marraythisP[2] = dubthisslice[j];    }catch(Exception e){marraythisP[2]=dubthisslice[j];}
                    try{marraythisP[3] = dubthisslice[(j+1)];}catch(Exception e){marraythisP[3]=dubthisslice[j];}
                    try{marraythisP[4] = dubthisslice[(j+m)];}catch(Exception e){marraythisP[4]=dubthisslice[j];}

                    try{marraythisX[0] = dubthisslice[(j-(m+1))];}catch(Exception e){marraythisX[0]=dubthisslice[j];}
                    try{marraythisX[1] = dubthisslice[(j-(m-1))];}catch(Exception e){marraythisX[1]=dubthisslice[j];}
                    try{marraythisX[2] = dubthisslice[j];        }catch(Exception e){marraythisX[2]=dubthisslice[j];}
                    try{marraythisX[3] = dubthisslice[(j+(m-1))];}catch(Exception e){marraythisX[3]=dubthisslice[j];}
                    try{marraythisX[4] = dubthisslice[(j+(m+1))];}catch(Exception e){marraythisX[4]=dubthisslice[j];}

                    medianarray[0] = median(marraythisX);
                    medianarray[1] = median(marraythisP);
                    medianarray[2] = dubthisslice[j];
                    filteredslice[j] = median(medianarray);
                }
                for(int h=0;h<dimension;++h)dubthisslice[h]=filteredslice[h];
            }
        }
        if(nsize==1){
            for(int k=0;k<times;++k){
                for(int j=0;j<dimension;++j){

                    try{marraythisP[0] = dubthisslice[(j-m)];}catch(Exception e){marraythisP[0]=dubthisslice[j];}
                    try{marraythisP[1] = dubthisslice[(j-1)];}catch(Exception e){marraythisP[1]=dubthisslice[j];}
                    try{marraythisP[2] = dubthisslice[j];    }catch(Exception e){marraythisP[2]=dubthisslice[j];}
                    try{marraythisP[3] = dubthisslice[(j+1)];}catch(Exception e){marraythisP[3]=dubthisslice[j];}
                    try{marraythisP[4] = dubthisslice[(j+m)];}catch(Exception e){marraythisP[4]=dubthisslice[j];}

                    try{marraythisP[5] = dubthisslice[(j-2*m)];}catch(Exception e){
                        try{marraythisP[5]=dubthisslice[j-m];}catch(Exception ee){marraythisP[5]=dubthisslice[j];}}

                    try{marraythisP[6] = dubthisslice[(j-2)];}catch(Exception e){
                        try{marraythisP[6]=dubthisslice[j-1];}catch(Exception ee){marraythisP[6]=dubthisslice[j];}}

                    try{marraythisP[7] = dubthisslice[(j+2)];}catch(Exception e){
                        try{marraythisP[7]=dubthisslice[j+1];}catch(Exception ee){marraythisP[7]=dubthisslice[j];}}

                    try{marraythisP[8] = dubthisslice[(j+2*m)];}catch(Exception e){
                        try{marraythisP[8]=dubthisslice[j+m];}catch(Exception ee){marraythisP[8]=dubthisslice[j];}}


                    try{marraythisX[0] = dubthisslice[(j-(m+1))];}catch(Exception e){marraythisX[0]=dubthisslice[j];}
                    try{marraythisX[1] = dubthisslice[(j-(m-1))];}catch(Exception e){marraythisX[1]=dubthisslice[j];}
                    try{marraythisX[2] = dubthisslice[j];        }catch(Exception e){marraythisX[2]=dubthisslice[j];}
                    try{marraythisX[3] = dubthisslice[(j+(m-1))];}catch(Exception e){marraythisX[3]=dubthisslice[j];}
                    try{marraythisX[4] = dubthisslice[(j+(m+1))];}catch(Exception e){marraythisX[4]=dubthisslice[j];}

                    try{marraythisX[5] = dubthisslice[(j-(2*m+2))];}catch(Exception e){
                        try{marraythisX[5]=dubthisslice[j-(m+1)];}catch(Exception ee){marraythisX[5]=dubthisslice[j];}}

                    try{marraythisX[6] = dubthisslice[(j-(2*m-2))];}catch(Exception e){
                        try{marraythisX[6]=dubthisslice[j-(m-1)];}catch(Exception ee){marraythisX[6]=dubthisslice[j];}}

                    try{marraythisX[7] = dubthisslice[(j+(2*m-2))];}catch(Exception e){
                        try{marraythisX[7]=dubthisslice[j+(m-1)];}catch(Exception ee){marraythisX[7]=dubthisslice[j];}}

                    try{marraythisX[8] = dubthisslice[(j+(2*m+2))];}catch(Exception e){
                        try{marraythisX[8]=dubthisslice[j+(m+1)];}catch(Exception ee){marraythisX[8]=dubthisslice[j];}}

                    medianarray[0] = median(marraythisX);
                    medianarray[1] = median(marraythisP);
                    medianarray[2] = dubthisslice[j];
                    filteredslice[j] = median(medianarray);
                }
                for(int h=0;h<dimension;++h)dubthisslice[h]=filteredslice[h];
            }

        }
        if(nsize==2){
            for(int k=0;k<times;++k){
                for(int j=0;j<dimension;++j){

                    try{marraythisP[0] = dubthisslice[(j-m)];}catch(Exception e){marraythisP[0]=dubthisslice[j];}
                    try{marraythisP[1] = dubthisslice[(j-1)];}catch(Exception e){marraythisP[1]=dubthisslice[j];}
                    try{marraythisP[2] = dubthisslice[j];    }catch(Exception e){marraythisP[2]=dubthisslice[j];}
                    try{marraythisP[3] = dubthisslice[(j+1)];}catch(Exception e){marraythisP[3]=dubthisslice[j];}
                    try{marraythisP[4] = dubthisslice[(j+m)];}catch(Exception e){marraythisP[4]=dubthisslice[j];}

                    try{marraythisP[5] = dubthisslice[(j-2*m)];}catch(Exception e){
                        try{marraythisP[5]=dubthisslice[j-m];}catch(Exception ee){marraythisP[5]=dubthisslice[j];}}

                    try{marraythisP[6] = dubthisslice[(j-2)];}catch(Exception e){
                        try{marraythisP[6]=dubthisslice[j-1];}catch(Exception ee){marraythisP[6]=dubthisslice[j];}}

                    try{marraythisP[7] = dubthisslice[(j+2)];}catch(Exception e){
                        try{marraythisP[7]=dubthisslice[j+1];}catch(Exception ee){marraythisP[7]=dubthisslice[j];}}

                    try{marraythisP[8] = dubthisslice[(j+2*m)];}catch(Exception e){
                        try{marraythisP[8]=dubthisslice[j+m];}catch(Exception ee){marraythisP[8]=dubthisslice[j];}}

                    try{marraythisP[9] = dubthisslice[(j-3*m)];}catch(Exception e){
                        try{marraythisP[9] = dubthisslice[(j-2*m)];}catch(Exception ee){
                            try{marraythisP[9]=dubthisslice[j-m];}catch(Exception eee){marraythisP[9]=dubthisslice[j];}}}

                    try{marraythisP[10] = dubthisslice[(j-3)];}catch(Exception e){
                        try{marraythisP[10] = dubthisslice[(j-2)];}catch(Exception ee){
                            try{marraythisP[10]=dubthisslice[j-1];}catch(Exception eee){marraythisP[10]=dubthisslice[j];}}}

                    try{marraythisP[11] = dubthisslice[(j+3)];}catch(Exception e){
                        try{marraythisP[11] = dubthisslice[(j+2)];}catch(Exception ee){
                            try{marraythisP[11]=dubthisslice[j+1];}catch(Exception eee){marraythisP[11]=dubthisslice[j];}}}

                    try{marraythisP[12] = dubthisslice[(j+3*m)];}catch(Exception e){
                        try{marraythisP[12] = dubthisslice[(j+2*m)];}catch(Exception ee){
                            try{marraythisP[12]=dubthisslice[j+m];}catch(Exception eee){marraythisP[12]=dubthisslice[j];}}}


                    try{marraythisX[0] = dubthisslice[(j-(m+1))];}catch(Exception e){marraythisX[0]=dubthisslice[j];}
                    try{marraythisX[1] = dubthisslice[(j-(m-1))];}catch(Exception e){marraythisX[1]=dubthisslice[j];}
                    try{marraythisX[2] = dubthisslice[j];        }catch(Exception e){marraythisX[2]=dubthisslice[j];}
                    try{marraythisX[3] = dubthisslice[(j+(m-1))];}catch(Exception e){marraythisX[3]=dubthisslice[j];}
                    try{marraythisX[4] = dubthisslice[(j+(m+1))];}catch(Exception e){marraythisX[4]=dubthisslice[j];}


                    try{marraythisX[5] = dubthisslice[(j-(2*m+2))];}catch(Exception e){
                        try{marraythisX[5]=dubthisslice[j-(m+1)];}catch(Exception ee){marraythisX[5]=dubthisslice[j];}}
                    try{marraythisX[6] = dubthisslice[(j-(2*m-2))];}catch(Exception e){
                        try{marraythisP[6]=dubthisslice[j-(m-1)];}catch(Exception ee){marraythisX[6]=dubthisslice[j];}}
                    try{marraythisX[7] = dubthisslice[(j+(2*m-2))];}catch(Exception e){
                        try{marraythisX[7]=dubthisslice[j+(m-1)];}catch(Exception ee){marraythisX[7]=dubthisslice[j];}}
                    try{marraythisX[8] = dubthisslice[(j+(2*m+2))];}catch(Exception e){
                        try{marraythisX[8]=dubthisslice[j+(m+1)];}catch(Exception ee){marraythisX[8]=dubthisslice[j];}}

                    try{marraythisX[9] = dubthisslice[(j-(3*m+3))];}catch(Exception e){
                        try{marraythisX[9] = dubthisslice[(j-(2*m+2))];}catch(Exception ee){
                            try{marraythisX[9]=dubthisslice[j-(m+1)];}catch(Exception eee){marraythisX[9]=dubthisslice[j];}}}
                    try{marraythisX[10] = dubthisslice[(j-(3*m-3))];}catch(Exception e){
                        try{marraythisX[10] = dubthisslice[(j-(2*m-2))];}catch(Exception ee){
                            try{marraythisX[10]=dubthisslice[j-(m-1)];}catch(Exception eee){marraythisX[10]=dubthisslice[j];}}}
                    try{marraythisX[11] = dubthisslice[(j+(3*m-3))];}catch(Exception e){
                        try{marraythisX[11] = dubthisslice[(j+(3*m-3))];}catch(Exception ee){
                            try{marraythisX[11]=dubthisslice[j+(m-1)];}catch(Exception eee){marraythisX[11]=dubthisslice[j];}}}
                    try{marraythisX[12] = dubthisslice[(j+(3*m+3))];}catch(Exception e){
                        try{marraythisX[12] = dubthisslice[(j+(2*m+2))];}catch(Exception ee){
                            try{marraythisX[12]=dubthisslice[j+(m+1)];}catch(Exception eee){marraythisX[12]=dubthisslice[j];}}}

                    medianarray[0] = median(marraythisX);
                    medianarray[1] = median(marraythisP);
                    medianarray[2] = dubthisslice[j];
                    filteredslice[j] = median(medianarray);
                }
                for(int h=0;h<dimension;++h)dubthisslice[h]=filteredslice[h];
            }
        }

        if(atebit){
            newslice8 = double2byte(filteredslice);
            ip2 = new ByteProcessor(m, n);
            ip2.setPixels(newslice8);
        }
        else{
            newslice = double2short(filteredslice);
            ip2 = new ShortProcessor(m, n);
            ip2.setPixels(newslice);
        }

        return ip2;
    }
    private static double median(double array[]){
        Arrays.sort(array);
        int len = array.length;
        if(len%2==0)return((array[(len/2)-1]+array[len/2])/2);
        else return array[((len-1)/2)];
    }
    private short[] double2short(double array[]){
        short shortarray[] = new short[array.length];
        for(int j=0;j<array.length;++j)shortarray[j] = (short)array[j];
        return shortarray;
    }
    private double[] short2double(short array[]){
        double doublearray[] = new double[array.length];
        for(int j=0;j<array.length;++j)doublearray[j] = (double)(0xffff & array[j]);
        return doublearray;
    }
    private byte[] double2byte(double array[]){
        byte bytearray[] = new byte[array.length];
        for(int j=0;j<array.length;++j)bytearray[j] = (byte)array[j];
        return bytearray;
    }
    private double[] byte2double(byte array[]){
        double doublearray[] = new double[array.length];
        for(int j=0;j<array.length;++j)doublearray[j] = (double)(0xff & array[j]);
        return doublearray;
    }
}

