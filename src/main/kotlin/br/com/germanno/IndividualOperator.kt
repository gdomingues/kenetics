package br.com.germanno

/**
 * @author Germanno Domingues - germanno.domingues@gmail.com
 * @since 1/10/17 2:02 AM
 */
interface IndividualOperator<in TGene, TIndividual : Individual> {

    fun calculateFitness(individual: TIndividual): Int

    fun crossover(individual: TIndividual, other: TIndividual): TIndividual

    fun mutate(individual: TIndividual, gene: TGene): Boolean

    fun isGoal(individual: TIndividual): Boolean

    fun clone(individual: TIndividual): TIndividual

}