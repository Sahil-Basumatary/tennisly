# Vendor assets (gitignored)

Licensed stadium models live here. Never commit these files — the vendor
licenses allow commercial use but prohibit redistribution.

Expected layout after purchase downloads:

```
vendor-assets/
  wimbledon/
    TennisStadium.fbx
    Textures.zip          (or the extracted Textures/ folder)
  indoor/
    tennisstadiumv4.glb
```

The conversion script turns these into runtime GLBs under
`public/models/` (also gitignored):

```
public/models/
  stadium_grass.glb
  stadium_hard.glb
```
