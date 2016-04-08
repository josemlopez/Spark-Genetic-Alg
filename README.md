# Spark-Genetic-Alg

#### What it's a GA: 
https://en.wikipedia.org/wiki/Genetic_algorithm 

#### The problem of a GA over SPARK:
A Genetic Algorithm needs to compare the Fitness of all the individuals to select the bests for the next generation and replace some other Individuals.
This task has a high cost because the population (RDD) have to be sent from the Workers to the Driver. The Driver must to compare the bests and then select the individuals to be replaced. 

An improvement to this implementation can be to use the partitions of RDD (population) to execute partially the build of the new generation.
Let's say we need the K best individuals in each generation of our population (population partitioned P times):
 
 1. Send the K best Individuals of each P partition to the driver 
 2. The driver will select the K best of K*P total bests Individuals.
 3. Replace the K best Individuals for any K other Individuals from the original population to create a new generation

The problem with this solution is the "replacement": how can we replace any K Individuals for the best K? There is only one way: to collect the RDD, and replace the K Individuals.

From another point of view with SPARK there is the chance to execute more than a population at same time.

#### Solution Given
This example try to give a practical solution for these two problem: 
 
  1. The need of sending the population to the Driver 
  2. Keep more than one population at same time.

With this solution you will have a Population with K Partitions. Each partition will be an isolate population where all the generations will be executed in the worker side. There is no need to send the population to the driver until the conclusion of the execution.

# Code
The code is based on a class called GA that contains the Genetic Algorithm: 

Inside GA there is a method called: *selectAndCrossAndMutatePopulation*.  

Why in this first attempt I'm  trying to do all the transformations to the population in the same place?: optimization. I'll create another version with an approach less "optimized" and less "monolithic" to compare the times and the results.

How it's applied the functions to each Partition?:

    val populationRDD = population.mapPartitionsWithIndex(selection, preservesPartitioning = true)
    

## Functions
Inside the class GeneticJob there are two Seq:

    val selections: Selector[SelectionFunction] = new Selector(Seq(new SelectionNaive, new SelectionRandom, new SelectionWrong))
    val mutations: Selector[MutationFunction] = new Selector(Seq(new OnePointMutation, new OnePointMutation, new NoMutation))
    
These Functions will be applied rotatably to each Partition in the RDD (each population): 

  1. Partition == 0 => will be applied (SelectionNaive && OnePointMutation) 
     Partition == 1 => will be applied (SelectionRandom && OnePointMutation) 
     Partition == 3 => will be applied (SelectionWrong && NoMutation) 
     Partition == 4 => the same than Partition == 0 
     ...

Using this approach, this implementation makes the selection of the best individuals for each partition (like it was a mini-population), crossover the parents and make the replacement, all in the worker side. 

##### Future

An improvement in terms of convergence can be to broadcast the N bests of each partition and cross that individuals. That approach can be an improvement or not, this will be tested and I'll make a comparison between implementations. 

Another improvement is to include a module that get statistics from the population in each Generation (near future)

# Use
git pull
sbt run 



