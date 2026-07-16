"""Blender CLI: FBX + texture folder -> single GLB for Babylon.

Usage:
  Blender --background --python convert_wimbledon.py -- \
    <input.fbx> <textures_dir> <output.glb>
"""
import sys
import bpy

argv = sys.argv[sys.argv.index("--") + 1 :]
fbx_path, textures_dir, out_path = argv

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.fbx(filepath=fbx_path)

# Repoint any broken texture paths at the extracted Textures folder
bpy.ops.file.find_missing_files(directory=textures_dir)

# Pack textures so the GLB is fully self-contained
bpy.ops.file.pack_all()

for img in bpy.data.images:
    if img.packed_file is None and img.filepath:
        try:
            img.pack()
        except RuntimeError:
            pass

bpy.ops.export_scene.gltf(
    filepath=out_path,
    export_format="GLB",
    export_apply=True,
    export_yup=True,
    export_image_format="AUTO",
    export_materials="EXPORT",
)
print("EXPORTED", out_path)
