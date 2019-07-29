package com.company;

import java.io.File;

public class DocumentMatch{
    public static File[] Documents;
    public int document;
    public int count;
    public double frequency;

    public DocumentMatch(int document){
        this.document = document;
        this.count = 0;
    }
    public int getDocument(){
        return document;
    }
    public void increment(){
        count++;
    }

    public int hashCode(){
        return this.document;
    }

    public void computeFrequency(int totalCount){
        frequency = (double)count/totalCount;
    }

    @Override
    public String toString() {
        return String.format("document %s, count %s", Documents[document].getName(), count);
    }
}
