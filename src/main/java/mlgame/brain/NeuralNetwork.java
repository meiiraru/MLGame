package mlgame.brain;

import org.joml.Math;

public class NeuralNetwork {

    private final int[] layers;
    private final float[][] neurons;
    private final float[][][] weights;
    private final float[][] biases;

    public Replay replay;
    public float fitness = 0; //used for the genetic algorithm

    public NeuralNetwork(int... layers) {
        this.layers = layers;
        this.neurons = new float[layers.length][];
        this.weights = new float[layers.length][][];
        this.biases  = new float[layers.length][];

        //initialize structures
        for (int i = 0; i < layers.length; i++) {
            neurons[i] = new float[layers[i]];
            biases[i] = new float[layers[i]];
            if (i > 0)
                weights[i] = new float[layers[i]][layers[i - 1]];
        }
        randomizeWeights();
    }

    private void randomizeWeights() {
        for (int i = 1; i < layers.length; i++) {
            for (int j = 0; j < layers[i]; j++) {
                biases[i][j] = ((float) Math.random() * 2) - 1f;
                for (int k = 0; k < layers[i - 1]; k++)
                    weights[i][j][k] = ((float) Math.random() * 2) - 1f;
            }
        }
    }

    //pass the game state through the network
    public boolean predict(float[] inputs) {
        //load inputs into the first layer
        System.arraycopy(inputs, 0, neurons[0], 0, inputs.length);

        //feed forward through hidden layers
        for (int i = 1; i < layers.length; i++) {
            for (int j = 0; j < layers[i]; j++) {
                float value = biases[i][j];
                for (int k = 0; k < layers[i - 1]; k++)
                    value += weights[i][j][k] * neurons[i - 1][k];

                //activation function
                if (i == layers.length - 1) {
                    //output layer - sigmoid (0f to 1f)
                    neurons[i][j] = (float) (1f / (1f + Math.exp(-value)));
                } else {
                    //hidden layers - ReLU (Math.max(0f, value))
                    neurons[i][j] = Math.max(0f, value);
                }
            }
        }

        //if the output neuron is > 0.5, we jump!
        return neurons[layers.length - 1][0] > 0.5f;
    }

    //genetic algorithm - tweaks the weights slightly to "learn"
    public void mutate(float mutationRate, float mutationAmount) {
        for (int i = 1; i < layers.length; i++) {
            for (int j = 0; j < layers[i]; j++) {
                if (Math.random() < mutationRate)
                    biases[i][j] += ((float) Math.random() * 2 - 1) * mutationAmount;

                for (int k = 0; k < layers[i - 1]; k++) {
                    if (Math.random() < mutationRate)
                        weights[i][j][k] += ((float) Math.random() * 2 - 1) * mutationAmount;
                }
            }
        }
    }

    //creates an exact copy of this brain
    public NeuralNetwork copy() {
        NeuralNetwork clone = new NeuralNetwork(layers);

        for (int i = 1; i < layers.length; i++) {
            System.arraycopy(this.biases[i], 0, clone.biases[i], 0, layers[i]);
            for (int j = 0; j < layers[i]; j++)
                System.arraycopy(this.weights[i][j], 0, clone.weights[i][j], 0, layers[i - 1]);
        }

        return clone;
    }
}
