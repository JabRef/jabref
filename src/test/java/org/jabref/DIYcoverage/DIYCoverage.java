package org.jabref.DIYcoverage;

public class DIYCoverage {
    public static boolean[][] takenTest = new boolean[4][1000]; /*ASSI3: Temporary array for branch coverage DIY */

    public static void cleanArray(int methodIdx){
        takenTest[methodIdx] = new boolean[1000];
    }
    public static void printAll(){
        for(int i = 0; i < takenTest.length; i++){
            for(int j = 0; j < takenTest[i].length; j++){
                System.out.println("Method number: " + i + " Number: " + j + " Status: " + takenTest[i][j]);
            }
        }
    }

    public static void printAllTrue(){
        for(int i = 0; i < takenTest.length; i++){
            for(int j = 0; j < takenTest[i].length; j++){
                if(takenTest[i][j]) {
                    System.out.println("Method number: " + i + " Number: " + j + " Status: " + takenTest[i][j]);
                }
            }
        }
    }
}
