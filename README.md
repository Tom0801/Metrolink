# Metrolink Planner
Summer project

This is a Java program for planning a journey on the Metrolink system

The program can read station data from a CSV file and find a route between two stations

## What the program does

This program has two main options

1. Find the route with the shortest time
2. Find the route with the fewest changes

The user can type the start station and the end station

The program will then print the route, the journey time, and the number of changes

## Files needed

The CSV file should be in the same folder as the Java file

## How to run the program

```bash
javac MetrolinkPlanner.java
```

Then run the program

```bash
java MetrolinkPlanner
```

## How to use the program

When the program starts, it asks for the data file name

Press Enter to use the default file name

Then choose one option from the menu

```text
1) Shortest time
2) Fewest changes
3) Exit
```

After that, type the start station name and the end station name

The station name must match the name in the data file

## Notes

The program uses a graph to store stations and connections

Each station can have different line versions

If the same station is on more than one line, the program treats it as a place where the user can change line

The shortest time option uses Dijkstra algorithm

The fewest changes option search has covered all paths
