package com.workshop.iot.linescaner;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Created by amitp on 11/2/17.
 */

public class ScanUtil {
    private static final double DARKNESS_THRESOLD=0.6;

    public static Box getLineCordinate(Bitmap baseBitmap) {
        getdarknessArray(baseBitmap);
        Line lineTop = getMaxBrightLine(baseBitmap,baseBitmap.getWidth()/8);
        Line lineBottom = getMaxBrightLine(baseBitmap,baseBitmap.getWidth()/2);
        return new Box(lineTop,lineBottom,(baseBitmap.getWidth()*300)/(8*baseBitmap.getHeight()));
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
    private static Line getMaxBrightLine(Bitmap bitmap, int verticlePosition){
        Line result=new Line(0,0);
        for(int index=0;index<bitmap.getHeight();index++){
            if(!isColorDark(bitmap.getPixel(verticlePosition,index))){
                int startIndex=index;
                while((!isColorDark(bitmap.getPixel(verticlePosition,index)))&& (index<bitmap.getHeight()-1)){
                    index++;
                }
                int endIndex=index;
                if(result.length<(endIndex-startIndex)*100/bitmap.getHeight()){
                    result=new Line((endIndex-startIndex)*100/bitmap.getHeight(),100-((endIndex+startIndex)*50)/bitmap.getHeight());
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
