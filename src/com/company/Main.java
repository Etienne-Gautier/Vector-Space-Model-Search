package com.company;

import java.time.Clock;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String query;
        Clock time = Clock.systemUTC();
        DocumentIndexer indexer = new DocumentIndexer(args[0], args[1]);
        indexer.ReadAllDocuments();
        indexer.findMostCommonWords(1000);
        indexer.computeWeights();
        do{
            System.out.println("please enter you query");
            int[] result = new int[10];
            query = scanner.nextLine();
            long t1 = time.millis();
            // Non optimized algorithm
            for(int i=0;i<1000;i++) {
                result = indexer.searchQuery(query);
            }
            t1 = time.millis() - t1;
            System.out.println(String.format("Non optimized algorithm took %s ms", (double)t1/1000));


            t1 = time.millis();
            //Optimized algorithm
            for(int i=0;i<1000;i++) {
                result = indexer.searchQueryV2(query);
            }
            t1 = time.millis() - t1;
            System.out.println(String.format("Optimized algorithm took : %s ms", (double)t1/1000));

            for(int i: result){
                System.out.println(indexer.Documents[i].getName());
            }
            System.out.println("Do you want to continue? (y/n)");
        }while(scanner.nextLine().equals("y"));
    }
}
