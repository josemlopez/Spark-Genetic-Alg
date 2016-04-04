# Spark-Genetic-Alg

#### What it's a GA: 
https://en.wikipedia.org/wiki/Genetic_algorithm 

#### The problem of a GA over SPARK:
A Genetic Algorithm needs to compare the Fitness of all the individuals to select the bests (or any :depends of the type of the selection function used) for the next generation and replace the worst (or any depends of the replacement fucntion used) with the individuals of the new Generation.
 This task has a high cost because we need to send the population to the driver, compare the bests and select the individuals. 
An improvement to this implementation can be the next one: 
Let's say we need the K best individuals of the population, and we have P partitions.
 
 1. send the K best of each partition to the driver 
 2. the driver will select the K best of K*P of the total individuals.

The problem with this solution is the "replacement": how can we replace the K worst for the K best if that k worst can be distributed by any of the partitions?

In this case the solution is not easy, we need to be able to remember the partition from where each k worst individual comes, to be able to do the replacement: this is not a practical solution. 
Finally the same problem arise: we need to collect the population to the driver. But, what happen if the population or the individuals are very large to do a "collect" to the driver?


The implementation in this example try to give a practical solution for this problem.

The idea is to create a Genetic Algorithm over Spark optimizing the steps as possible and trying to avoid the need of a collect.

# code
The code is based on a class called GA that contains the Genetic Algorithm: 

Inside GA there is a method called: *selectAndCrossAndMutatePopulation*.  Why in this first attempt I'm  trying to do all the transformations to the population in the same place?: optimization. I'll create another version with an approach less "optimized" and less "monolithic" to compare the times and the results.

The idea behind this approach is to make all the transformations in the worker. 

The selection of the best individuals in our population is a way to do the selection but there are other ways. 

Selecting the best fitness for the next generation is called "elitism" and sometimes the algorithms stuck in to a not good solution because of it. A tool for create an spurious is the mutation or select only some of the best al let the rest of the population exactly like it's. 

SPARK. 
Spark is a parallelized engine that is capable to execute transformations and operations over a immutable collection called RDD. 
That RDD are partitioned over the workers in order to be able to execute the transformations in a parallel way. 

This implementation of a GA uses the fact that you can make transformations on the partitions:
```
population.mapPartitions(selection, preservesPartitioning = true)
```

Using this approach, this implementation makes the selection of the best individuals for each partition (like it was a mini-population), 
crossover the parents and make the replacement of the worst individuals for the new ones generated. 

The idea is to calculate the solution using K populations 

An improvement can be to broadcast the N bests of each partition and cross that individuals. That approach can be an improvement or not, this will be tested and I'll make a comparison between implementations. 


# Note
If you are asking yourself where are the rest of the types of Selection, Replacement an mutation: all that will come in a future.
 
For the moment the GA implemented is:
####Selection: 
Deterministic: get the K best individuals. 
Although this is not a good idea if we want a good solution and not a local optimum, this implementation converge quickly. 
####Replacement:
Deterministic: get the worst K individuals and replace by the k created in the crossover
####Mutation:
One point mutation with a given probability 
####Crossover:
One point crossover that creates two children
