package com.workshop.iot.linescaner;

/**
 * Created by amitp on 11/6/17.
 */

public class Box {
    Line l1,l2;
    int verticleGape;
    int inclination;
    int centre;
public Box(){
    this.l1 = new Line(0,50);
    this.l2 = new Line(0,50);
    verticleGape=25;
    inclination=(l1.centre -l2.centre)*100/verticleGape;
    centre=(l1.centre +l2.centre)/2;
}
    public Box(Line l1, Line l2,int gap) {
        this.l1 = l1;
        this.l2 = l2;
        verticleGape=gap;
        inclination=(l1.centre -l2.centre)*100/verticleGape;
        centre=(l1.centre +l2.centre)/2;
    }
}
