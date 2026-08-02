"""Build textured player GLBs: Motion Cast T-pose mannequin + no-root clips.

Usage:
  Blender --background --python convert_players.py -- \
    <base_tpose.fbx> <clips_dir> <albedo.png> <output.glb>

Sibling roughness.png / normal.png next to the albedo are wired automatically.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

import bpy

CLIP_ALIASES = {
    r"3-IDLE": "idle",
    r"4-Moving from left to right-Speed2": "jog",
    r"8-Service ball": "serve",
    r"9-Hit 1 Speed1": "forehand",
    r"17-Backhand": "backhand",
    r"22-Backhand": "backhand",
    r"18-Smash": "smash",
}

REQUIRED_CLIPS = {"idle", "jog", "serve", "forehand", "backhand", "smash"}


def action_name_for(path: Path) -> str:
    stem = path.stem
    for pattern, name in CLIP_ALIASES.items():
        if re.search(pattern, stem, re.IGNORECASE):
            return name
    cleaned = re.sub(r"[^a-zA-Z0-9]+", "_", stem).strip("_").lower()
    return cleaned[:40] or "clip"


def clear_scene() -> None:
    bpy.ops.wm.read_factory_settings(use_empty=True)


def import_fbx(path: Path) -> None:
    bpy.ops.import_scene.fbx(
        filepath=str(path),
        automatic_bone_orientation=True,
        use_anim=True,
        ignore_leaf_bones=True,
    )


BODY_MATERIAL = "tennisly_body"


def body_material_names() -> set[str]:
    """Only the skinned mesh is the mannequin; the racket is a static prop that
    ships its own diffuse. Rename the body material to a stable id so the web
    renderer can recolour kits without guessing at vendor naming."""
    names: set[str] = set()
    for obj in bpy.data.objects:
        if obj.type != "MESH":
            continue
        if not any(m.type == "ARMATURE" for m in obj.modifiers):
            continue
        for index, slot in enumerate(obj.material_slots):
            if not slot.material:
                continue
            slot.material.name = BODY_MATERIAL if index == 0 else f"{BODY_MATERIAL}_{index}"
            names.add(slot.material.name)
    if not names:
        raise SystemExit("No skinned mesh found — cannot identify the body material")
    return names


def apply_albedo(path: Path) -> None:
    """Wire the full PBR set. Without the roughness map every surface shares one
    gloss value, which reads as moulded plastic under a strong key light."""
    if not path.exists():
        return
    targets = body_material_names()
    tex_dir = path.parent
    albedo_img = bpy.data.images.load(str(path))
    rough_path = tex_dir / "roughness.png"
    normal_path = tex_dir / "normal.png"
    rough_img = bpy.data.images.load(str(rough_path)) if rough_path.exists() else None
    normal_img = bpy.data.images.load(str(normal_path)) if normal_path.exists() else None
    if rough_img:
        rough_img.colorspace_settings.name = "Non-Color"
    if normal_img:
        normal_img.colorspace_settings.name = "Non-Color"

    for mat in bpy.data.materials:
        if mat.name not in targets:
            continue
        if not mat.use_nodes:
            mat.use_nodes = True
        nodes = mat.node_tree.nodes
        links = mat.node_tree.links
        bsdf = next((n for n in nodes if n.type == "BSDF_PRINCIPLED"), None)
        if not bsdf:
            continue
        tex = nodes.new("ShaderNodeTexImage")
        tex.image = albedo_img
        links.new(tex.outputs["Color"], bsdf.inputs["Base Color"])
        if "Metallic" in bsdf.inputs:
            bsdf.inputs["Metallic"].default_value = 0.0
        if rough_img:
            rough_tex = nodes.new("ShaderNodeTexImage")
            rough_tex.image = rough_img
            links.new(rough_tex.outputs["Color"], bsdf.inputs["Roughness"])
        if normal_img:
            normal_tex = nodes.new("ShaderNodeTexImage")
            normal_tex.image = normal_img
            normal_map = nodes.new("ShaderNodeNormalMap")
            links.new(normal_tex.outputs["Color"], normal_map.inputs["Color"])
            links.new(normal_map.outputs["Normal"], bsdf.inputs["Normal"])


def relink_textures(search_dirs: list[Path]) -> None:
    for directory in search_dirs:
        if directory.is_dir():
            bpy.ops.file.find_missing_files(directory=str(directory))

    # Blender matches on full filename. Some packs ship the racket diffuse as
    # .png under Blender/tex while the FBX still points at the original .jpg,
    # so fall back to matching on stem before giving up on the texture.
    candidates: dict[str, Path] = {}
    for directory in search_dirs:
        if not directory.is_dir():
            continue
        for found in directory.rglob("*"):
            if found.is_file():
                candidates.setdefault(found.stem.lower(), found)

    for img in bpy.data.images:
        if not img.filepath:
            continue
        current = Path(bpy.path.abspath(img.filepath))
        if current.exists():
            continue
        match = candidates.get(current.stem.lower())
        if match:
            img.filepath = str(match)
            img.reload()


def main() -> None:
    argv = sys.argv[sys.argv.index("--") + 1 :]
    base_fbx = Path(argv[0])
    clips_dir = Path(argv[1])
    albedo = Path(argv[2])
    out_path = Path(argv[3])
    clips = sorted(clips_dir.glob("*.fbx"))
    if not clips:
        raise SystemExit(f"No FBX clips in {clips_dir}")

    clear_scene()
    import_fbx(base_fbx)
    base_arm = next(o for o in bpy.data.objects if o.type == "ARMATURE")
    base_arm.name = "PlayerArmature"
    relink_textures([albedo.parent, base_fbx.parent])
    apply_albedo(albedo)

    # Drop any default action from T-pose import
    if base_arm.animation_data and base_arm.animation_data.action:
        act = base_arm.animation_data.action
        base_arm.animation_data.action = None
        bpy.data.actions.remove(act)

    kept: dict[str, bpy.types.Action] = {}
    for clip in clips:
        before_objs = set(bpy.data.objects)
        before_actions = set(bpy.data.actions)
        import_fbx(clip)
        new_actions = [a for a in bpy.data.actions if a not in before_actions]
        name = action_name_for(clip)
        if new_actions:
            action = new_actions[0]
            action.name = name
            kept[name] = action
        for obj in list(bpy.data.objects):
            if obj not in before_objs:
                bpy.data.objects.remove(obj, do_unlink=True)

    if not base_arm.animation_data:
        base_arm.animation_data_create()
    track = base_arm.animation_data.nla_tracks.new()
    track.name = "clips"
    frame = 1
    for name, action in sorted(kept.items()):
        track.strips.new(name, frame, action)
        frame += int(action.frame_range[1]) + 2
    # Leave the active action slot empty: the glTF NLA exporter skips any action
    # that is also assigned as active, which silently dropped `idle` from the GLB.
    base_arm.animation_data.action = None

    missing = REQUIRED_CLIPS - set(kept)
    if missing:
        raise SystemExit(f"Missing clips {sorted(missing)} in {clips_dir}")

    # Every clip import re-registers the prop textures, so resolve again before
    # packing or the export drops them with a stream of pack warnings.
    relink_textures([albedo.parent, base_fbx.parent])
    try:
        bpy.ops.file.pack_all()
    except RuntimeError:
        pass
    for img in bpy.data.images:
        if img.packed_file is None and img.filepath:
            try:
                img.pack()
            except RuntimeError:
                pass
    out_path.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.export_scene.gltf(
        filepath=str(out_path),
        export_format="GLB",
        export_animations=True,
        export_nla_strips=True,
        export_anim_single_armature=True,
        export_apply=False,
        export_yup=True,
        export_materials="EXPORT",
        export_image_format="AUTO",
    )
    print("EXPORTED", out_path, "actions", sorted(kept.keys()))


if __name__ == "__main__":
    main()
