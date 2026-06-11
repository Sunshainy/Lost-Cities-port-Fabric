# Changelog

All notable changes to this project will be documented in this file.

## [0.8.0-beta] - 2026-06-11

### Added
- **Rail Corridors**: Implemented underground connections between buildings.
- **Scattered Stuff**: Added debris, trash, and decorative elements to city streets.

### Optimized
- **QualityRandom**: Replaced vanilla Random with a custom implementation for better performance and deterministic generation.
- **Worldgen Logic**: Improved palette selection and enhanced generation to better utilize world seed and building data.
- **Rotation**: Refactored rotation logic to ensure consistent 16x16 chunk-relative placement for building parts and stairs.

### Fixed
- **Profile Selection**: Fixed issues in GUI and configuration logic.
- **Build Environment**: Corrected `.gitignore` rules that were accidentally omitting the configuration package.

### Changed
- **Compatibility**: Expanded support to include both Minecraft 1.20 and 1.20.1.
- **Mod Metadata**: Updated versioning and narrowed dependencies.
