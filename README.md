<h1 style="color: gold;">Pocket Repose</h1>

HELLO FRIENDS!
  Pocket Repose is a Minecraft mod that introduces the Traveler's Suitcase block, offering players access to their very own pocket dimension. 
  Pocket dimensions are created through renaming the Keystone item. 
  Every dimension is unique to the Keystone's name, allowing for privacy and multiplayer fun.

## Features

- **Suitcase Block:** Place and interact with the suitcase block to access your pocket dimensions.
- **Private Dimensions:** Create custom dimensions that serve as a personal space for exploration, storage, and building.
- **Unique Keys:** Each new dimension is custom. Rename your key to give your dimension a unique identity.
- **Entry and Exit:** Seamlessly enter and exit your private dimension using the suitcase block. Multiplayer compatible. Let your friends carry you around!

## Work In Progress

- **Mob Entry:** Right-click on mobs to store in your suitcase.

## Minecraft 26.2

This branch targets Minecraft 26.2 and is ported from the known-working 1.20.1
implementation. It requires:

- Fabric Loader 0.19.5 or newer
- Fabric API 0.159.0+26.2
- Fantasy 0.8.3+26.2
- Java 25

Install the three mod JARs (Pocket Repose, Fabric API, and Fantasy) in the same
Fabric 26.2 `mods` folder. Do not load worlds you care about without making a
backup first; Minecraft 26.2 world upgrades are not reversible.

### Smoke test

1. Craft and rename a Keystone in an anvil.
2. Use the renamed Keystone to create its pocket dimension.
3. Bind it to a suitcase, open the suitcase, sneak, and enter.
4. Exit through the portal floor and confirm you return to that suitcase.
5. Break and replace the bound suitcase, then confirm its binding, lock state,
   and any traveler warning are preserved.
