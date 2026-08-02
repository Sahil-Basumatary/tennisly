# Third-party assets

## Committed (CC0 — public domain, no attribution required)

| Asset | Source | Files |
| --- | --- | --- |
| Stadium 01 HDRI | [Poly Haven](https://polyhaven.com/a/stadium_01) | `public/textures/court/stadium_01_{1k,2k}.hdr` |
| Grass004 PBR set | [ambientCG](https://ambientcg.com/view?id=Grass004) | `public/textures/court/grass_*.jpg` |
| Ground027 PBR set | [ambientCG](https://ambientcg.com/view?id=Ground027) | `public/textures/court/clay_*.jpg` |
| Asphalt012 PBR set | [ambientCG](https://ambientcg.com/view?id=Asphalt012) | `public/textures/court/hard_*.jpg` |

## Licensed, not committed

Stadium models are purchased under royalty-free licenses that permit commercial
use but prohibit redistribution, so the files are gitignored
(`vendor-assets/`, `public/models/`) and loaded at runtime from local disk in
development or object storage in deployment. See `vendor-assets/README.md`
for the expected layout.

| Surface | Source | Runtime file | Notes |
| --- | --- | --- | --- |
| GRASS | CGTrader Wimbledon Centre Court (FBX + textures) | `public/models/stadium_grass.glb` | Converted via `vendor-assets/convert_wimbledon.py` |
| HARD | Fab Indoor Tennis Stadium (GLB) | `public/models/stadium_hard.glb` | Copied as-is |
| CLAY | Same as HARD | `public/models/stadium_hard.glb` | Indoor bowl + our clay PBR court mesh |

### Players (Option 1 characters + Option 2 Motion Cast mocap)

| Role | Source | Runtime | Notes |
| --- | --- | --- | --- |
| Male athlete | Unity Motion Cast #13 Tennisman | `public/models/players/player_male.glb` | idle/jog/serve/FH/BH/smash |
| Female athlete | Unity Motion Cast #01 Tenniswoman | `public/models/players/player_female.glb` | same clip set |
| Mixamo bodies (Adam/Remy/Sophie/Jody) | Mixamo FBX | `vendor-assets/players/{male,female}/…` | staged for optional retarget onto mocap |

