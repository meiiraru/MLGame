Machine Learning Game
            
A simple 2D platformer, similar to the game Beat Stomper

The player is a simple square that needs to jump on platforms to avoid falling down  
The platforms are generated randomly, with different sizes and horizontal speeds  
The player can jump by pressing any button on the keyboard or mouse

A neural network can be trained to play the game using a genetic algorithm  
The neural network takes as input the platforms alongside the player properties  
It outputs a value between 0 and 1, which is the probability of jumping
            
The neural network consists of 3 layers, 17 input neurons, 96 hidden neurons, and 1 output neuron  
A genetic algorithm is used to evolve the neural network over generations, selecting the best performing networks and mutating them to create new networks  
A population of 1000 brains are tested simultaneously in 3 different game instances (seeds)  
The most performing brains are selected to create the next generation, keeping the best ones without mutations (elitism), and the worst ones completely random to allow different genetics

Made by Meiiraru (Pumpkin) Akitsuki