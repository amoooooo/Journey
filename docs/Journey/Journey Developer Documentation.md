# Journey Developer Documentation

Welcome to the **Journey Developer Documentation**! This guide provides comprehensive information about the Journey system, including event handling, Molang queries, and task creation. Whether you're a developer integrating new features or managing quests on your Cobblemon server, this documentation will assist you in effectively utilizing the Journey framework.

---

## Table of Contents

1. [Introduction](#introduction)
2. [Events](./events.md)
3. [Molang Queries](./molang-queries.md)
4. [Creating a Task](./creating-a-task.md)
5. [Appendix](#appendix)

---

## Introduction

**Journey** is a quest and task management system designed for Cobblemon servers. It allows server administrators to create intricate quests that players can embark on, enhancing the gameplay experience. Journey leverages **Molang** expressions to create dynamic and conditional quest behaviors, ensuring that tasks are engaging and responsive to player actions.

This documentation covers:

- **Events**: Various in-game events that can trigger quest progress.
- **Molang Queries**: Expressions used to filter and evaluate quest conditions.
- **Task Creation**: Step-by-step guide to designing and implementing quests.

Let's dive into the core components of Journey!

---

## Events

[Back to Table of Contents](./README.md)

Journey listens to a variety of in-game events to track and update quest progress. Each event corresponds to specific player actions or game state changes. Below is a detailed overview of the available events and their associated Molang queries.

### Overview of Journey Events

| Event Name           | Description                                               |
|----------------------|-----------------------------------------------------------|
| `ENTER_ZONE`         | Triggered when a player enters a specific zone.           |
| `LEAVE_ZONE`         | Triggered when a player exits a specific zone.            |
| `ENTER_ZONE_AREA`    | Triggered when a player enters a specific area within a zone. |
| `ENTITY_INTERACT`    | Triggered when a player interacts with an entity.         |
| `BATTLE_VICTORY`     | Triggered when a player wins a battle.                    |
| `BATTLE_FLED`        | Triggered when a player flees from a battle.             |
| `ITEM_PICKUP`        | Triggered when a player picks up an item.                 |
| `POKEMON_CAPTURE`    | Triggered when a player captures a Pokémon.               |
| `POKEMON_EVOLVE`     | Triggered when a player's Pokémon evolves.                |
| `POKEMON_LEVEL_UP`   | Triggered when a player's Pokémon levels up.              |

### Detailed Event Descriptions and Molang Queries

Each event has specific Molang queries that can be used to filter or evaluate conditions for quest progression.

---

#### 1. ENTER_ZONE

**Description**: Triggered when a player enters a designated zone within the Ultra Megalopolis.

**Molang Queries**:

- **`q.zone.uuid`**: Retrieves the UUID of the zone the player has entered.
  
  **Example Filter**:
  
  ```molang
  q.zone.uuid == 'c106b35a-1058-4aca-809c-b9588c14ba11'
  ```

- **`q.player.is_in_zone`**: Checks if the player is within a specific zone.
  
  **Example Filter**:
  
  ```molang
  q.player.is_in_zone('c106b35a-1058-4aca-809c-b9588c14ba11')
  ```

---

#### 2. LEAVE_ZONE

**Description**: Triggered when a player exits a designated zone.

**Molang Queries**:

- **`q.zone.uuid`**: Retrieves the UUID of the zone the player has left.
  
  **Example Filter**:
  
  ```molang
  q.zone.uuid == 'c106b35a-1058-4aca-809c-b9588c14ba11'
  ```

---

#### 3. ENTER_ZONE_AREA

**Description**: Triggered when a player enters a specific area within a zone.

**Molang Queries**:

- **`q.zone.uuid`**: Retrieves the UUID of the zone.
- **`q.area.uuid`**: Retrieves the UUID of the area within the zone.
  
  **Example Filter**:
  
  ```molang
  q.zone.uuid == 'zone-uuid' && q.area.uuid == 'area-uuid'
  ```

---

#### 4. ENTITY_INTERACT

**Description**: Triggered when a player interacts with a specific entity (e.g., NPCs).

**Molang Queries**:

- **`q.entity.uuid`**: Retrieves the UUID of the entity being interacted with.
  
  **Example Filter**:
  
  ```molang
  q.entity.uuid == '2de91ae9-e18e-4419-bffe-43d1b6cdbb2e'
  ```

- **Additional Conditions**:
  
  - Combining with player quest status:
    
    ```molang
    q.entity.uuid == '2de91ae9-e18e-4419-bffe-43d1b6cdbb2e' && q.player.has_completed_task('journey:evolve_starter')
    ```

---

#### 5. BATTLE_VICTORY

**Description**: Triggered when a player wins a battle.

**Molang Queries**:

- **`q.battle.is_wild`**: Checks if the battle was against a wild opponent.
  
  **Example Filter**:
  
  ```molang
  q.battle.is_wild == 1.0
  ```

- **`q.battle.opponent.uuid`**: Retrieves the UUID of the opponent Pokémon.
  
  **Example Filter**:
  
  ```molang
  q.battle.opponent.uuid == 'opponent-uuid'
  ```

- **`q.battle.team.contains_starter`**: Checks if the player's team contains the starter Pokémon.
  
  **Example Filter**:
  
  ```molang
  q.battle.team.contains_starter == 1.0
  ```

---

#### 6. BATTLE_FLED

**Description**: Triggered when a player flees from a battle.

**Molang Queries**:

- **`q.battle.is_pvw`**: Checks if the battle was Player vs. Wild.
  
  **Example Filter**:
  
  ```molang
  q.battle.is_pvw == 1.0
  ```

- **`q.battle.opponent.uuid`**: Retrieves the UUID of the opponent Pokémon.
  
  **Example Filter**:
  
  ```molang
  q.battle.opponent.uuid == 'opponent-uuid'
  ```

---

#### 7. ITEM_PICKUP

**Description**: Triggered when a player picks up an item.

**Molang Queries**:

- **`q.item.id`**: Retrieves the registry name of the item.
  
  **Example Filter**:
  
  ```molang
  q.item.id == 'cobblemon:ultra_badge'
  ```

- **`q.item.count`**: Retrieves the quantity of the item picked up.
  
  **Example Filter**:
  
  ```molang
  q.item.count >= 1.0
  ```

---

#### 8. POKEMON_CAPTURE

**Description**: Triggered when a player captures a Pokémon.

**Molang Queries**:

- **`q.pokemon`**: Retrieves detailed information about the captured Pokémon.
  
  **Example Filter**:
  
  ```molang
  q.pokemon.species.name == 'Pikachu' && q.pokemon.is_shiny == 1.0
  ```

---

#### 9. POKEMON_EVOLVE

**Description**: Triggered when a player's Pokémon evolves.

**Molang Queries**:

- **`q.pokemon`**: Retrieves detailed information about the evolved Pokémon.
  
  **Example Filter**:
  
  ```molang
  q.pokemon.species.name == 'Raichu'
  ```

---

#### 10. POKEMON_LEVEL_UP

**Description**: Triggered when a player's Pokémon levels up.

**Molang Queries**:

- **`q.pokemon.level`**: Retrieves the new level of the Pokémon.
  
  **Example Filter**:
  
  ```molang
  q.pokemon.level >= 30.0
  ```

- **`q.pokemon.gender`**: Retrieves the gender of the Pokémon.
  
  **Example Filter**:
  
  ```molang
  q.pokemon.gender == 'MALE'
  ```

---

## Molang Queries

[Back to Table of Contents](./README.md)

Molang queries are expressions used to evaluate conditions within tasks. They allow for dynamic and conditional quest progression based on in-game events and player actions. Journey extends Cobblemon's default Molang functions with additional queries tailored for quest management.

### Categories of Molang Queries

1. **Player Functions**: Default Cobblemon functions extended by Journey.
2. **Journey Functions**: Additional functions specific to the Journey system.
3. **Event-Specific Functions**: Queries tailored for specific events.

---

### 1. Player Functions

These functions relate directly to the player's status and inventory.

| Function Name        | Description                                           |
|----------------------|-------------------------------------------------------|
| `has_completed_task` | Checks if the player has completed a specific task.   |
| `has_completed_subtask` | Checks if the player has completed a specific subtask. |
| `start_task`         | Initiates a new task for the player.                  |
| `has_item`           | Checks if the player possesses a specific item.        |
| `pokedex`            | Retrieves the player's Pokédex data.                  |
| `starter_pokemon`    | Retrieves the player's starter Pokémon.               |
| `is_in_zone`         | Checks if the player is within a specific zone.       |

#### Examples

---

**Function**: `has_completed_task`

**Description**: Determines if the player has completed the specified task.

**Usage in Filter**:

```molang
q.player.has_completed_task('journey:evolve_starter')
```

**Example Context**:

A subtask that requires the player to have evolved their starter Pokémon before interacting with Captain Phyco.

```json
{
  "filter": "q.entity.uuid == '2de91ae9-e18e-4419-bffe-43d1b6cdbb2e' && q.player.has_completed_task('journey:evolve_starter')"
}
```

---

**Function**: `has_item`

**Description**: Checks if the player has a specific item in their inventory.

**Usage in Filter**:

```molang
q.player.has_item('cobblemon:ultra_badge')
```

**Example Context**:

A task that requires the player to possess the Ultra Badge before proceeding.

```json
{
  "filter": "q.player.has_item('cobblemon:ultra_badge')"
}
```

---

**Function**: `is_in_zone`

**Description**: Checks if the player is currently within a specified zone.

**Usage in Filter**:

```molang
q.player.is_in_zone('c106b35a-1058-4aca-809c-b9588c14ba11')
```

**Example Context**:

A subtask that activates when the player is within the Observation Tower zone.

```json
{
  "filter": "q.player.is_in_zone('c106b35a-1058-4aca-809c-b9588c14ba11')"
}
```

---

### 2. Journey Functions

These functions are specific to the Journey system, providing additional capabilities for quest management.

| Function Name        | Description                                           |
|----------------------|-------------------------------------------------------|
| `has_completed_subtask` | Checks if the player has completed a specific subtask. |
| `start_task`         | Initiates a new task for the player.                  |
| `pokedex`            | Retrieves the player's Pokédex data.                  |
| `starter_pokemon`    | Retrieves the player's starter Pokémon.               |
| `is_in_zone`         | Checks if the player is within a specific zone.       |

#### Examples

---

**Function**: `has_completed_subtask`

**Description**: Determines if the player has completed a specific subtask within a task.

**Usage in Filter**:

```molang
q.player.has_completed_subtask('journey:discover_biomes', 'intro:observe_tower')
```

**Example Context**:

A subtask that requires the player to observe the tower after completing the initial task.

```json
{
  "filter": "q.player.has_completed_subtask('journey:discover_biomes', 'intro:observe_tower')"
}
```

---

**Function**: `pokedex`

**Description**: Retrieves the player's Pokédex data, allowing for queries based on captured Pokémon.

**Usage in Filter**:

```molang
q.player.pokedex.has_pokemon('Pikachu') == 1.0
```

**Example Context**:

A task that requires the player to capture a Pikachu.

```json
{
  "filter": "q.player.pokedex.has_pokemon('Pikachu') == 1.0"
}
```

---

### 3. Event-Specific Functions

These queries are tailored to specific events, providing detailed information about the event context.

#### ENTER_ZONE Event

**Molang Queries**:

- **`q.zone.uuid`**: UUID of the zone entered.
- **`q.player.is_in_zone`**: Checks if the player is in a specific zone.

**Example Filter**:

```molang
q.zone.uuid == 'c106b35a-1058-4aca-809c-b9588c14ba11' && q.player.has_completed_task('journey:observe_tower')
```

---

#### ENTITY_INTERACT Event

**Molang Queries**:

- **`q.entity.uuid`**: UUID of the entity being interacted with.
- **`q.player.has_completed_task`**: Checks if the player has completed a specific task.

**Example Filter**:

```molang
q.entity.uuid == '2de91ae9-e18e-4419-bffe-43d1b6cdbb2e' && q.player.has_completed_task('journey:evolve_starter')
```

---

## Creating a Task

[Back to Table of Contents](./README.md)

Creating a task in Journey involves defining its properties, associated events, filters, and rewards. This guide walks you through the process of designing and implementing a task using the Journey system.

### Step-by-Step Guide

1. **Define Task Properties**
2. **Specify Events and Filters**
3. **Set Target Conditions**
4. **Assign Rewards**
5. **Location Specification**
6. **Registering the Task**

---

### 1. Define Task Properties

Each task must have the following properties:

- **`id`**: Unique identifier for the task.
- **`name`**: Display name of the task.
- **`description`**: Brief description of the task objectives.
- **`event`**: The event that triggers or progresses the task.
- **`event_data`**: Additional data related to the event.
- **`filter`**: Molang expression to filter or evaluate task conditions.
- **`target`**: The goal or condition required to complete the task.
- **`location`**: (Optional) Coordinates related to the task.
- **`rewards`**: List of rewards granted upon task completion.

---

### 2. Specify Events and Filters

Events determine when and how a task progresses. Filters use Molang expressions to add conditions that must be met for task progress or completion.

**Example**: Interacting with a specific NPC after completing a prerequisite task.

```json
{
  "event": "ENTITY_INTERACT",
  "event_data": {
    "uuid": "2de91ae9-e18e-4419-bffe-43d1b6cdbb2e"
  },
  "filter": "q.entity.uuid == '2de91ae9-e18e-4419-bffe-43d1b6cdbb2e' && q.player.has_completed_task('journey:evolve_starter')"
}
```

**Explanation**:

- **`event`**: `ENTITY_INTERACT` – The task progresses when the player interacts with an entity.
- **`event_data.uuid`**: Specifies the UUID of the NPC.
- **`filter`**: The interaction only counts if the player has completed the `'journey:evolve_starter'` task.

---

### 3. Set Target Conditions

The **`target`** defines what the player must achieve to complete the task. It varies based on the event type.

**Examples**:

- **Single Interaction**:

  ```json
  "target": 1
  ```

- **Multiple Items Pickup**:

  ```json
  "target": 5
  ```

- **Discovering Multiple Biomes**:

  ```json
  "target": 7
  ```

---

### 4. Assign Rewards

Rewards incentivize players to complete tasks. They can include currencies, items, commands, or custom rewards.

**Example**: Rewarding Poké Dollars and sending a message.

```json
"rewards": [
  {
    "type": "currency",
    "data": {
      "currency": "impactor:pokedollars",
      "amount": 500
    }
  },
  {
    "type": "command",
    "data": {
      "command": "tellraw {player} {\"text\":\"\",\"extra\":[{\"text\":\"Your mission awaits!\",\"color\":\"blue\"}]}"
    }
  }
]
```

---

### 5. Location Specification

Defining a **`location`** helps in tasks that are location-dependent, such as entering a specific area.

**Example**:

```json
"location": {
  "x": 245,
  "y": 23,
  "z": -360
}
```

---

### 6. Registering the Task

Once the task properties are defined, it must be registered within the Journey system. This typically involves adding the task to the `TaskRegistry`.

**Example**:

```json
{
  "id": "intro:speak_to_captain_phyco",
  "name": "<blue>Speak to Captain Phyco",
  "description": "<gray>Interact with Captain Phyco to receive your first mission.",
  "event": "ENTITY_INTERACT",
  "event_data": {
    "uuid": "2de91ae9-e18e-4419-bffe-43d1b6cdbb2e"
  },
  "filter": "q.entity.uuid == '2de91ae9-e18e-4419-bffe-43d1b6cdbb2e' && q.player.has_completed_task('journey:evolve_starter')",
  "target": 1,
  "location": {
    "x": 245,
    "y": 23,
    "z": -360
  },
  "rewards": [
    {
      "type": "currency",
      "data": {
        "currency": "impactor:pokedollars",
        "amount": 500
      }
    },
    {
      "type": "command",
      "data": {
        "command": "tellraw {player} {\"text\":\"\",\"extra\":[{\"text\":\"Your mission awaits!\",\"color\":\"blue\"}]}"
      }
    }
  ]
}
```

---

## Appendix

[Back to Table of Contents](./README.md)

### A. JSON Task Structure Reference

Here's a breakdown of the JSON structure used to define tasks in Journey:

```json
{
  "id": "unique_task_id",
  "name": "<color>Task Name",
  "description": "<gray>Task description.",
  "event": "EVENT_NAME",
  "event_data": {
    "key": "value"
  },
  "filter": "Molang expression",
  "target": target_value,
  "location": {
    "x": coordinate,
    "y": coordinate,
    "z": coordinate
  },
  "rewards": [
    {
      "type": "reward_type",
      "data": {
        "key": "value"
      }
    }
  ]
}
```

**Field Descriptions**:

- **`id`**: Unique identifier for the task (e.g., `intro:speak_to_captain_phyco`).
- **`name`**: Display name with optional color codes.
- **`description`**: Brief overview of the task.
- **`event`**: The in-game event that triggers task progress (e.g., `ENTITY_INTERACT`).
- **`event_data`**: Additional data pertinent to the event.
- **`filter`**: Molang expression to specify conditions for task progression or completion.
- **`target`**: The required progress to complete the task (e.g., `1` for single interaction).
- **`location`**: (Optional) Coordinates related to the task.
- **`rewards`**: List of rewards granted upon task completion.

---

### B. Molang Expression Syntax

Molang expressions are used to evaluate conditions within tasks. They follow a specific syntax that allows for logical operations and function calls.

**Basic Operators**:

- **Equality**: `==`
- **Inequality**: `!=`
- **Logical AND**: `&&`
- **Logical OR**: `||`
- **Greater Than**: `>`
- **Less Than**: `<`
- **Greater Than or Equal To**: `>=`
- **Less Than or Equal To**: `<=`

**Functions**:

- **String Functions**: `.contains()`, `.startsWith()`, `.endsWith()`
- **Numeric Functions**: `math.abs()`, `math.max()`, `math.min()`

**Example**:

```molang
q.player.has_completed_task('journey:evolve_starter') && q.zone.uuid == 'c106b35a-1058-4aca-809c-b9588c14ba11'
```

---

### C. Color Codes in Names and Descriptions

Journey supports color codes within task names and descriptions to enhance readability and aesthetics. The following color codes are commonly used:

- **`<blue>`**: Blue
- **`<gold>`**: Gold
- **`<gray>`**: Gray
- **`<green>`**: Green

**Example**:

```json
"name": "<gold>Welcome to Ultra Megalopolis",
"description": "<gray>Embark on your journey in Ultra Megalopolis. Complete the initial tasks to familiarize yourself with this expansive realm."
```

---

### D. Molang Functions Overview

Journey extends Cobblemon's default Molang functions with additional player-specific functions. Here's an overview of available functions:

#### Player Functions

- **`q.player.has_completed_task(taskName)`**: Checks if the player has completed the specified task.
- **`q.player.has_completed_subtask(taskName, subtaskName)`**: Checks if the player has completed the specified subtask.
- **`q.player.start_task(taskName)`**: Initiates the specified task for the player.
- **`q.player.has_item(itemId)`**: Checks if the player possesses the specified item.
- **`q.player.pokedex.has_pokemon(pokemonName)`**: Checks if the player's Pokédex contains the specified Pokémon.
- **`q.player.starter_pokemon`**: Retrieves the player's starter Pokémon details.
- **`q.player.is_in_zone(zoneUUID)`**: Checks if the player is currently within the specified zone.

#### Event-Specific Functions

- **`q.zone.uuid`**: Retrieves the UUID of the current zone.
- **`q.area.uuid`**: Retrieves the UUID of the current area within a zone.
- **`q.entity.uuid`**: Retrieves the UUID of the entity being interacted with.
- **`q.battle.is_wild`**: Determines if the battle was against a wild opponent.
- **`q.battle.opponent.uuid`**: Retrieves the UUID of the battle opponent.
- **`q.battle.team.contains_starter`**: Checks if the player's team includes the starter Pokémon.
- **`q.item.id`**: Retrieves the registry name of the item involved in the event.
- **`q.item.count`**: Retrieves the quantity of the item involved in the event.
- **`q.pokemon`**: Retrieves detailed information about a Pokémon involved in the event.

---

## Creating a Task

[Back to Table of Contents](./README.md)

Creating a task in Journey involves defining its properties, associated events, filters, and rewards. This guide provides a step-by-step process to design and implement a task effectively.

### Step 1: Define Task Properties

Start by outlining the fundamental properties of your task.

**Required Fields**:

- **`id`**: Unique identifier for the task (e.g., `intro:speak_to_captain_phyco`).
- **`name`**: Display name with optional color codes (e.g., `<blue>Speak to Captain Phyco`).
- **`description`**: Brief description of the task's objective.
- **`event`**: The event that triggers or progresses the task (e.g., `ENTITY_INTERACT`).
- **`event_data`**: Additional data related to the event (e.g., entity UUID).
- **`filter`**: Molang expression to filter or evaluate task conditions.
- **`target`**: The goal or condition required to complete the task.
- **`location`**: (Optional) Coordinates related to the task.
- **`rewards`**: List of rewards granted upon task completion.

### Step 2: Specify Events and Filters

Determine the event that will drive the task's progression and define the conditions using Molang expressions.

**Example**: Player interacts with Captain Phyco after evolving their starter Pokémon.

```json
"event": "ENTITY_INTERACT",
"event_data": {
  "uuid": "2de91ae9-e18e-4419-bffe-43d1b6cdbb2e"
},
"filter": "q.entity.uuid == '2de91ae9-e18e-4419-bffe-43d1b6cdbb2e' && q.player.has_completed_task('journey:evolve_starter')"
```

### Step 3: Set Target Conditions

Define what the player needs to achieve to complete the task.

**Example**: The player needs to interact with Captain Phyco once.

```json
"target": 1
```

### Step 4: Assign Rewards

Determine the incentives for task completion, such as currencies, items, or custom commands.

**Example**: Reward the player with Poké Dollars and send a confirmation message.

```json
"rewards": [
  {
    "type": "currency",
    "data": {
      "currency": "impactor:pokedollars",
      "amount": 500
    }
  },
  {
    "type": "command",
    "data": {
      "command": "tellraw {player} {\"text\":\"\",\"extra\":[{\"text\":\"Your mission awaits!\",\"color\":\"blue\"}]}"
    }
  }
]
```

### Step 5: Specify Location (Optional)

If the task is location-dependent, provide the coordinates.

**Example**:

```json
"location": {
  "x": 245,
  "y": 23,
  "z": -360
}
```

### Step 6: Registering the Task

After defining all properties, register the task within the Journey system by adding it to the `TaskRegistry`.

**Example Task JSON**:

```json
{
  "id": "intro:speak_to_captain_phyco",
  "name": "<blue>Speak to Captain Phyco",
  "description": "<gray>Interact with Captain Phyco to receive your first mission.",
  "event": "ENTITY_INTERACT",
  "event_data": {
    "uuid": "2de91ae9-e18e-4419-bffe-43d1b6cdbb2e"
  },
  "filter": "q.entity.uuid == '2de91ae9-e18e-4419-bffe-43d1b6cdbb2e' && q.player.has_completed_task('journey:evolve_starter')",
  "target": 1,
  "location": {
    "x": 245,
    "y": 23,
    "z": -360
  },
  "rewards": [
    {
      "type": "currency",
      "data": {
        "currency": "impactor:pokedollars",
        "amount": 500
      }
    },
    {
      "type": "command",
      "data": {
        "command": "tellraw {player} {\"text\":\"\",\"extra\":[{\"text\":\"Your mission awaits!\",\"color\":\"blue\"}]}"
      }
    }
  ]
}
```

**Explanation of Fields**:

- **`id`**: Unique identifier combining task category and name.
- **`name`**: Task title with color coding for emphasis.
- **`description`**: Brief instructions for the player.
- **`event`**: Specifies that the task progresses upon entity interaction.
- **`event_data.uuid`**: The UUID of Captain Phyco NPC.
- **`filter`**: Ensures the task progresses only when interacting with Captain Phyco and after completing the prerequisite task.
- **`target`**: Player needs to interact once to complete the task.
- **`location`**: Coordinates of Captain Phyco's location.
- **`rewards`**: Poké Dollars and a command to send a message to the player.

---

### Example Task Breakdown

Let's dissect the example task to understand each component.

#### Task ID

```json
"id": "intro:speak_to_captain_phyco"
```

- **`intro`**: Category or phase of the quest.
- **`speak_to_captain_phyco`**: Descriptive action the player must perform.

#### Task Name

```json
"name": "<blue>Speak to Captain Phyco"
```

- **`<blue>`**: Color code to display the task name in blue.
- **`Speak to Captain Phyco`**: Clear instruction for the player.

#### Task Description

```json
"description": "<gray>Interact with Captain Phyco to receive your first mission."
```

- **`<gray>`**: Color code to display the description in gray.
- **`Interact with Captain Phyco to receive your first mission.`**: Detailed explanation of the task.

#### Event and Event Data

```json
"event": "ENTITY_INTERACT",
"event_data": {
  "uuid": "2de91ae9-e18e-4419-bffe-43d1b6cdbb2e"
}
```

- **`ENTITY_INTERACT`**: The task progresses when the player interacts with an entity.
- **`uuid`**: Specifies the exact entity (Captain Phyco) the player must interact with.

#### Filter

```json
"filter": "q.entity.uuid == '2de91ae9-e18e-4419-bffe-43d1b6cdbb2e' && q.player.has_completed_task('journey:evolve_starter')"
```

- Ensures that the interaction is with Captain Phyco **and** the player has completed the `'journey:evolve_starter'` task.

#### Target

```json
"target": 1
```

- The player needs to interact **once** to complete the task.

#### Location

```json
"location": {
  "x": 245,
  "y": 23,
  "z": -360
}
```

- Coordinates of Captain Phyco's location in Ultra Megalopolis.

#### Rewards

```json
"rewards": [
  {
    "type": "currency",
    "data": {
      "currency": "impactor:pokedollars",
      "amount": 500
    }
  },
  {
    "type": "command",
    "data": {
      "command": "tellraw {player} {\"text\":\"\",\"extra\":[{\"text\":\"Your mission awaits!\",\"color\":\"blue\"}]}"
    }
  }
]
```

- **Poké Dollars**: 500 awarded to the player.
- **Command**: Sends a colored message to the player upon task completion.

---

## Appendix

[Back to Table of Contents](./README.md)

### A. Example Task JSON

Below is a complete example of a task JSON structure for reference.

```json
{
  "name": "<gold>Welcome to Ultra Megalopolis",
  "description": [
    "<gray>Embark on your journey in Ultra Megalopolis. Complete the initial tasks to familiarize yourself with this expansive realm."
  ],
  "sequential": true,
  "rewards": [
    {
      "type": "currency",
      "data": {
        "currency": "impactor:pokedollars",
        "amount": 1000
      }
    }
  ],
  "icon": {
    "item_id": "cobblemon:ultra_badge"
  },
  "repeat_type": "NONE",
  "repeat_interval": 0,
  "tasks": [
    {
      "id": "intro:speak_to_nova",
      "name": "<blue>Speak to Space Cadet Nova",
      "description": "<gray>Interact with Space Cadet Nova to learn about Ultra Space.",
      "event": "ENTITY_INTERACT",
      "event_data": {
        "uuid": "110ea106-6dd0-4b64-8f08-481f8dde4306"
      },
      "filter": "q.entity.uuid == '110ea106-6dd0-4b64-8f08-481f8dde4306'",
      "target": 1,
      "location": {
        "x": 342,
        "y": 20,
        "z": -268
      },
      "rewards": [
        {
          "type": "currency",
          "data": {
            "currency": "impactor:pokedollars",
            "amount": 200
          }
        },
        {
          "type": "command",
          "data": {
            "command": "tellraw {player} {\"text\":\"\",\"extra\":[{\"text\":\"Great! Let's get you started.\",\"color\":\"green\"}]}"
          }
        }
      ]
    },
    {
      "id": "intro:observe_tower",
      "name": "<blue>Observe the Tower",
      "description": "<gray>Enter the observation zone to learn more about the central tower.",
      "event": "ENTER_ZONE",
      "event_data": {
        "uuid": "c106b35a-1058-4aca-809c-b9588c14ba11"
      },
      "filter": "q.zone.uuid == 'c106b35a-1058-4aca-809c-b9588c14ba11'",
      "target": 1,
      "location": {
        "x": 245,
        "y": 23,
        "z": -262
      },
      "rewards": [
        {
          "type": "currency",
          "data": {
            "currency": "impactor:pokedollars",
            "amount": 300
          }
        },
        {
          "type": "command",
          "data": {
            "command": "tellraw {player} {\"text\":\"\",\"extra\":[{\"text\":\"The tower is the heart of Ultra Megalopolis.\",\"color\":\"blue\"}]}"
          }
        }
      ]
    },
    {
      "id": "intro:speak_to_captain_phyco",
      "name": "<blue>Speak to Captain Phyco",
      "description": "<gray>Interact with Captain Phyco to receive your first mission.",
      "event": "ENTITY_INTERACT",
      "event_data": {
        "uuid": "2de91ae9-e18e-4419-bffe-43d1b6cdbb2e"
      },
      "filter": "q.entity.uuid == '2de91ae9-e18e-4419-bffe-43d1b6cdbb2e' && q.player.has_completed_task('journey:evolve_starter')",
      "target": 1,
      "location": {
        "x": 245,
        "y": 23,
        "z": -360
      },
      "rewards": [
        {
          "type": "currency",
          "data": {
            "currency": "impactor:pokedollars",
            "amount": 500
          }
        },
        {
          "type": "command",
          "data": {
            "command": "tellraw {player} {\"text\":\"\",\"extra\":[{\"text\":\"Your mission awaits!\",\"color\":\"blue\"}]}"
          }
        }
      ]
    }
  ]
}
```

**Key Points**:

- **Sequential Tasks**: The `sequential` field indicates whether tasks must be completed in order.
- **Icon**: Represents the task visually in the quest log.
- **Repeatable**: The `repeat_type` and `repeat_interval` determine if and how often the task can be repeated.

---

### E. Journey Molang Functions

Journey extends Cobblemon's default Molang functions with additional player-specific functions. These functions are accessible within Molang expressions to create dynamic and conditional quest behaviors.

#### Default Cobblemon Player Functions

- **`q.player.has_completed_task(taskName)`**: Checks if the player has completed a specific task.
- **`q.player.has_completed_subtask(taskName, subtaskName)`**: Checks if the player has completed a specific subtask within a task.
- **`q.player.start_task(taskName)`**: Initiates a new task for the player.
- **`q.player.has_item(itemId)`**: Checks if the player possesses a specific item.
- **`q.player.pokedex`**: Retrieves the player's Pokédex data.
- **`q.player.starter_pokemon`**: Retrieves the player's starter Pokémon details.
- **`q.player.is_in_zone(zoneUUID)`**: Checks if the player is currently within a specific zone.

#### Journey-Specific Molang Functions

These functions are additions from the `JourneyMolang` setup.

- **`q.zone.uuid`**: Retrieves the UUID of the current zone.
- **`q.area.uuid`**: Retrieves the UUID of the current area within a zone.
- **`q.entity.uuid`**: Retrieves the UUID of the entity being interacted with.
- **`q.battle.is_wild`**: Determines if the battle was against a wild opponent.
- **`q.battle.opponent.uuid`**: Retrieves the UUID of the battle opponent.
- **`q.battle.team.contains_starter`**: Checks if the player's team includes the starter Pokémon.
- **`q.item.id`**: Retrieves the registry name of the item involved in the event.
- **`q.item.count`**: Retrieves the quantity of the item involved in the event.
- **`q.pokemon`**: Retrieves detailed information about a Pokémon involved in the event.

#### Example Molang Expressions in Filters

**1. Checking Player Task Completion**

```molang
q.player.has_completed_task('journey:evolve_starter')
```

**2. Interacting with a Specific NPC After Task Completion**

```molang
q.entity.uuid == '2de91ae9-e18e-4419-bffe-43d1b6cdbb2e' && q.player.has_completed_task('journey:evolve_starter')
```

**3. Entering a Specific Zone**

```molang
q.zone.uuid == 'c106b35a-1058-4aca-809c-b9588c14ba11'
```

**4. Battle Victory Against a Wild Pokémon**

```molang
q.battle.is_wild == 1.0 && q.battle.opponent.uuid == 'opponent-uuid'
```

**5. Picking Up a Specific Item**

```molang
q.item.id == 'cobblemon:ultra_badge' && q.item.count >= 1.0
```

---

## Conclusion

The Journey system offers a robust framework for creating engaging quests and tasks within your Cobblemon server. By leveraging events and Molang queries, you can design dynamic and conditional quests that respond to player actions and in-game events. This documentation serves as a foundational guide to understanding and utilizing Journey's capabilities effectively.

For further assistance or advanced configurations, refer to the [Appendix](./README.md#appendix) or reach out to the Journey development community.

Happy questing!