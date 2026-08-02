# Vendor assets (gitignored)

Licensed 3D assets live here. Never commit binary files — vendor licenses
usually allow commercial use but prohibit redistribution of the source files.

## Stadiums

```
vendor-assets/
  wimbledon/
    TennisStadium.fbx
    Textures/              (extracted textures)
  indoor/
    tennisstadiumv4.glb
```

Convert Wimbledon with:

```bash
/Applications/Blender.app/Contents/MacOS/Blender --background --python convert_wimbledon.py -- \
  "$PWD/wimbledon/TennisStadium.fbx" \
  "$PWD/wimbledon/Textures/Textures" \
  "$PWD/../public/models/stadium_grass.glb"
```

Runtime (also gitignored):

```
public/models/
  stadium_grass.glb
  stadium_hard.glb
```

## Players (Motion Cast — current pipeline)

Mocap pack laid out as:

```
vendor-assets/players/mocap/
  tennisman/                 # and tenniswoman/
    3-IDLE-no root.fbx
    4-Moving from left to right-Speed2-no root.fbx
    8-Service ball-Take1-no root.fbx
    9-Hit 1 Speed1-no root.fbx
    17-Backhand Slice-Take2-no root.fbx   # 22-Backhand Slice on tenniswoman
    18-Smash-Take1-no root.fbx            # 18-Smash-proxy on tenniswoman
    Blender/tex/{albedo,roughness,normal}.png
```

Any clip FBX works as the base mesh — they all carry the skinned mannequin.
The converter wires albedo + roughness + normal, renames actions to
`idle/jog/serve/forehand/backhand/smash`, and fails if one is missing.

The skinned mesh's material is renamed to `tennisly_body`. That id is a
contract with `scene/playerKit.ts`, which recolours exactly that material into
per-side broadcast kits — the racket shares the GLB and keeps its own diffuse.

```bash
cd apps/web/vendor-assets
for g in "tennisman:player_male" "tenniswoman:player_female"; do
  src="${g%%:*}"; out="${g##*:}"
  /Applications/Blender.app/Contents/MacOS/Blender --background --python convert_players.py -- \
    "$PWD/players/mocap/$src/3-IDLE-no root.fbx" \
    "$PWD/players/mocap/$src" \
    "$PWD/players/mocap/$src/Blender/tex/albedo.png" \
    "$PWD/../public/models/players/$out.glb"
done
```

Verify the clips and material ids actually survived the glTF export:

```bash
python3 -c "
import json,struct
d=open('apps/web/public/models/players/player_male.glb','rb').read()
n=struct.unpack('<I',d[12:16])[0]
j=json.loads(d[20:20+n])
print([a['name'] for a in j['animations']])
print([m['name'] for m in j['materials']])"
```

## Players (Mixamo — superseded)

Download from [mixamo.com](https://www.mixamo.com) (free Adobe account):

1. Pick a character → Download as **FBX Binary**, **T-Pose**, without skin if you plan one mesh — or **With Skin**.
2. With the same character selected, download animations as **FBX** (30 fps, without skin if reusing character mesh):
   - Idle
   - Walking / Jogging
   - Tennis-related if available (or sports plant / swing proxies)
3. Place files here:

```
vendor-assets/
  players/
    home/
      character.fbx          # or .glb if already converted
      idle.fbx
      jog.fbx
      forehand.fbx           # optional v1
      backhand.fbx           # optional v1
      serve.fbx              # optional v1
    away/
      character.fbx
      idle.fbx
      jog.fbx
      ...
```

Then convert each to GLB (Blender pack + glTF export) into:

```
public/models/players/
  home.glb                   # character + embedded clips, or separate files
  away.glb
```

Optional racket:

```
vendor-assets/players/racket.glb
public/models/players/racket.glb
```
