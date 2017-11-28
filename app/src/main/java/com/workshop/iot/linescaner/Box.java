package com.workshop.iot.linescaner;

/**
 * Created by amitp on 11/6/17.
 */

public class Box {
    Line l1,l2;
    int verticleGape;
    int inclination;
    int centre;
    boolean isPath;
    boolean onMarker;
public Box(){
    this.l1 = new Line(10,50);
    this.l2 = new Line(10,50);
    verticleGape=25;
    inclination=(l1.centre -l2.centre)*100/verticleGape;
    centre=(l1.centre +l2.centre)/2;
    isPath=isValidQudralateral();
    onMarker=false;
}
    public Box(Line l1, Line l2,int gap,boolean onMarker) {
        this.l1 = l1;
        this.l2 = l2;
        verticleGape=gap;
        inclination=(l1.centre -l2.centre)*100/verticleGape;
        centre=(l1.centre +l2.centre)/2;
        isPath=isValidQudralateral();
        this.onMarker=onMarker;
    }
    private boolean isValidQudralateral(){
    if(l1.length>5 && l2.length>5){
        if(l1.length>50 || l2.length>50){
            return true;
        }
        if((l1.length*1.0/l2.length)>0.5 && (l1.length*1.0/l2.length)< 1.5){
            return true;
        }
    }
    return false;
    }
}
