package mlgame.brain;

import cinnamon.gui.Toast;
import mlgame.MainMenu;
import mlgame.game.Game;
import mlgame.game.GameState;

import java.util.Arrays;

import static mlgame.brain.Environment.INPUT_SIZE;
import static mlgame.brain.Environment.OUTPUT_SIZE;

public class Trainer {

    public static final int POPULATION_SIZE = 1000;
    public static final int GENERATIONS = 50_000;
    public static final int NEURONS = 32;

    private final MainMenu menu;
    private NeuralNetwork[] population;

    public Trainer(MainMenu menu) {
        this.menu = menu;
        this.population = new NeuralNetwork[POPULATION_SIZE];

        //initialize the random brains
        for (int i = 0; i < POPULATION_SIZE; i++)
            population[i] = new NeuralNetwork(INPUT_SIZE, NEURONS, OUTPUT_SIZE);
    }

    public void startTraining() {
        Toast.addToast("Starting AI Training...");
        long bestScoreOfAllTime = Long.MIN_VALUE;

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
            System.out.println("Generation " + generation + " | Best Fitness: " + bestGenFitness);

            //save replay if this is the best run so far
            if (bestGenFitness > bestScoreOfAllTime) {
                bestScoreOfAllTime = (long) bestGenFitness;
                System.out.println("New global best! Saving replay...");
                population[0].replay.save("best_ai_replay");
            }

            //create the next generation (evolution)
            NeuralNetwork[] nextGen = new NeuralNetwork[POPULATION_SIZE];

            //keep the top 5 brains exactly as they are (elitism)
            for (int i = 0; i < 5; i++)
                nextGen[i] = population[i].copy();

            //the rest are mutated clones of the top 20 brains
            for (int i = 5; i < POPULATION_SIZE - 100; i++) {
                int parentIndex = i % 20;
                NeuralNetwork child = population[parentIndex].copy();

                //mutate with a 33% chance to change a weight by +- 0.5
                child.mutate(0.33f, 0.5f);
                nextGen[i] = child;
            }

            //the last 100 are completely random brains to maintain diversity
            for (int i = POPULATION_SIZE - 100; i < POPULATION_SIZE; i++)
                nextGen[i] = new NeuralNetwork(INPUT_SIZE, NEURONS, OUTPUT_SIZE);

            population = nextGen;
        }
        Toast.addToast("Training Complete!");
    }

    private float evaluateBrain(NeuralNetwork brain, long seed) {
        //initialize the game
        Game simGame = new Game(menu, seed, true);
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
