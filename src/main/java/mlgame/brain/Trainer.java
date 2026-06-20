package mlgame.brain;

import cinnamon.gui.Toast;
import mlgame.game.Game;
import mlgame.game.GameState;

import java.util.Arrays;

import static mlgame.brain.Environment.INPUT_SIZE;
import static mlgame.brain.Environment.OUTPUT_SIZE;

public class Trainer {

    public static final int POPULATION_SIZE = 1000;
    public static final int GENERATIONS = 100_000;
    public static final int NEURONS = 32;

    public static final float MUTATION_RATE = 0.33f;
    public static final float MUTATION_STRENGTH = 0.5f;

    private static boolean training;

    public NeuralNetwork[] population;

    public Trainer() {
        this.population = new NeuralNetwork[POPULATION_SIZE];

        //initialize the random brains
        for (int i = 0; i < POPULATION_SIZE; i++)
            population[i] = new NeuralNetwork(INPUT_SIZE, NEURONS, OUTPUT_SIZE);
    }

    public void startTraining() {
        if (training)
            return;

        Toast.addToast("Starting AI Training...");
        training = true;
        float bestScoreOfAllTime = Float.MIN_VALUE;
        int bestGen = -1;

        //same seed for fair comparison in a generation
        long seed = System.currentTimeMillis();

        for (int generation = 1; generation <= GENERATIONS; generation++) {
            //evaluate the entire population
            for (int i = 0; i < POPULATION_SIZE; i++) {
                NeuralNetwork brain = population[i];
                brain.fitness = evaluateBrain(brain, seed);
            }

            //sort population by fitness (highest to lowest)
            Arrays.sort(population, (a, b) -> Float.compare(b.fitness, a.fitness));

            float bestGenFitness = population[0].fitness;
            float worstGenFitness = population[POPULATION_SIZE - 1].fitness;
            System.out.println("Generation " + generation + " | Best: " + bestGenFitness + " | Worst: " + worstGenFitness + " | BestGen: " + bestGen);

            //save replay if this is the best run so far
            if (bestGenFitness > bestScoreOfAllTime) {
                bestScoreOfAllTime = bestGenFitness;
                bestGen = generation;
                System.out.println("New global best! Saving replay...");
                population[0].replay.save("best_ai_replay");
            }

            //create the next generation
            NeuralNetwork[] nextGen = new NeuralNetwork[POPULATION_SIZE];

            //apply elitism to keep the top 5 brains exactly as they are
            for (int i = 0; i < 5; i++)
                nextGen[i] = population[i].copy();

            int top = (int) (POPULATION_SIZE * 0.05f); //top 5%
            int bot = (int) (POPULATION_SIZE * 0.1f);  //bottom 10%

            //the rest are mutated clones of the top 5% brains as evolution
            for (int i = 5; i < POPULATION_SIZE - bot; i++) {
                int parentIndex = i % top;
                NeuralNetwork child = population[parentIndex].copy();

                //mutate the children population
                child.mutate(MUTATION_RATE, MUTATION_STRENGTH);
                nextGen[i] = child;
            }

            //the last 10% are completely random brains to maintain diversity
            for (int i = POPULATION_SIZE - bot; i < POPULATION_SIZE; i++)
                nextGen[i] = new NeuralNetwork(INPUT_SIZE, NEURONS, OUTPUT_SIZE);

            population = nextGen;
        }

        training = false;
        Toast.addToast("Training Complete!");
    }

    private float evaluateBrain(NeuralNetwork brain, long seed) {
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

        return totalReward;
    }
}
