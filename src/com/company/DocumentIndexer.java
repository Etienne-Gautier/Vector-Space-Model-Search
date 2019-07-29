package com.company;


import java.util.*;
import java.nio.charset.Charset;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class DocumentIndexer {
    private HashSet<String> StopWords;
    private HashMap<String, ArrayList<DocumentMatch>> wordIndex;
    public File[] Documents;
    private String[] topTerms;
    private HashMap<String, Integer> topTermsMap;
    private double[][] weights;
    private Set<String> queryWords;

    public DocumentIndexer(String stopWordsPath, String documentFolder){
        this.ReadStopWords(stopWordsPath);
        Documents = new File(documentFolder).listFiles();
        wordIndex = new HashMap<>();
        DocumentMatch.Documents = Documents;
        topTerms = new String[1000];
        topTermsMap = new HashMap<>();
        weights = new double[Documents.length][1000];
    }

    public void ReadAllDocuments(){
        for(int i=0; i<Documents.length; i++){
            this.ReadDocument(i);
        }
    }

    public void findMostCommonWords(int topN){
        ArrayList<Map.Entry<String, Integer>> wordCounts = new ArrayList<>();
        int totalCount;
        // Step 1: compute the total counts
        for (Map.Entry<String, ArrayList<DocumentMatch>> entry : wordIndex.entrySet()) {
            totalCount = 0;
            for(DocumentMatch match: entry.getValue()){
                totalCount += match.count;
            }
            wordCounts.add(new AbstractMap.SimpleEntry<>(entry.getKey(), totalCount));
        }
        // Step 2: get the top elements
        Comparator<Map.Entry<String, Integer>> entryComparator = Comparator.comparing(Map.Entry::getValue);
        wordCounts.sort(entryComparator.reversed());
        String word;
        // Step 3: fill the word list
        for(int i=0; i<1000; i++){
            word = wordCounts.get(i).getKey();
            topTerms[i] = word;
            topTermsMap.put(word, i);
        }
    }

    public ArrayList<Map.Entry<Integer, Double>> buildQueryVector(String query){
        // Step 1: read query
        String[] queryTerms = getWords(query);

        // Step 2: count words
        DocumentMatch currentMatch;
        HashMap<String, DocumentMatch> queryCounts = new HashMap<>();
        int validWordInDocumentCount = 0;
        for (String word : queryTerms) {
            if (!StopWords.contains(word)) {
                validWordInDocumentCount++;
                if (queryCounts.containsKey(word)) {
                    queryCounts.get(word).increment();
                } else {
                    currentMatch = new DocumentMatch(-1);
                    currentMatch.increment();
                    queryCounts.put(word, currentMatch);
                }
            }
        }
        // Step 3: compute frequencies
        for(DocumentMatch match: queryCounts.values()){
            match.computeFrequency(validWordInDocumentCount);
        }

        // Step 4: build query vector
        ArrayList<Map.Entry<Integer, Double>> queryWordWeights = new ArrayList<>();
        this.queryWords = queryCounts.keySet();
        for(String word: this.queryWords) {
            if (topTermsMap.containsKey(word)) {
                queryWordWeights.add(new AbstractMap.SimpleEntry<>(topTermsMap.get(word), idf(word) * queryCounts.get(word).frequency));
            }
        }
        // Step 5 Normalize the query vector
        final double totalWeight = Math.sqrt(queryWordWeights.stream().mapToDouble(entry -> Math.pow(entry.getValue(), 2)).sum());
        queryWordWeights.forEach(integerDoubleEntry -> integerDoubleEntry.setValue(integerDoubleEntry.getValue()/totalWeight));


        return queryWordWeights;
    }

    void computeWeights(){
        double termWeight;
        ArrayList<DocumentMatch> currentTermMatches;
        for(int i=0; i<1000; i++){
            currentTermMatches = wordIndex.get(topTerms[i]);
            termWeight = idf(currentTermMatches);
            for(DocumentMatch match: currentTermMatches){
                weights[match.document][i] = termWeight * match.frequency;
            }
        }

        // Step 2: computeFrequency
        for(int i=0; i<weights.length; i++){
            final double[] currentWordVector = weights[i];
            final double documentWeight = Math.sqrt(DoubleStream.of(currentWordVector).map(val -> Math.pow(val, 2)).sum());
            weights[i] = IntStream.range(0, 1000)
                    .mapToDouble(j->
                        {
                            double result = currentWordVector[j]/documentWeight;
                            return Double.isNaN(result)? 0.: result;
                        }).toArray();
        }

    }

    /**
     * Measure of term specificity
     * @param term
     * @return
     */
    public double idf(String term){
        return idf(wordIndex.get(term));
    }
    private double idf(ArrayList<DocumentMatch> currentTermList){
        return Math.log((double)Documents.length/currentTermList.size());
    }

    /**
     * Measure of term importance in document
     * @param term
     * @param document
     * @return
     */
    public double tf(String term, int document){
        ArrayList<DocumentMatch> currentList = wordIndex.get(term);
        int location = Collections.binarySearch( currentList, new DocumentMatch(document),Comparator.comparing(DocumentMatch::getDocument));
        return currentList.get(location).frequency;
    }

    private void ReadStopWords(String stopWordPath){
        StopWords = new HashSet<>(readAllLines(stopWordPath, Charset.defaultCharset()));
    }
    private void ReadDocument(int documentIndex){
        String doc = readFile(Documents[documentIndex].getPath(), Charset.defaultCharset());
        String[] words = getWords(doc);
        DocumentMatch currentMatch;
        int validWordInDocumentCount = 0;
        //To minimize index lookup
        ArrayList<DocumentMatch> currentTermMatches;
        // To compute frequencies
        ArrayList<DocumentMatch> currentDocMatches = new ArrayList<>();
        for (String word : words) {
            if (!StopWords.contains(word)) {
                validWordInDocumentCount++;
                if (wordIndex.containsKey(word)) {
                    currentTermMatches = wordIndex.get(word);
                } else {
                    currentTermMatches = new ArrayList<>();
                    wordIndex.put(word, currentTermMatches);
                }
                if (!currentTermMatches.isEmpty() && currentTermMatches.get(currentTermMatches.size() - 1).document == documentIndex) {
                    currentTermMatches.get(currentTermMatches.size() - 1).increment();
                } else {
                    currentMatch = new DocumentMatch(documentIndex);
                    currentMatch.increment();
                    currentTermMatches.add(currentMatch);
                    currentDocMatches.add(currentMatch);
                }
            }
        }
        for(DocumentMatch match: currentDocMatches){
            match.computeFrequency(validWordInDocumentCount);
        }
    }

    private String[] getWords(String doc){
        doc = doc.replaceAll("<[^>]*>", "");

        doc = doc.toLowerCase();
        return Pattern.compile("[a-z0-9]+")
                .matcher(doc)
                .results()
                .map(MatchResult::group)
                .toArray(String[]::new);
    }
    private static String readFile(String path, Charset encoding)
    {
        String result;
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(path));
            result = new String(encoded, encoding);
        }
        catch(IOException ex){
            result = "";
        }
        return result;
    }

    private static List<String> readAllLines(String path, Charset encoding){
        List<String> result;
        try{
            result = Files.readAllLines(Paths.get(path), encoding);
        }
        catch (IOException ex){
            result = new ArrayList<>();
        }
        return result;
    }


    public int[] searchQuery(String query){
        ArrayList<Map.Entry<Integer, Double>> queryVector = buildQueryVector(query);
        // Step 2: compute similarities
        ArrayList<Map.Entry<Integer, Double>> similarities = new ArrayList<>(Documents.length);
        for(int i=0; i<Documents.length; i++){
            final double[] currentDocumentWeight = weights[i];
            similarities.add(new AbstractMap.SimpleEntry<>(i, queryVector
                    .stream()
                    .mapToDouble(queryItem -> queryItem.getValue() * currentDocumentWeight[queryItem.getKey()])
                    .sum()));
        }
        // Step 3: get the top elements
        Comparator<Map.Entry<Integer, Double>> entryComparator = Comparator.comparing(Map.Entry::getValue);
        PriorityQueue<Map.Entry<Integer, Double>> priorityQueue = new PriorityQueue<>(similarities.size(), entryComparator.reversed());
        priorityQueue.addAll(similarities);
        return IntStream.range(0,10).map(i -> priorityQueue.poll().getKey()).toArray();
    }

    public int[] searchQueryV2(String query){
        //Step 1: build query
        ArrayList<Map.Entry<Integer, Double>> queryVector = buildQueryVector(query);
        //Step 2: query words individually in the index
        HashSet<Integer> candidateDocuments = new HashSet<>();
        for(String word: this.queryWords){
            if(wordIndex.containsKey(word)){
                wordIndex.get(word)
                        .stream()
                        .forEach(match -> candidateDocuments.add(match.document));
            }
        }

        // Step 3: compute similarities only on potential documents
        ArrayList<Map.Entry<Integer, Double>> similarities = new ArrayList<>(Documents.length);
        for(int i: candidateDocuments){
            final double[] currentDocumentWeight = weights[i];
            similarities.add(new AbstractMap.SimpleEntry<>(i, queryVector
                    .stream()
                    .mapToDouble(queryItem -> queryItem.getValue() * currentDocumentWeight[queryItem.getKey()])
                    .sum()));
        }
        // Step 5: sort and get the top elements
        Comparator<Map.Entry<Integer, Double>> entryComparator = Comparator.comparing(Map.Entry::getValue);
        similarities.sort(entryComparator.reversed());
        return  IntStream.range(0, 10).map(i -> similarities.get(i).getKey()).toArray();
    }


}
