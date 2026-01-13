# Ex3 – Detailed Project Documentation

This document provides a detailed explanation of the Pac-Man game implementation,
with emphasis on server-side design, architecture decisions, game logic,
and the testing strategy used in this project.

---

## 1. Architecture Overview

The project is structured into three clearly separated layers:
server, client, and adapter.

This separation ensures clean design, easier testing, and maintainability.

---

## 2. Server Layer

The server layer is responsible for all game logic and state management.
It is completely independent of rendering, user input, or graphical concerns.

Main responsibilities:
- Managing the game board
- Tracking Pac-Man position and direction
- Handling ghost behavior and timers
- Detecting collisions
- Managing scoring
- Determining win and loss conditions

Main class:
- MyGameServer

The server exposes a clean API that is used by both the client and the adapter.

---

## 3. Client Layer

The client layer is responsible for visualization and user interaction.
It does not contain any game logic.

Main responsibilities:
- Rendering the board and entities using StdDraw
- Displaying HUD information
- Handling keyboard input
- Running the main game loop

Main classes:
- MyMain
- MyGameUI
- InputController

The client supports both manual play and automatic (algorithm-driven) play.

---

## 4. Adapter Layer

The adapter layer bridges the custom server implementation with the
course engine interfaces.

Main class:
- MyPacmanGameAdapter

This adapter allows:
- Running course-provided Pac-Man algorithms
- Using engine-based testing tools
- Maintaining full independence of the server implementation

---

## 5. Game Mechanics

### Board
- Represented as a 2D integer matrix
- Supports cyclic tunnels
- Uses engine-compatible tile codes

### Pac-Man
- Moves one cell at a time
- Cannot enter walls or the ghost house
- Eats pellets and power pellets
- Wins when all pellets are consumed

### Ghosts
- Released gradually from the ghost house
- Move autonomously
- Become edible after power pellets
- Use timers to track edible duration

---

## 6. Pac-Man Algorithm

The automatic mode uses a custom Pac-Man algorithm.

Key characteristics:
- Prioritizes pink pellets
- Builds a danger map based on ghost distances
- Avoids dangerous paths
- Switches to escape behavior when threatened
- Avoids oscillations and dead ends

Main class:
- Ex3Algo

---

## 7. Testing Strategy

The project includes an extensive JUnit test suite.

Test coverage includes:
- Helper functions in the algorithm
- Edge cases and defensive scenarios
- Legal and illegal movement validation
- Ghost house detection
- Power mode detection
- Direction selection logic

Tests are designed to fail when the implementation is modified incorrectly,
ensuring robustness and correctness.

All tests are located under the tests directory.

---

## 8. Video Demonstration

A short video (up to 120 seconds) accompanies this project.
The video explains the server-side implementation and demonstrates live gameplay.

---

## 9. Build and Distribution

The project is distributed via GitHub Releases and includes:
- Runnable JAR files
- Full source code archive
- Documentation archive

---

## 10. Summary

This project demonstrates:
- Object-oriented design principles
- Clear separation of concerns
- Defensive programming
- Algorithmic reasoning
- Extensive automated testing

The implementation fulfills all requirements of Exercise 3
and extends them with a clean, well-structured, and maintainable solution.
