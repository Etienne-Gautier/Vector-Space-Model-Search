------------------INFORMATION RETRIEVAL: Assignment 2--------------
Requirements:

	This assignment has been coded in java 10. It uses only libraries from the JRE.

NOTE:
	All the code is contained in the package com.company
About the program:
	The code uses vector space model retrieval mode.
	This code has been optimized for fast query times.
	There are two different implementation of the query search.

		-Non optimized: 
			does not use the inverted index representation. Typical execution time of a query is ~0.5 ms.
		-Optimized: 
			uses the inverted index to narrow down the query to potential documents (documents that contain the query words).
			Typical execution time of a query is ~0.3 ms

Launching the program:

	To run it, the program takes 2 arguments:
		-The path of the file containing the common words (i.e. dataset/common_words.txt)
		-The path of the folder containing all the documents to index (i.e. dataset/cranfieldDocs)

Using the program:

	This program runs with a simple CLI loop in 2 steps:
		1) Enter your query:
		2) Do you want to continue? (y/n)

