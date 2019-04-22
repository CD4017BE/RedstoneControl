This is basically a full remake of my previous Minecraft mod Automated Redstone and it aims to takes on the same basic concepts but make everything better.

- Blocky cables were replaced with a straight point A -> point B wire system similar to Immersive Engineering but different. The goal is to make wiring more convenient and getting rid of block side limitations for connecting signals to devices (which previously often lead to unnecessary complicated side configuration GUIs)
- This comes with a new internal signal transmission system that's far more performance efficient that regular vanilla Redstone transmission because it sends signal changes directly into the logic code of the receiver device without going through countless block updates, neighbor block checks and TileEntity lookups. (of course there are features in place to easily move signals between those two systems)
- Most features of Automated Redstone are planned to come back in a different form, including of course programmable circuits

## Project Setup
- First setup the required [CD4017BE_lib](https://github.com/CD4017BE/CD4017BE_lib#project-setup-for-mc-1112-and-newer) project
- And then add this project in the same way. All projects should be located in the same parent directory and imported into a common eclipse workspace.
