**# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Time is following the [Holocene Calendar](https://en.wikipedia.org/wiki/Holocene_calendar).

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
