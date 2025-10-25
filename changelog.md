# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Time is following the [Holocene Calendar](https://en.wikipedia.org/wiki/Holocene_calendar).

## [2.5.5] - 12025-10-25

- Fixed issue when chains removed when one end was unloaded.

## [2.5.4] - 12025-06-04

- Fixed issue when chain is broken by TNT explosion [#86](https://github.com/legoatoom/ConnectibleChains/issues/86)

## [2.5.3] - 12025-05-07

- Added `nl_nl` translations
- Updated `es_ar` translation by courtesy of [Texaliuz](https://github.com/Texaliuz)
- Fixed duplication bug [#78](https://github.com/legoatoom/ConnectibleChains/issues/78)
- Support for Supplementaries Rope [#72](https://github.com/legoatoom/ConnectibleChains/issues/72)
- Introduced more debug logging when connections break.

## [2.5.2] - 12025-01-29

- UV Configuration in model JSON
- Support for Farmers Delights Rope

## [2.5.1] - 12024-12-24

- Hotfix fallback chain texture incorrect.

## [2.5.0] - 12024-12-24
- Reworked logic and rendering of the mod.
- Implemented 2 new rendering models: `connectiblechains:square` and `connectiblechains:plus`
  - See wiki for more info.

### Fixes

- Fixed bug of randomly broken chains when one end is in an unloaded
  chunk. [#68](https://github.com/legoatoom/ConnectibleChains/issues/68)
- While leading animals, right-clicking with a lead in hand now first tries to attach animals to a
  fence. [#65](https://github.com/legoatoom/ConnectibleChains/issues/65)
- Connecting to existing uses the correct sound. [#64](https://github.com/legoatoom/ConnectibleChains/issues/64)

## [2.4.2] - 12024-08-15
- Fixed pick block bug where you would always get a vanilla chain.

## [2.4.1] - 12024-06-14
- The mod now uses `connectiblechains:catenary_items` tag for allowed items to be used that are not chains.
- Leads are now also available to use as catenary.
- Fixed bug where custom textures were working on initial launch.
- Items now grab the sound of their block variant, if there isn't one, it defaults to chains.
- The chain drip function is altered sightly to decrease drip for longer chains.
- Added `es_mx` translation by courtesy of [TheLegendofSaram](https://github.com/TheLegendofSaram)
  - Copied language to `es_ar`,`es_cl`,`es_ec`, `es_es`, `es_uy`, and `es_ve`.
  - If you noticed any mistake due to regional differences in the Spanish language, make an issue or pull request.
- Added `ru_ru` translation by courtesy of [Alexander317](https://github.com/Alexander317)

## [2.4.0] - 12024-06-06
- Reworking Networking again since it changed again.
- Increased visibility range, equal to other entities, like chests.
- Default max range increased from 7 to 16.
- Reworked Tag system to use the `fabric-convention-tags-v2`.
- Reworked how the mod grabs textures for the `knot` and `chain` texture.
  - For every item in the `chains` convention tag, we check if JSON files is located at
  `<mod-id>/models/entity/connectiblechains/<item_id>.json`, see the `chain.json` file on GitHub for an example.
  - If this file is missing, the default locations (what currently happens) are used.
  - Note: Animated textures do **not** work.
  

## [2.3.0] - 12024-03-30
- Reworked Networking to conform with new standard.
- Removed all mixins, as they were no longer needed (I hope).
- Added `c:bars` tags and allowed chains to be connected to iron bars.
- Minecraft version 1.20.4

## [2.2.1] - 12023-06-29
- Fixed texture bug when having one chain in hand.

## [2.2.0] - 12023-04-16
- Fix Tooltip Config not Working
- Added new text for the tooltip.
- Reworked the compatibility to be tag-based.
  - Using `data/c/tags/items/chains.json` we check if an item can be used to make a catenary.\
  The code then assumes an `item` and `block`texture for the `knot` and `catenary`. \
  This implies that you can make anything a compatible chain, as long as you have textures available.
  For modded players, you need to add a data pack where you  add tags for the chain items you want added.

## [2.1.4] - 12022-11-18
- Fix Gold Chain Charm
- Added `nether_brass_chain` from Architects Palette
- Added `golden_chain` from Blockus
- Added config to remove the tooltip.

## [2.1.3] - 12022-09-08
- Support for Golden Chain of Dustrial Decor
- Crash Fixed with ShowMeYourSkin mod.

## [2.1.2] - 12022-09-08
- Support for Charm and Mythicmetals Decorations chains.
- Added tooltip for supported chains.
- Resources file structure changed, wiki will be adapted accordingly.

## [2.1.1] - 12022-03-26
- Updated version specifically for 1.18.2.

## [2.1.0] - 12022-03-21
### Added
- Compatibility API for other mods
- New chains for Valley Craft, BetterNether and Better End

### Fixed
- Sound and animation issues
- Drops is creative mode
- Not being able to place held chains when not holding a chain item


## [2.0.1] - 12022-01-17
### Fixed
- Servers not working

## [2.0.0] - 12022-01-14
### Fixed
- Fixed Collision doesn't work for mobs #7
- Fixed Max Chain Distance only taking effect after restart
- Improved collider placement
- Fixed left over knots when a chain breaks because it's too long
- Improved lang file a little
- Sync chain config to client
- Replaced chain line renderer fixing many issues
- Fixed UV stretching (In some cases the pixels still aren't acute trapezoids but obtuse)
- Fixed hard coded texture
- Fixed very high poly count
- Added adjustable quality setting
- Fixed pixel gaps between polygons
- Added model caching
- Added debug rendering
- Improved lighting
- Fixed incorrect chain position on first frames after a new connection

### Credit
- Thanks to Qendolin for this update. 

## [1.2.3] - 12021-10-03
### Fixed
- Issue of connecting chains twice
- Rendering Crash

## [1.2.2] - 12021-07-25
### Changed
- Mobs also effected by chain collision
- Better Rendering Chain
- Arrows bounce off

### Fixed
- Rendering Chain Issue

### Added
- Support for mod-menu and auto config. (Drag and Max Range) 


## [1.2.1] - 12021-04-01
### Changed
- Chains can now only be broken by a player holding a shear.
    - This is based on fabric shear tag, so if other mods add shears they should also work.
- Chains now also check if ENTITY_DROP gamerule is on before dropping a chain.
- Chain Collisions are now visible when using F3+B when holding a shear.

### Fixed
- Duplication Glitch


## [1.2.0] - 12021-03-30
### Added
- Collision and hit-box added to the chain, not when connected to a player.
- Chains can be connected to a wall.

### Fixed
- Optimized and clearer chain rendering code.
- Fixed small mistakes.
- Updated deprecated code.

## [1.1.0] - 12020-11-04
### Added
- This Changelog

### Fixed
- Chains disappear after leaving the area
- Some data was not synchronized
- Chain_Knot had no lang name.**
