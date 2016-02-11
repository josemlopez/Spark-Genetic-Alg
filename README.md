# Spark-Genetic-Alg

The idea is to create a Genetic Algorithm over Spark optimizing the steps as possible. 

The code is based on a class called GA that contains the Genetic Algorithm: 

Inside GA there is a method called: *selectAndCrossAndMutatePopulation*.  Why in this first attempt I'm  trying to do all the transformations to the population in the same place?: optimization. I'll create another version with an approach less "optimized" and less "monolithic" to compare the times and the results.

The idea behind this approach is to make all the transformations in the worker. Our population in the RDD is partitioned, and a Genetic Algorithm needs to compare the Fitness between all the individual to select the bests for the next generation. This task has a high cost because we need to send the population to the driver, compare the bests and select the individuals. 
The selection of the best individuals in our population is a way to do the selection but there are other ways. 

Selecting the best fitness for the next generation is called "elitism" and sometimes the algorithms stuck in to a not good solution because of it. A tool for create an spurious is the mutation or select only some of the best al let the rest of the population exactly like it's. 

SPARK. 
Spark is a parallelized engine that is capable to execute transformations and operations over a collection called RDD. 
That collections are partitioned over the workers in order to make the transformations. 

This implementation of a GA uses the fact that you can make transformations over the partitions:
```
//We use a map for each partition, this way the execution is all in the worker side.
//We have to give some concessions:
population.mapPartitions(selection, preservesPartitioning = true)
```
Using this approach I make the selection of the best individuals for that partition (like it was a mini-population), I cross the bests of that partition. 
An improvement can be to broadcast the N bests of each partition and cross that individuals. That approach can be an improvement or not, this will be tested and I'll make a comparison between implementations. 
