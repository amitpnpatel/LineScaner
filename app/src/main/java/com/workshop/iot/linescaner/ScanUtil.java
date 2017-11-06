package com.workshop.iot.linescaner;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.provider.Settings;

/**
 * Created by amitp on 11/2/17.
 */

public class ScanUtil {
    private static final double DARKNESS_THRESOLD=0.6;

    public static Square getLineCordinate(Bitmap baseBitmap) {
        getdarknessArray(baseBitmap);
        Square square=getMaxBrightSquare(baseBitmap);
        System.out.println("c:"+square.center+" l:"+square.length);
        return square;
    }

    private static int[] getdarknessArray(Bitmap bitmap){
        int[] result= new int[11];
        int height=bitmap.getWidth();
        int width=bitmap.getHeight();
        for(int index=0;index<result.length;index++){
            result[index]=getBritnessCount(bitmap, height/4,(width*(index+1)/(result.length+1)),2);
        }
        printArray(result);
        return result;
    }
    private static int getBritnessCount(Bitmap bitmap, int vcordinate,int hcordinate,int squareLength){
        int result=0;
        for(int vindex=vcordinate-squareLength;vindex<vcordinate+squareLength;vindex++){
            for(int hindex=hcordinate-squareLength;hindex<hcordinate+squareLength;hindex++){
                if(!isColorDark(bitmap.getPixel(vindex,hindex))){
                    result++;
                }
            }
        }
        return result;
    }
    private static Square getMaxBrightSquare(Bitmap bitmap){
        Square result=new Square(0,0);
        for(int index=0;index<bitmap.getHeight();index++){
            if(!isColorDark(bitmap.getPixel(bitmap.getWidth()/4,index))){
                int startIndex=index;
                while((!isColorDark(bitmap.getPixel(bitmap.getWidth()/4,index)))&& (index<bitmap.getHeight()-1)){
                    index++;
                }
                int endIndex=index;
                if(result.length<(endIndex-startIndex)*100/bitmap.getHeight()){
                    result=new Square((endIndex-startIndex)*100/bitmap.getHeight(),100-((endIndex+startIndex)*50)/bitmap.getHeight());
                }
            }
        }
        return result;
    }
    private static void printArray(int[] array){
        for (int element:array) {
            System.out.print(element);
            System.out.print(" ");
        }
        System.out.println(" ");
    }

    private static boolean isColorDark(int color){
        double darkness = getPixeldarkness(color);
        if(darkness<DARKNESS_THRESOLD){
            return false; // It's a light color
        }else{
            return true; // It's a dark color
        }
    }
    private static double getPixeldarkness(int color){
        double darkness = 1-(0.299* Color.red(color) + 0.587*Color.green(color) + 0.114*Color.blue(color))/255;
        return darkness;
    }
}
