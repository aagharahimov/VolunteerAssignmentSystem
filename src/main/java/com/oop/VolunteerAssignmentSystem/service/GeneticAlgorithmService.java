package com.oop.VolunteerAssignmentSystem.service;

import com.oop.VolunteerAssignmentSystem.model.Assignment;
import com.oop.VolunteerAssignmentSystem.model.AssignmentResult;
import com.oop.VolunteerAssignmentSystem.model.ServiceDetails;
import com.oop.VolunteerAssignmentSystem.model.Volunteer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class GeneticAlgorithmService {
    private static final Logger logger = LoggerFactory.getLogger(GeneticAlgorithmService.class);

    private static final int POPULATION_SIZE = 100;
    private static final int MAX_GENERATIONS = 200;
    private static final double MUTATION_RATE = 0.1; // Chance per volunteer assignment in a chromosome
    private static final double CROSSOVER_RATE = 0.7;
    private static final int TOURNAMENT_SIZE = 5;
    private static final int ND_PENALTY_SQUARED_BASE = 10; // For 10 * Nd^2, where Nd is number of services

    // Chromosome: A list of service IDs, one for each volunteer.
    // The index in the list corresponds to a volunteer.
    static class Chromosome {
        List<String> assignments; // assignments.get(i) is service for volunteer i
        double fitness; // Lower is better (cost)

        Chromosome(List<String> assignments) {
            this.assignments = assignments;
            this.fitness = Double.MAX_VALUE;
        }
    }

    public AssignmentResult run(Map<String, Volunteer> volunteerMap, List<ServiceDetails> serviceDetailsList) {
        logger.info("Starting GA with {} volunteers and {} services.", volunteerMap.size(), serviceDetailsList.size());
        if (volunteerMap.isEmpty() || serviceDetailsList.isEmpty()) {
            AssignmentResult emptyResult = new AssignmentResult();
            emptyResult.setMessage("Cannot run GA: No volunteers or services.");
            emptyResult.setTotalCost(0);
            emptyResult.setAssignments(Collections.emptyList());
            return emptyResult;
        }

        List<Volunteer> volunteers = new ArrayList<>(volunteerMap.values());
        Map<String, ServiceDetails> serviceMap = serviceDetailsList.stream()
                .collect(Collectors.toMap(ServiceDetails::getId, s -> s));

        List<Chromosome> population = initializePopulation(volunteers, serviceDetailsList);
        evaluatePopulation(population, volunteers, serviceMap, serviceDetailsList.size());

        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            List<Chromosome> newPopulation = new ArrayList<>();
            while (newPopulation.size() < POPULATION_SIZE) {
                Chromosome parent1 = selectParent(population);
                Chromosome parent2 = selectParent(population);
                Chromosome offspring1 = new Chromosome(new ArrayList<>(parent1.assignments));
                Chromosome offspring2 = new Chromosome(new ArrayList<>(parent2.assignments));

                if (ThreadLocalRandom.current().nextDouble() < CROSSOVER_RATE) {
                    crossover(parent1, parent2, offspring1, offspring2);
                }

                mutate(offspring1, serviceDetailsList);
                mutate(offspring2, serviceDetailsList);

                newPopulation.add(offspring1);
                if (newPopulation.size() < POPULATION_SIZE) {
                    newPopulation.add(offspring2);
                }
            }
            population = newPopulation;
            evaluatePopulation(population, volunteers, serviceMap, serviceDetailsList.size());

            // Optional: Elitism - carry over the best individual
            // population.sort(Comparator.comparingDouble(c -> c.fitness));
            // Chromosome bestThisGen = population.get(0);
            // logger.info("Generation {}: Best Fitness = {}", generation, bestThisGen.fitness);
        }

        population.sort(Comparator.comparingDouble(c -> c.fitness));
        Chromosome bestChromosome = population.get(0);
        logger.info("GA finished. Best fitness (total cost): {}", bestChromosome.fitness);

        return convertChromosomeToAssignmentResult(bestChromosome, volunteers, serviceMap, serviceDetailsList.size());
    }

    private List<Chromosome> initializePopulation(List<Volunteer> volunteers, List<ServiceDetails> services) {
        List<Chromosome> population = new ArrayList<>();
        List<String> serviceIds = services.stream().map(ServiceDetails::getId).collect(Collectors.toList());
        if (serviceIds.isEmpty()) {
            // Handle case where no services are available, though checked earlier
            return population;
        }

        for (int i = 0; i < POPULATION_SIZE; i++) {
            List<String> assignments = new ArrayList<>();
            for (int j = 0; j < volunteers.size(); j++) {
                // Randomly assign a service initially
                assignments.add(serviceIds.get(ThreadLocalRandom.current().nextInt(serviceIds.size())));
            }
            population.add(new Chromosome(assignments));
        }
        return population;
    }

    private void evaluatePopulation(List<Chromosome> population, List<Volunteer> volunteers, Map<String, ServiceDetails> serviceMap, int numDistinctServices) {
        for (Chromosome chromosome : population) {
            chromosome.fitness = calculateFitness(chromosome, volunteers, serviceMap, numDistinctServices);
        }
    }

    private double calculateFitness(Chromosome chromosome, List<Volunteer> volunteers, Map<String, ServiceDetails> serviceMap, int numDistinctServices) {
        double totalCost = 0;
        Map<String, Integer> serviceCounts = new HashMap<>();

        for (int i = 0; i < volunteers.size(); i++) {
            Volunteer volunteer = volunteers.get(i);
            String assignedServiceId = chromosome.assignments.get(i);
            serviceCounts.put(assignedServiceId, serviceCounts.getOrDefault(assignedServiceId, 0) + 1);

            int preferenceRank = 0; // 0 if not preferred
            if (volunteer.getPreferredServicesRanks() != null) {
                for (Map.Entry<Integer, String> entry : volunteer.getPreferredServicesRanks().entrySet()) {
                    if (entry.getValue().equals(assignedServiceId)) {
                        preferenceRank = entry.getKey();
                        break;
                    }
                }
            }

            if (preferenceRank > 0) { // It's a preferred service
                totalCost += Math.pow(preferenceRank - 1, 2);
            } else { // Not in preferred list
                // Nd is the total number of distinct services (or a fixed penalty if preferred)
                // Using numDistinctServices as Nd as per slides "10 x Nd^2"
                totalCost += (double) ND_PENALTY_SQUARED_BASE * numDistinctServices * numDistinctServices;
            }
        }

        // Add penalty for exceeding service capacity
        for (Map.Entry<String, Integer> entry : serviceCounts.entrySet()) {
            ServiceDetails service = serviceMap.get(entry.getKey());
            if (service != null && entry.getValue() > service.getMaxVolunteers()) {
                // Heavy penalty for exceeding capacity
                totalCost += 1000 * (entry.getValue() - service.getMaxVolunteers());
            }
        }
        return totalCost;
    }

    private Chromosome selectParent(List<Chromosome> population) { // Tournament selection
        Chromosome bestInTournament = null;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            Chromosome randomContender = population.get(ThreadLocalRandom.current().nextInt(population.size()));
            if (bestInTournament == null || randomContender.fitness < bestInTournament.fitness) {
                bestInTournament = randomContender;
            }
        }
        return bestInTournament;
    }

    private void crossover(Chromosome parent1, Chromosome parent2, Chromosome offspring1, Chromosome offspring2) {
        // Single-point crossover
        int crossoverPoint = ThreadLocalRandom.current().nextInt(parent1.assignments.size());
        for (int i = 0; i < parent1.assignments.size(); i++) {
            if (i < crossoverPoint) {
                offspring1.assignments.set(i, parent1.assignments.get(i));
                offspring2.assignments.set(i, parent2.assignments.get(i));
            } else {
                offspring1.assignments.set(i, parent2.assignments.get(i));
                offspring2.assignments.set(i, parent1.assignments.get(i));
            }
        }
    }

    private void mutate(Chromosome chromosome, List<ServiceDetails> services) {
        List<String> serviceIds = services.stream().map(ServiceDetails::getId).collect(Collectors.toList());
        if (serviceIds.isEmpty()) return;

        for (int i = 0; i < chromosome.assignments.size(); i++) {
            if (ThreadLocalRandom.current().nextDouble() < MUTATION_RATE) {
                chromosome.assignments.set(i, serviceIds.get(ThreadLocalRandom.current().nextInt(serviceIds.size())));
            }
        }
    }

    private AssignmentResult convertChromosomeToAssignmentResult(Chromosome chromosome, List<Volunteer> volunteers, Map<String, ServiceDetails> serviceMap, int numDistinctServices) {
        List<Assignment> assignmentsList = new ArrayList<>();
        double calculatedTotalCost = 0; // Recalculate for the final assignment list

        for (int i = 0; i < volunteers.size(); i++) {
            Volunteer volunteer = volunteers.get(i);
            String assignedServiceId = chromosome.assignments.get(i);
            double cost = 0;
            int preferenceRank = 0;

            if (volunteer.getPreferredServicesRanks() != null) {
                for (Map.Entry<Integer, String> entry : volunteer.getPreferredServicesRanks().entrySet()) {
                    if (entry.getValue().equals(assignedServiceId)) {
                        preferenceRank = entry.getKey();
                        break;
                    }
                }
            }

            if (preferenceRank > 0) {
                cost = Math.pow(preferenceRank - 1, 2);
            } else {
                cost = (double) ND_PENALTY_SQUARED_BASE * numDistinctServices * numDistinctServices;
            }
            calculatedTotalCost += cost;
            assignmentsList.add(new Assignment(volunteer.getId(), assignedServiceId, cost, preferenceRank));
        }
        // Capacity penalty check (though GA fitness should have minimized this)
        Map<String, Integer> serviceCounts = assignmentsList.stream()
                .collect(Collectors.groupingBy(Assignment::getServiceId, Collectors.summingInt(a -> 1)));

        for (Map.Entry<String, Integer> entry : serviceCounts.entrySet()) {
            ServiceDetails service = serviceMap.get(entry.getKey());
            if (service != null && entry.getValue() > service.getMaxVolunteers()) {
                calculatedTotalCost += 1000 * (entry.getValue() - service.getMaxVolunteers()); // Add penalty again if over capacity
                logger.warn("Service {} is over capacity in final assignment: {}/{}", service.getId(), entry.getValue(), service.getMaxVolunteers());
            }
        }


        AssignmentResult result = new AssignmentResult();
        result.setAssignments(assignmentsList);
        result.setTotalCost(chromosome.fitness); // Use fitness from GA, as it includes capacity penalties
        // result.setTotalCost(calculatedTotalCost); // Or recalculate here if you prefer
        result.setMessage("Optimization complete. Best assignment found.");
        return result;
    }
}
