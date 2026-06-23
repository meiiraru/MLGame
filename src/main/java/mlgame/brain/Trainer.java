package mlgame.brain;

import cinnamon.Client;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Pair;
import mlgame.game.Game;
import mlgame.game.GameState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Random;

import static mlgame.brain.Environment.INPUT_SIZE;
import static mlgame.brain.Environment.OUTPUT_SIZE;

public class Trainer {

    public static final int POPULATION_SIZE = 1000;
    public static final int NEURONS = 64;

    public static final float MUTATION_RATE = 0.02f;
    public static final float MUTATION_STRENGTH = 0.05f;
    public static final float MUTATION_REPLACE = 0.1f;

    public static final int SNAPSHOT_INTERVAL = 1000;

    public final Path trainingPath;
    public long[] seeds = new long[3];

    public NeuralNetwork[] population;
    public float bestFitness = Float.MIN_VALUE;
    public int bestScore = Integer.MIN_VALUE;
    public int bestGen = -1;
    public int generation = 1;

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

    public Trainer(Path trainingPath) {
        this.trainingPath = trainingPath;

        //try to load from previous training stats
        loadStatsFromFile();
    }

    public void startTraining() {
        if (locked)
            return;

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
        locked = true;
        trainingStartTime = System.currentTimeMillis();
        while (training) {
            //take a snapshot of the current best
            if (generation % SNAPSHOT_INTERVAL == 0)
                snapshot();

            //local high score
            int localBestScore = Integer.MIN_VALUE;
            Replay localBestReplay = null;

            //evaluate the entire population
            for (int i = 0; i < populationSize; i++) {
                NeuralNetwork brain = population[i];
                float totalFitness = 0;

                //simulate each brain across multiple seeds
                for (long seed : seeds) {
                    Pair<Float, Integer> result = evaluateBrain(brain, seed);
                    totalFitness += result.first();

                    //update local best score
                    if (result.second() > localBestScore) {
                        localBestScore = result.second();
                        localBestReplay = brain.replay;
                    }
                }

                //average fitness across seeds
                brain.fitness = totalFitness / seeds.length;
            }

            //sort population by fitness (highest to lowest)
            Arrays.sort(population, (a, b) -> Float.compare(b.fitness, a.fitness));

            int top = (int) (populationSize * 0.2f); //top 20%
            int bot = (int) (populationSize * 0.1f); //bottom 10%

            //save replay if this is the best run so far
            if (localBestScore > bestScore) {
                bestScore = localBestScore;
                bestGen = generation;
                localBestReplay.save(trainingPath.resolve("best_ai_replay.replay"));
                snapshot();
            }

            //create the next generation
            NeuralNetwork[] nextGen = new NeuralNetwork[populationSize];

            //apply elitism to keep the top 5 brains exactly as they are
            for (int i = 0; i < 5; i++)
                nextGen[i] = population[i].copy();

            //the rest are mutated clones of the top brains as evolution
            for (int i = 5; i < populationSize - bot; i++) {
                int parentIndex = (int) (Math.pow(Math.random(), 2) * top);
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

    private Pair<Float, Integer> evaluateBrain(NeuralNetwork brain, long seed) {
        //initialize the game
        Game simGame = new Game(null, seed, true);
        simGame.newGame();
        simGame.gameState = GameState.PLAYING;

        //initialize the replay for this brain
        brain.replay = new Replay(seed);

        //initialize the environment
        Environment env = new Environment(simGame);
        float totalReward = 0;
        float targetReward = 5_000_000; //stop early if we reach this score to save time
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

        return Pair.of(totalReward, simGame.score);
    }

    public void snapshot() {
        if (population[0].replay == null)
            return;

        //save the replay snapshot for this generation
        int score = population[0].score;
        population[0].replay.save(trainingPath.resolve("snapshots/" + generation + "_" + score + ".replay"));

        //save stats and brain
        saveStatsToFile();
        saveBrainToFile();
    }

    public void saveStatsToFile() {
        //seed1,seed2,...;curr_gen;best_fitness;best_score;best_gen
        Path statsFile = trainingPath.resolve("training_stats.moon");
        String stats = seeds[0] + "," + seeds[1] + "," + seeds[2] + ";" + generation + ";" + bestFitness + ";" + bestScore + ";" + bestGen;
        IOUtils.writeFileCompressed(statsFile, stats.getBytes());
    }

    private void loadStatsFromFile() {
        Path statsFile = trainingPath.resolve("training_stats.moon");
        byte[] statsBytes = IOUtils.readFileCompressed(statsFile);
        if (statsBytes != null) {
            String stats = new String(statsBytes);
            String[] parts = stats.split(";");
            try {
                String[] seedParts = parts[0].split(",");
                seeds[0] = Long.parseLong(seedParts[0]);
                seeds[1] = Long.parseLong(seedParts[1]);
                seeds[2] = Long.parseLong(seedParts[2]);

                generation  = Integer.parseInt(parts[1]);
                bestFitness = Float.parseFloat(parts[2]);
                bestScore   = Integer.parseInt(parts[3]);
                bestGen     = Integer.parseInt(parts[4]);
                return;
            } catch (Exception e) {
                Client.LOGGER.error("Failed to parse brain data, starting fresh", e);
            }
        }

        Random random = new Random(System.currentTimeMillis());
        seeds[0] = random.nextLong();
        seeds[1] = random.nextLong();
        seeds[2] = random.nextLong();
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
