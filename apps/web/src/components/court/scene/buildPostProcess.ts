import {
  type Camera,
  DefaultRenderingPipeline,
  ImageProcessingConfiguration,
  type Scene,
  SSAO2RenderingPipeline,
} from "@babylonjs/core";

export type PostProcessTier = "high" | "low";

export type CourtPostProcess = {
  pipeline: DefaultRenderingPipeline;
  ssao: SSAO2RenderingPipeline | null;
  tier: PostProcessTier;
  dispose: () => void;
};

/**
 * Broadcast grade of the render: ACES filmic curve, gentle highlight bloom on
 * the sunlit surfaces, edge sharpening, and a vignette that mimics a long lens.
 * Values stay conservative — TV feeds are contrasty, never glowing.
 */
export function buildPostProcess(scene: Scene, camera: Camera): CourtPostProcess {
  const tier = detectTier(scene);
  const pipeline = new DefaultRenderingPipeline("broadcastPipeline", true, scene, [camera]);

  pipeline.samples = tier === "high" ? 4 : 1;
  pipeline.fxaaEnabled = tier === "low";

  pipeline.imageProcessing.toneMappingEnabled = true;
  pipeline.imageProcessing.toneMappingType = ImageProcessingConfiguration.TONEMAPPING_ACES;
  pipeline.imageProcessing.exposure = 1.05;
  pipeline.imageProcessing.contrast = 1.08;
  pipeline.imageProcessing.vignetteEnabled = true;
  pipeline.imageProcessing.vignetteWeight = 1.1;
  pipeline.imageProcessing.vignetteStretch = 0.4;
  pipeline.imageProcessing.vignetteCameraFov = 0.9;

  pipeline.bloomEnabled = true;
  pipeline.bloomThreshold = 0.86;
  pipeline.bloomWeight = tier === "high" ? 0.22 : 0.14;
  pipeline.bloomKernel = 48;
  pipeline.bloomScale = 0.5;

  pipeline.sharpenEnabled = true;
  pipeline.sharpen.edgeAmount = 0.22;
  pipeline.sharpen.colorAmount = 1;

  const ssao = tier === "high" ? buildAmbientOcclusion(scene, camera) : null;

  return {
    pipeline,
    ssao,
    tier,
    dispose: () => {
      ssao?.dispose();
      pipeline.dispose();
    },
  };
}

/**
 * Contact darkening in creases, under arms, and where feet meet the court.
 * Without it the single-tone kit reads as a flat cut-out rather than a body.
 */
function buildAmbientOcclusion(scene: Scene, camera: Camera): SSAO2RenderingPipeline | null {
  if (!SSAO2RenderingPipeline.IsSupported) return null;
  // Full-res AO buffer: downsampled ratios resample the composited frame and
  // visibly soften courtside signage, which costs more than the AO gains.
  const ssao = new SSAO2RenderingPipeline("courtSSAO", scene, { ssaoRatio: 1, blurRatio: 1 });
  ssao.samples = 16;
  ssao.radius = 0.6;
  ssao.totalStrength = 0.8;
  ssao.expensiveBlur = true;
  // Players and court only — AO across the whole bowl is wasted depth sampling.
  ssao.maxZ = 45;
  scene.postProcessRenderPipelineManager.attachCamerasToRenderPipeline("courtSSAO", camera);
  return ssao;
}

/**
 * Integrated GPUs choke on MSAA + bloom at stadium scale, so sniff the renderer
 * string and drop to a cheaper stack rather than shipping a 20fps feed.
 */
function detectTier(scene: Scene): PostProcessTier {
  const maxSamples = scene.getEngine().getCaps().maxSamples;
  if (maxSamples !== undefined && maxSamples < 4) return "low";
  const cores = typeof navigator !== "undefined" ? (navigator.hardwareConcurrency ?? 4) : 4;
  return cores >= 4 ? "high" : "low";
}
