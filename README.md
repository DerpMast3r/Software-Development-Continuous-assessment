# ECM2414 Software Development - Multi-threaded Card Game

## Overview

This project is an implementation of a multi-threaded card game in Java for the ECM2414 Software Development coursework.

The game supports a positive number of players as specified by the user. Each player will be represented by a separate thread and will attempt to collect four cards from the same denomination.

Players will draw cards from the decks designated to them and have cards discarded into the next player's deck. The game will end when a player can acquire a winning hand.

## Requirements

* Java Development Kit (JDK) 11 or later.
* A terminal or command prompt.
* Valid text files containing card packs.

## Project Files

* Card.java - Represents an individual card.
* CardDeck.java - Manages a deck of cards, which includes things like drawing and adding cards.
* CardGame.java - This handles the game setup, validation of inputs, dealing, thread execution and final output of deck.
* GameState.java - Will monitor the winning player and coordinate thread activity.
* Player.java - Implements the player behaviour, management of hands, drawing, discarding and output logging.
* PlayerCheck.java - This contains the basic checks for player behaviour.

## Compiling of Program

Navigate to the project directory which contains the Java source files.

Compile using: 

```javac Card.java CardDeck.java CardGame.java GameState.java Player.java PlayerCheck.java```

## Running of Program

After the compilation, run:

```java CardGame```

The program will prompt for:
1. The number of players.
2. The path for a text file which will contain a card pack.

For n players, the pack should contain 8n cards exactly, with only one non-negative integer per line.

Example:

```Enter number of players: 2```

```Enter path to input pack file: normalpack.txt```

## Game Rules

Each player will start with four cards. Remaining cards will be distributed among the decks.

Player ```i``` draws from deck ```i``` and discards to deck ```i+1``` and the final player discards to deck 1.

Players will prefer cards that have denominations matching their player number.

A player can win when their hand will contain four cards of the same denomination. There will be only one winner able to be declared.

## Concurrency

Each player will run as a separate Java thread. The shared ```GameState``` will coordinate complete draw and discard operations using synchronisation. Players who are waiting for cards will release the shared monitor, which allows other players to continue. The winner declaration uses ```AtomicInteger.compareAndSet()``` to prevent there being multiple winners.

## Output Files

For a game that has n players, the program will generate:

* ```player1_output.txt``` through ```playern_output.txt```, recording actions of players and the final hands.
* ```deck1_output.txt``` through ```deckn_output.txt```, recording each deck's final contents.

The expected total would be 2n output files.

## Testing

The following manual tests have been performed:

* Compilation of six Java source files - Passed
* Two-player immediate winning hand - Passed
* Two-player normal gameplay - Passed
* Three-player normal gameplay - Passed
* Player and deck output generation - Passed for the tested scenarios

Additional concurrency, invalid-input and automated testing results to be recorded here

## Known Limitations

To be completed following final testing. In particular, termination for card distributions where no winning hand can be reached requires verification

## Automated Testing

Two test suites are provided:

Plain-Java suites (no external dependencies):
* java CardDeckCheck
* java GameStateCheck
* java CardGameCheck
* java PlayerCheck

These run on any JDK 11+ and require no third-party libraries.

JUnit 5 suites (requires junit-platform-console-standalone.jar):
    ```java -jar junit-platform-console-standalone.jar --class-path . --scan-class-path```

Or run a specific class:
    ```java -jar junit-platform-console-standalone.jar --class-path . --select-class GameStateJUnitTest```

## Building and Running the JAR
The executable JAR is named cards.jar. It contains the nine .class files, nine .java source files, and a manifest with Main-Class: CardGame (Does not include JUnit tests).

From the project directory:

```javac *.java```
```jar --create --file cards.jar --main-class CardGame *.class *.java```

Verify the JAR:

```java -m jdk.jartool/sun.tools.jar.Main --list --file cards.jar```
```java -jar cards.jar```

Running from the JAR:

```java -jar cards.jar```

The prompts and behaviour are identical to java CardGame.

