# I2CS_Ex3
🎮 Pac-Man Reimagined — Exercise 3
<img width="846" height="784" alt="image" src="https://github.com/user-attachments/assets/d338c75b-744c-4904-9b34-c3c475a5c114" />

A modern, fully tested Pac-Man implementation with a clean server-client architecture,
algorithmic control, and extensible design.

🧠 Project Idea

This project is a complete re-implementation of the classic Pac-Man game, developed for Exercise 3.

The main goal was not only to recreate the game, but to design a robust game server, a clean graphical client, and a flexible adapter that allows external algorithms to control Pac-Man seamlessly.

The result is a well-structured, fully tested system that supports both manual gameplay and automatic algorithm-driven gameplay.

🏗 Architecture Overview

The project is divided into three clearly separated layers:

1️⃣ Server Side — Game Logic

The server contains all core game mechanics and rules:

Board and maze representation

Pac-Man movement and collisions

Ghost behavior and states

Scoring system

Power pellets and eatable ghosts

Win / lose conditions

Cyclic tunnels (wrap-around map)

📌 Main class:

MyGameServer

The server is completely independent of graphics, input, or timing.

2️⃣ Client Side — Rendering & Input

The client is responsible for:

Rendering the game using StdDraw

Handling keyboard input

Managing the main game loop and timing

Displaying a clean HUD with game status

📌 Main classes:

MyMain – application entry point

MyGameUI – rendering logic

InputController – keyboard input handling

3️⃣ Adapter Layer — Engine Integration

To integrate with the course engine and algorithms, the project includes:

MyPacmanGameAdapter

This adapter implements the PacManGame interface and bridges the external engine with the custom server implementation.

✔ Existing algorithms work without modification
✔ Server logic remains isolated and clean
✔ Clear separation of responsibilities

🎯 Game Modes
🕹 MANUAL Mode

Player controls Pac-Man using the keyboard

Arrow keys or WASD supported

🤖 AUTO Mode

Pac-Man is controlled by an algorithm

Each move is decided automatically

Ideal for testing, demonstrations, and analysis

⏯ The game can be paused or resumed at any time using the SPACE key.

🧪 Testing Strategy

The project includes extensive JUnit testing, designed to catch even subtle bugs.

✔ Unit Tests

Algorithm helper methods

Direction logic

Parsing and utility functions

Edge cases and safety checks

✔ Contract Tests

Adapter correctness

Server invariants

API stability

✔ Integration Tests

Long AUTO gameplay runs

Score and pellet invariants

Full system stability under load

All tests are designed to fail immediately if core assumptions are violated.

📚 Documentation

Detailed JavaDoc for all server-side classes

Clear architectural explanation

Design decisions documented in English

This README serves as a high-level overview of the project.

📦 Build Artifacts

The GitHub release includes:

Ex3_2.jar — Client-only solution

Ex3_3.jar — Full solution (Client + Server)

Ex3_docs.zip — Documentation

Ex3_all_src.zip — Full source code and resources

All JAR files are runnable.

🎥 Demo Video

A short demo video (up to 120 seconds) demonstrates:

Server-side design

Manual and automatic gameplay

Core features of the implementation
