package mlgame.brain;

import cinnamon.Client;
import cinnamon.utils.IOUtils;
import mlgame.game.Game;
import mlgame.game.GameState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static mlgame.brain.Environment.INPUT_SIZE;
import static mlgame.brain.Environment.OUTPUT_SIZE;

public class Trainer {

    public static final int NEURONS = 96;

    public static final int POPULATION_SIZE = 1000;
    public static final float ELITISM_COUNT_RATE = 0.1f;
    public static final float MUTATION_COUNT_RATE = 0.5f;
    public static final float NEW_RANDOM_RATE = 0.05f;

    public static final float MUTATION_RATE = 0.02f;
    public static final float MUTATION_STRENGTH = 0.05f;
    public static final float MUTATION_REPLACE = 0.1f;

    public static final int SNAPSHOT_INTERVAL = 500;
    public static final int RESEED_INTERVAL = 10;

    public final Path trainingPath;
    public long[] seeds = new long[10];

    public NeuralNetwork[] population;
    public float bestFitness = -Float.MAX_VALUE;
    public float allTimeBest = -Float.MAX_VALUE;
    public int bestGen = -1;
    public int generation = 1;

    public long elapsedTrainingTime;
    public long trainingStartTime;
    public boolean training = false;
    private boolean locked = false;
    private boolean loaded = false;
    public boolean saving = false;

    private int populationSize = POPULATION_SIZE;
    private int neurons = NEURONS;
    private float mutationRate = MUTATION_RATE;
    private float mutationStrength = MUTATION_STRENGTH;
    private float mutationReplace = MUTATION_REPLACE;

    public final List<String> snapshots = new ArrayList<>();

    public Trainer(Path trainingPath) {
        this.trainingPath = trainingPath;

        //try to load from previous training stats
        loadStatsFromFile();

        loadSnapshotListFromFile();
    }

    public void startTraining() {
        if (locked)
            return;

        locked = true;

        if (!loaded) {
            //try to load the population from file
            loadBrainFromFile();
            loaded = true;
        }

        training = true;
        new Thread(this::train).start();
    }

    public void stopTraining() {
        training = false;
    }

    private void train() {
        trainingStartTime = System.currentTimeMillis();
        Random random = new Random();

        while (training) {
            //rotate the seeds every couple generations to prevent overfitting
            if (generation % RESEED_INTERVAL == 0) {
                for (int i = 0; i < seeds.length; i++)
                    seeds[i] = random.nextLong();

                //reset best
                bestFitness = -Float.MAX_VALUE;
            }

            //take a snapshot of the current best
            if (generation % SNAPSHOT_INTERVAL == 0)
                snapshot();

            //local best fitness
            float localBestFitness = -Float.MAX_VALUE;
            Replay localBestReplay = null;

            //evaluate the entire population
            for (int i = 0; i < populationSize; i++) {
                NeuralNetwork brain = population[i];
                float fitness = 0f;

                //simulate each brain across multiple seeds
                for (long seed : seeds) {
                    float result = evaluateBrain(brain, seed);
                    fitness += result;
                }

                fitness /= seeds.length; //average fitness across seeds
                brain.fitness = fitness;

                //update local best fitness
                if (fitness > localBestFitness) {
                    localBestFitness = fitness;
                    localBestReplay = brain.replay;
                }
            }

            //sort population by fitness (highest to lowest)
            Arrays.sort(population, (a, b) -> Float.compare(b.fitness, a.fitness));

            //store the best run of this group
            if (localBestFitness > bestFitness) {
                bestFitness = localBestFitness;
                snapshots.add(generation + "," + bestFitness + ",0");
            }

            //snapshot if this is the best run of all time!
            if (bestFitness > allTimeBest && localBestReplay != null) {
                allTimeBest = bestFitness;
                bestGen = generation;
                localBestReplay.save(trainingPath.resolve("best.replay"));
                snapshot(localBestReplay, bestFitness, generation);
            }

            //create the next generation
            NeuralNetwork[] nextGen = new NeuralNetwork[populationSize];

            int elitism = (int) (populationSize * ELITISM_COUNT_RATE);
            int top     = (int) (populationSize * MUTATION_COUNT_RATE);
            int bot     = (int) (populationSize * NEW_RANDOM_RATE);

            //apply elitism to keep the top 5 brains exactly as they are
            for (int i = 0; i < elitism; i++)
                nextGen[i] = population[i].copy();

            //the rest are mutated clones of the top brains as evolution
            for (int i = elitism; i < populationSize - bot; i++) {
                int parentIndex = (int) (Math.random() * top);
                NeuralNetwork child = population[parentIndex].copy();

                //mutate the children population
                child.mutate(mutationRate, mutationStrength, mutationReplace);
                nextGen[i] = child;
            }

            //the last are completely random brains in hope for a better random strategy
            for (int i = populationSize - bot; i < populationSize; i++)
                nextGen[i] = new NeuralNetwork(INPUT_SIZE, neurons, OUTPUT_SIZE);

            population = nextGen;
            generation++;
        }
        snapshot();
        locked = false;
    }

    public float evaluateBrain(NeuralNetwork brain, long seed) {
        //initialize the game
        Game simGame = new Game(null, seed, true);
        simGame.newGame();
        simGame.gameState = GameState.PLAYING;

        //initialize the replay for this brain
        brain.replay = new Replay(seed);

        //initialize the environment
        Environment env = new Environment(simGame);
        float totalReward = 0;
        float targetReward = 1_000; //stop early if we reach this fitness score to save time
        boolean prevJump = false;

        //run the game until it is over
        while (simGame.gameState != GameState.GAME_OVER && totalReward < targetReward) {
            //send the environment state to the brain and get the next action
            float[] state = env.getState();
            boolean jump = !prevJump && brain.predict(state);
            brain.replay.recordAction(jump);

            //step the environment with the chosen action and get the reward
            float reward = env.step(jump);
            totalReward += reward;
            prevJump = jump;
        }

        return totalReward;
    }

    public void snapshot() {
        snapshot(population[0].replay, population[0].fitness, generation);
    }

    private void snapshot(Replay replay, float fitness, int generation) {
        if (replay == null)
            return;

        //save the replay snapshot
        replay.save(trainingPath.resolve("snapshots/" + generation + ".replay"));

        //save the snapshot to the snapshot list
        snapshots.add(generation + "," + fitness + ",1");

        //elapsed time
        long timeNow = System.currentTimeMillis();
        elapsedTrainingTime += (System.currentTimeMillis() - trainingStartTime);
        trainingStartTime = timeNow;

        //save the data to a file
        saveSnapshotListToFile();
        saveStatsToFile();
        saveBrainToFile();
    }

    public void saveSnapshotListToFile() {
        //save the snapshot list to a file format: "generation,fitness;generation,fitness;..."
        Path snapshotFile = trainingPath.resolve("snapshot_data.moon");
        IOUtils.createOrGetFile(snapshotFile);
        StringBuilder sb = new StringBuilder();
        for (String snapshot : snapshots)
            sb.append(snapshot).append(";");
        IOUtils.writeFileCompressed(snapshotFile, sb.toString().getBytes());
    }

    private void loadSnapshotListFromFile() {
        Path snapshotFile = trainingPath.resolve("snapshot_data.moon");
        byte[] snapshotBytes = IOUtils.readFileCompressed(snapshotFile);
        if (snapshotBytes != null) {
            String snapshotData = new String(snapshotBytes);
            String[] snapshotParts = snapshotData.split(";");
            snapshots.clear();
            for (String snapshot : snapshotParts) {
                if (!snapshot.isEmpty())
                    snapshots.add(snapshot);
            }
        }
    }

    public void saveStatsToFile() {
        //seed1,seed2,...;curr_gen;best_gen;best_fitness;all_time_best;elapsed_training_time
        Path statsFile = trainingPath.resolve("training_stats.moon");
        StringBuilder sb = new StringBuilder();

        //seeds
        for (long seed : seeds)
            sb.append(seed).append(",");
        sb.append(";");

        //data
        sb.append(generation).append(";");
        sb.append(bestGen).append(";");
        sb.append(bestFitness).append(";");
        sb.append(allTimeBest).append(";");
        sb.append(elapsedTrainingTime).append(";");

        IOUtils.writeFileCompressed(statsFile, sb.toString().getBytes());
    }

    private void loadStatsFromFile() {
        Path statsFile = trainingPath.resolve("training_stats.moon");
        byte[] statsBytes = IOUtils.readFileCompressed(statsFile);
        if (statsBytes != null) {
            String stats = new String(statsBytes);
            String[] parts = stats.split(";");
            try {
                String[] seedParts = parts[0].split(",");
                for (int i = 0; i < seedParts.length; i++)
                    seeds[i] = Long.parseLong(seedParts[i]);

                generation  = Integer.parseInt(parts[1]);
                bestGen     = Integer.parseInt(parts[2]);
                bestFitness = Float.parseFloat(parts[3]);
                allTimeBest = Float.parseFloat(parts[4]);
                elapsedTrainingTime = Long.parseLong(parts[5]);
                return;
            } catch (Exception e) {
                Client.LOGGER.error("Failed to parse brain data, starting fresh", e);
            }
        }

        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < seeds.length; i++)
            seeds[i] = random.nextLong();
    }

    public void saveBrainToFile() {
        //save each brain in a string format: "population:neurons:mutationRate:mutationStrength:mutationReplace:brain1:brain2:..."
        //brain format: "layer1,layer2,...;bias1,weight1,weight2,..."
        this.saving = true;
        StringBuilder sb = new StringBuilder();

        sb.append(populationSize).append(":");
        sb.append(neurons).append(":");
        sb.append(mutationRate).append(":");
        sb.append(mutationStrength).append(":");
        sb.append(mutationReplace).append(":");

        for (NeuralNetwork brain : population)
            sb.append(brain.serialize()).append(":");

        backupBrain(); //backup current brain file before overwriting
        Path brainFile = trainingPath.resolve("brain.moon");
        IOUtils.writeFileCompressed(brainFile, sb.toString().getBytes());
        deleteBackup(); //delete backup if save was successful

        this.saving = false;
    }

    private void loadBrainFromFile() {
        Path brainFile = trainingPath.resolve("brain.moon");
        byte[] brainBytes = IOUtils.readFileCompressed(brainFile);
        if (brainBytes != null) {
            String brainData = new String(brainBytes);
            String[] brainParts = brainData.split(":");
            try {
                int offset = 0;
                this.populationSize = Integer.parseInt(brainParts[offset++]);
                this.neurons = Integer.parseInt(brainParts[offset++]);
                this.mutationRate = Float.parseFloat(brainParts[offset++]);
                this.mutationStrength = Float.parseFloat(brainParts[offset++]);
                this.mutationReplace = Float.parseFloat(brainParts[offset++]);

                this.population = new NeuralNetwork[populationSize];
                for (int i = 0; i < populationSize; i++)
                    this.population[i] = NeuralNetwork.deserialize(brainParts[i + offset]);
                return;
            } catch (Exception e) {
                Client.LOGGER.error("Failed to parse brain data, starting fresh", e);
            }
        }

        //initiate fresh if everything went wrong
        this.population = new NeuralNetwork[populationSize];
        for (int i = 0; i < populationSize; i++)
            population[i] = new NeuralNetwork(INPUT_SIZE, neurons, OUTPUT_SIZE);
    }

    private void backupBrain() {
        //copy ./brain.moon to ./prev_brain.moon
        Path brainFile = trainingPath.resolve("brain.moon");
        Path backupFile = trainingPath.resolve("prev_brain.moon");

        if (!Files.exists(brainFile))
            return;

        try {
            Files.copy(brainFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Client.LOGGER.error("Failed to backup brain file", e);
        }
    }

    private void deleteBackup() {
        Path backupFile = trainingPath.resolve("prev_brain.moon");
        if (Files.exists(backupFile)) {
            try {
                Files.delete(backupFile);
            } catch (Exception e) {
                Client.LOGGER.error("Failed to delete backup brain file", e);
            }
        }
    }
}
