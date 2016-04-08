# Spark-Genetic-Alg

#### What it's a GA: 
https://en.wikipedia.org/wiki/Genetic_algorithm 

#### The problem of a GA over SPARK:
A Genetic Algorithm needs to compare the Fitness of all the individuals to select the bests for the next generation and replace some other Individuals.
This task has a high cost because we need to send the population to the driver, compare the bests and select the individuals to be replaced. 

An improvement to this implementation can be to use the partitions of RDD (population): 
Let's say we need the K best individuals in each generation of our population (population partitioned P times):
 
 1. Send the K Individuals of each P partition to the driver 
 2. The driver will select the K best of K*P total bests Individuals.
 3. Replace the K best Individuals for any K other Individuals to create a new generation

The problem with this solution is the "replacement": how can we replace any K Individuals for the best K? There is only one way: to collect the RDD, and replace the K Individuals.

Another thing to take advantage is the chance to execute more than a population at same time.

#### Solution Given
This example try to give a practical solution for all that problems.

The idea is to create a Genetic Algorithm over Spark optimizing the steps as possible and trying to avoid the needs of any collect to the Driver.

For that solution I have a Population with K Partitions. Each partition will be an isolate population where all the generations will be executed in the worker side. There is no need to transport the population to the driver until the conclusion of the execution.

# Code
The code is based on a class called GA that contains the Genetic Algorithm: 

Inside GA there is a method called: *selectAndCrossAndMutatePopulation*.  

Why in this first attempt I'm  trying to do all the transformations to the population in the same place?: optimization. I'll create another version with an approach less "optimized" and less "monolithic" to compare the times and the results.

SPARK. 
Spark is a parallelized engine that is capable to execute transformations and operations over a immutable collection called RDD. 

That RDD are partitioned over the workers in order to be able to execute the transformations in a parallel way. 

This implementation of a GA uses the fact that you can make transformations on the partitions:
```
population.mapPartitions(selection, preservesPartitioning = true)
```

Using this approach, this implementation makes the selection of the best individuals for each partition (like it was a mini-population), 
crossover the parents and make the replacement, all in the worker side. 

The idea is to calculate the solution using K populations 

##### Future
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


# Use
git pull
sbt run 



