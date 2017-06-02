package br.com.germanno

import java.util.*

/**
 * @author Germanno Domingues - germanno.domingues@gmail.com
 * @since 1/10/17 2:07 AM
 */
abstract class KeneticsManager<in TGene, TIndividual : Individual, TIndividualOperator : IndividualOperator<TGene, TIndividual>>(
        private val genes: List<TGene>,
        private val individualOperator: TIndividualOperator,
        private val individualCreator: () -> TIndividual,
        private val maxTimeRunning: Int = 30000,
        private val eliminationPercentage: Float = 0.2F,
        private val freshIndividualsPercentage: Float = 0.4F,
        private val mutationPercentage: Float = 0.05F
) {
    private val random = Random()
    private var runs = 0
    private var timeTotal: Long = 0
    private var timeSelection: Long = 0
    private var timeFreshIndividuals: Long = 0
    private var timeCrossover: Long = 0
    private var timeMutation: Long = 0
    private var timeFitness: Long = 0
    private var timeSorting: Long = 0
    private var iterations: Int = 0

    private lateinit var bestFitIndividual: TIndividual

    fun doTheEvolution(population: MutableList<TIndividual>, printTimes: Boolean = false): TIndividual {
        val start = System.currentTimeMillis()
        val populationSize = population.size
        val quantityToKill = (populationSize * eliminationPercentage).toInt()
        val quantityOfFreshNew = (quantityToKill * freshIndividualsPercentage).toInt()
        val crossoverSize = quantityToKill - quantityOfFreshNew
        val mutationSize = (populationSize * mutationPercentage).toInt()
        val freshIndividuals = (0 until populationSize).map { individualCreator() }

        var evolvedPopulation = population.calculateFitness().sortIndividuals()

        do {
            iterations++

            evolvedPopulation =
                    evolvedPopulation
                            .doSelection(quantityToKill)
                            .addFreshIndividuals(freshIndividuals, quantityOfFreshNew)
                            .doCrossover(crossoverSize)
                            .doMutation(mutationSize)
                            .calculateFitness()
                            .sortIndividuals()

            bestFitIndividual = evolvedPopulation.first()

            if (individualOperator.isGoal(bestFitIndividual)) break

        } while (System.currentTimeMillis() - start < maxTimeRunning)

        timeTotal += System.currentTimeMillis() - start
        runs++

        if (printTimes) {
            printMeasuredTimes()
        }

        return bestFitIndividual
    }

    private fun MutableList<TIndividual>.doSelection(toBeRemoved: Int): MutableList<TIndividual> {
        val startSelection = System.currentTimeMillis()

        val toBeSelected = size - toBeRemoved
        val elite = (toBeSelected * 0.1).toInt()
        val listIndices = (0 until elite).toMutableList()

        while (listIndices.size < toBeSelected) {
            val first = elite + random.nextInt(size - elite)
            val second = elite + random.nextInt(size - elite)

            if (first != second && !listIndices.contains(first) && !listIndices.contains(second)) {
                val fitnessFirst = get(first).fitness
                val fitnessSecond = get(second).fitness

                if (fitnessFirst > fitnessSecond) {
                    listIndices.add(first)
                } else if (fitnessFirst < fitnessSecond) {
                    listIndices.add(second)
                } else {
                    listIndices.add(if (random.nextBoolean()) first else second)
                }
            }
        }

        val result = listIndices.map { this[it] }.toMutableList()

        timeSelection += System.currentTimeMillis() - startSelection

        return result
    }

    private fun MutableList<TIndividual>.addFreshIndividuals(freshIndividuals: List<TIndividual>,
                                                             quantityOfFreshNew: Int): MutableList<TIndividual> {
        val startFreshIndividuals = System.currentTimeMillis()
        val start = random.nextInt(freshIndividuals.size - quantityOfFreshNew)

        addAll(freshIndividuals.subList(start, start + quantityOfFreshNew))
        timeFreshIndividuals += System.currentTimeMillis() - startFreshIndividuals

        return this
    }

    private fun MutableList<TIndividual>.doCrossover(crossoverSize: Int): MutableList<TIndividual> {
        val startCrossover = System.currentTimeMillis()
        for (i in 0 until crossoverSize) {
            val first = randomIndividualIndex()
            var second: Int
            do second = randomIndividualIndex() while (second == first)

            add(individualOperator.crossover(this[first], this[second]))
        }
        timeCrossover += System.currentTimeMillis() - startCrossover

        return this
    }

    private fun MutableList<TIndividual>.doMutation(mutationSize: Int): MutableList<TIndividual> {
        val startMutation = System.currentTimeMillis()
        var i = mutationSize
        while (i > 0) {
            val gene = genes[random.nextInt(genes.size)]
            if (!individualOperator.mutate(randomIndividual(), gene)) {
                continue
            }
            i--
        }
        timeMutation += System.currentTimeMillis() - startMutation

        return this
    }

    private fun MutableList<TIndividual>.calculateFitness(): MutableList<TIndividual> {
        val startFitness = System.currentTimeMillis()
        forEach { individualOperator.calculateFitness(it) }
        timeFitness += System.currentTimeMillis() - startFitness

        return this
    }

    private fun MutableList<TIndividual>.sortIndividuals(): MutableList<TIndividual> {
        val startSorting = System.currentTimeMillis()
        sortByDescending { it.fitness }
        timeSorting += System.currentTimeMillis() - startSorting

        return this
    }

    //region Helpers

    private fun printMeasuredTimes() {
        println()
        println("----------- Kenetics statistics ----------- ")
        println("Time total: ${timeTotal / runs.toDouble()}ms")
        println("Time selection: ${timeSelection / runs.toDouble()}ms")
        println("Time crossover: ${timeCrossover / runs.toDouble()}ms")
        println("Time mutation: ${timeMutation / runs.toDouble()}ms")
        println("Time calculateFitness: ${timeFitness / runs.toDouble()}ms")
        println("Time sorting: ${timeSorting / runs.toDouble()}ms")
        println("Iterations: ${iterations / runs}")
        println("---------------------- ")
        println()
    }

    private fun MutableList<TIndividual>.randomIndividual(): TIndividual {
        return this[randomIndividualIndex()]
    }

    private fun MutableList<TIndividual>.randomIndividualIndex(): Int {
        return random.nextInt(size)
    }

    //endregion

}