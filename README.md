<!--suppress HtmlDeprecatedAttribute, HtmlRequiredAltAttribute -->
<p align="center"><img src="https://raw.githubusercontent.com/legoatoom/ConnectibleChains/master/src/main/resources/assets/logo.png" height="128"></p>
<h3 align="center">Connectible Chains</h3>
<p align="center">Connect your fences and walls with a decorative chain!</p>
<p align="center">
  <a title="Fabric API" href="https://github.com/FabricMC/fabric">
    <img src="https://i.imgur.com/Ol1Tcf8.png" width="151" height="50">
  </a>
  <a title="Cloth API" href="https://modrinth.com/mod/cloth-config">
    <img src="https://i.imgur.com/7weZ8uu.png" width="151" height="50">
  </a>
</p>
<br>
<p align="center">
      <a href="https://www.gnu.org/licenses/lgpl-3.0.en.html"><img src="https://img.shields.io/github/license/legoatoom/ConnectibleChains?style=for-the-badge"></a>
        <a href="https://github.com/legoatoom/ConnectibleChains/releases"><img src="https://img.shields.io/github/v/release/legoatoom/ConnectibleChains?style=for-the-badge"></a>
        <a href="https://github.com/legoatoom/ConnectibleChains/issues"><img src="https://img.shields.io/github/issues-raw/legoatoom/ConnectibleChains?style=for-the-badge"></a>
        <br>
        <a href="https://www.curseforge.com/minecraft/mc-mods/connectible-chains"><img src="https://img.shields.io/badge/dynamic/json?color=f16436&logo=curseforge&query=downloads.total&url=https://api.cfwidget.com/415681&label=fabric&cacheSeconds=86400&style=for-the-badge"></a>
  <a href="https://www.curseforge.com/minecraft/mc-mods/connectible-chains-forge"><img src="https://img.shields.io/badge/dynamic/json?color=f16436&logo=curseforge&query=downloads.total&url=https://api.cfwidget.com/418514&label=forge&cacheSeconds=86400&style=for-the-badge"></a>
        <a href="https://modrinth.com/mod/connectible_chains"><img src="https://img.shields.io/modrinth/dt/ykSfIgTw?logo=modrinth&logoColor=white&style=for-the-badge"></a>
        <a href="https://github.com/legoatoom/ConnectibleChains"><img src="https://img.shields.io/github/downloads/legoatoom/ConnectibleChains/total?logo=GitHub&style=for-the-badge"></a>
</p>


## Description
Connect your fences/walls with a decorative chain!

Use the normal vanilla chain item on fences or walls, and you can create a chain for up to 7 blocks long (Configurable to 32 with AutoConfig).

These chains have collision and can be broken when attacking it with a shear. \
Sneak on fences if you do want to place them normally.

#### Images
<h6>Path</h6>
<img src="https://raw.githubusercontent.com/legoatoom/ConnectibleChains/9c61b5c2fbe298f6dc01db78fd62d47bf363a673/src/main/resources/assets/images/2022-01-14_22.03.12.png" height="250px">


#### Issues
If you have encountered any bugs or have some feedback, go to the GitHub [issues](https://github.com/legoatoom/ConnectibleChains/issues) page.

#### Versions
I will not be supporting versions lower than 1.19.4

#### Compatibility with other mods
Using `data/c/tags/items/chains.json` we check if an item can be used to make a catenary.\
The code then assumes an `item` and `block`texture for the `knot` and `catenary`. \
This implies that you can make anything a compatible chain, as long as you have textures available.
For modded players, you need to add a data pack where you  add tags for the modded chain items you want added.

See the example below to add `blockus:golden_chain`.
```json
{
  "replace": false,
  "values": [
    "minecraft:chain",
    "blockus:golden_chain"
  ]
}
```

#### Forge?
There is a Forge Port made for 1.18.2 Made by lilypuree
https://www.curseforge.com/minecraft/mc-mods/connectible-chains-forge


#### Other information

If there are invisible collisions for some reason, hold a shear while
having entity hitboxes on (F3+B) to see the collisions and then attack them to remove them.
