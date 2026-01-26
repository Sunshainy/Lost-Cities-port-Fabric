# 🏙️ Lost City - Fabric Port (1.20.1)

**Full-featured port of Lost Cities mod from Forge to Fabric for Minecraft 1.20.1**

## ⚠️ IMPORTANT WARNING

**This mod is currently in BETA development stage (~60-65% complete).**

**Please be aware that:**
- 🐛 **Bugs may occur** - the mod is still under active development
- ⚡ **Performance issues** - optimization is ongoing, you may experience lag or frame drops
- 🎨 **Visual artifacts** - some rendering issues may appear
- 💥 **Crashes possible** - the mod may crash in certain situations
- 🔧 **Incomplete features** - many features from the original mod are not yet implemented

**Use at your own risk!** We recommend making backups of your worlds before using this mod.

**Version Status:** BETA - Not recommended for production use

---

## 📖 About the Project

This mod is a port of the original **Lost Cities** mod by **McJty** from Forge to Fabric. The port is created by **sunshainy** and is inspired by the original mod, striving to maintain identical functionality and gameplay experience.

**Original Mod (Forge):** [https://github.com/McJtyMods/LostCities](https://github.com/McJtyMods/LostCities)

**Porting Status:** ~60-65% complete in terms of world generation functionality

### ✅ What's Already Implemented:
- ✅ Core world generation (95%)
- ✅ Building, street, highway, and bridge generation
- ✅ Asset and palette system
- ✅ Profile selection GUI when creating a world
- ✅ Terrain and block correction
- ✅ 17 standard generation profiles

### 🔄 In Development:
- ⏳ Structure generators (Corridors, Monorails, Railways, Scattered, Spheres)
- ⏳ Structure avoidance system
- ⏳ API for compatibility with other mods
- ⏳ Commands and configuration GUI

---

## 🎮 Generation Profiles

When creating a world, you can choose one of the following profiles:

### 🌆 Main Profiles

#### **default** - Standard Generation
- **Description:** Classic abandoned city generation
- **Features:** 
  - Regular cities with streets and buildings
  - Explosions and destruction enabled
  - Standard building height (0-9 floors)
  - Cellars up to 6 levels

#### **cavern** - Cities in Caves
- **Description:** Cities generate in large underground caves
- **Features:**
  - Ground level: 40 blocks
  - Building lighting enabled
  - No explosions or destruction
  - Railways disabled
  - **Recommendation:** Use with Lost Worlds 'caves' world type mod

#### **nodamage** - No Damage
- **Description:** Like default, but without explosions and destruction
- **Features:**
  - No explosions
  - No ruins
  - No rubble layer
  - Perfect for safe exploration

#### **floating** - Floating Islands
- **Description:** Cities on floating islands in the air
- **Features:**
  - City chance: 3%
  - No supports under bridges and highways
  - Maximum 1 cellar
  - Railways can end
  - **Recommendation:** Use with Lost Worlds 'islands' world type

#### **space** - Space Spheres
- **Description:** Cities in glass spheres floating in space
- **Features:**
  - 90% chance of city sphere generation
  - Space clearing around spheres
  - Minimal explosions
  - City chance: 70%
  - Maximum radius: 90 blocks
  - **Recommendation:** Use with Lost Worlds 'spheres' world type

#### **biosphere** - Biospheres
- **Description:** Jungles in glass spheres on barren landscape
- **Features:**
  - City chance: 80%
  - 70% ruin chance
  - No monorails
  - Lighting enabled
  - **Recommendation:** Use with Lost Worlds 'normal' world type

#### **biosphere_caves** - Biospheres in Caves
- **Description:** Spheres with jungles in large caves
- **Features:**
  - City chance: 90%
  - 70% ruin chance
  - Lighting enabled
  - **Recommendation:** Use with Lost Worlds 'cavespheres' world type

### 🎯 Special Profiles

#### **rarecities** - Rare Cities
- **Description:** Cities are very rare
- **Features:**
  - City chance: 0.1%
  - No ruins
  - Highways don't require two cities
  - Railways can end

#### **onlycities** - Only Cities
- **Description:** The entire world is one big city
- **Features:**
  - City chance: 20%
  - Maximum radius: 256 blocks
  - Huge megacities

#### **tallbuildings** - Tall Buildings
- **Description:** Very tall buildings (performance intensive!)
- **Features:**
  - Minimum 4 floors
  - Maximum 19 floors
  - Increased explosions
  - More destruction

#### **safe** - Safe Mode
- **Description:** No spawners, with lighting, but no loot
- **Features:**
  - No mob spawners
  - Lighting enabled
  - No loot in chests
  - Perfect for building

#### **ancient** - Ancient City
- **Description:** Ancient jungle city with vines and leaves
- **Features:**
  - Thick leaf layer (6 blocks)
  - 10% vine chance on walls
  - 90% ruin chance
  - Rubble layer enabled

#### **wasteland** - Wasteland
- **Description:** Wasteland without water, bare landscape
- **Features:**
  - All water replaced with air
  - No vegetation in parks
  - 50% ruin chance
  - Minimal vines and leaves
  - **Recommendation:** Works better with Biomes O Plenty and Wastify mods

#### **atlantis** - Atlantis
- **Description:** Drowned cities with raised water level
- **Features:**
  - Sea level: 89 blocks
  - 10% ruin chance
  - Cities underwater
  - **Recommendation:** Use with Lost Worlds 'atlantis' world type

#### **largecities** - Large Cities
- **Description:** Uses Perlin noise for large city generation
- **Features:**
  - Uses noise for location determination
  - Alternative style on city edges
  - Maximum 9 floors
  - Lighting enabled
  - Building chance: 40%

### 🔒 Private Profiles (for internal use)

- **bio_wasteland** - Wasteland for biospheres
- **void_outside** - Void for space spheres

---

## 🎮 How to Use

### 1. Installation

1. Install **Fabric Loader** (0.15.0+) for Minecraft 1.20.1
2. Install **Fabric API**
3. Place the JAR file in the `mods/` folder

### 2. Creating a World

1. Click **"Create New World"**
2. Click the **"Cities"** button
3. Select a profile:
   - **Disabled** - normal world without cities
   - Any of the available profiles above

### 3. Configuration

Configuration file: `config/lostcities.json`

---

## 📦 Datapack Support

The mod fully supports datapacks! You can:
- 🏗️ Add new buildings via JSON
- 🎨 Change palettes and styles
- 🧱 Add new building parts
- 🏢 Create multibuildings (2×2, 3×3 chunks)

**Datapack path:** `saves/<world>/datapacks/your_pack/data/lostcities/`

---

## 🔍 Loading Check

Check `logs/latest.log`:
```
[INFO] (Lost City) Initializing standard profiles...
[INFO] (Lost City) Initialized 17 standard profiles: default, cavern, nodamage, ...
[INFO] (Lost City) === AssetRegistries.load START ===
[INFO] (Lost City) Loaded XX buildings, XX palettes, XX parts...
```

---

## 📋 System Requirements

- **Minecraft:** 1.20.1
- **Fabric Loader:** >= 0.15.0
- **Fabric API:** >= 0.92.7
- **Java:** >= 17

---

## 🙏 Credits

- **McJty** - author of the original Lost Cities mod (Forge) - [GitHub](https://github.com/McJtyMods/LostCities)
- **Fabric Team** - for the excellent API
- **Community** - for testing and feedback

---

## 📊 Porting Status

**Overall Completion:** ~60-65% in terms of world generation functionality

### ✅ Implemented:
- Core world generation (95%)
- Building, street, highway, and bridge generation
- Asset and palette system
- Profile selection GUI
- 17 standard profiles
- Terrain and block correction

### ⏳ In Development:
- Structure generators (Corridors, Monorails, Railways, Scattered, Spheres)
- Structure avoidance system
- API for compatibility with other mods
- Commands and configuration GUI
- Building part editor

---

## 📝 License

MIT License

---

## 👤 Authors

- **Port Author:** sunshainy
- **Original Mod:** McJty (Lost Cities for Forge) - [GitHub](https://github.com/McJtyMods/LostCities)

---

**Enjoy exploring abandoned cities!** 🏙️🎮✨
