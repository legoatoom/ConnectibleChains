# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Time is following the [Holocene Calendar](https://en.wikipedia.org/wiki/Holocene_calendar).

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
- Chain_Knot had no lang name.
