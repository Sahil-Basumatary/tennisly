package dev.sahilbasumatary.replayservice.trajectory;

import dev.sahilbasumatary.replayservice.domain.PlayerSide;
import dev.sahilbasumatary.replayservice.dto.response.ReplayFrame;
import dev.sahilbasumatary.replayservice.dto.response.ShotSummaryResponse;
import dev.sahilbasumatary.replayservice.physics.BallPathBuffer;
import dev.sahilbasumatary.replayservice.physics.Vector3;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Resamples the dense physics integration of a point down to a fixed frame rate and produces the
 * player kinematics for each frame. The hitter holds their contact position for the shot while the
 * receiver glides towards their next contact, giving a smooth, watchable rally.
 */
@Component
public class FrameAssembler {

    private static final double KILOMETRES_PER_HOUR_PER_METRE_PER_SECOND = 3.6;

    public List<ReplayFrame> framesForPoint(
            PointTrajectory point, double startTimeSeconds, int framesPerSecond) {
        int estimated = 0;
        for (ShotTrajectory shot : point.shots()) {
            estimated += Math.max(1, (int) Math.round(shot.flightSeconds() * framesPerSecond)) + 1;
        }
        List<ReplayFrame> frames = new ArrayList<>(estimated);
        double cursor = startTimeSeconds;
        double frameStep = 1.0 / framesPerSecond;
        for (ShotTrajectory shot : point.shots()) {
            double flight = shot.flightSeconds();
            int frameCount = Math.max(1, (int) Math.round(flight * framesPerSecond));
            Vector3 hitterPosition =
                    new Vector3(shot.contactPoint().x(), shot.contactPoint().y(), 0);
            int sampleIndex = 0;
            BallPathBuffer samples = shot.path();
            for (int frame = 0; frame <= frameCount; frame++) {
                double localTime = Math.min(flight, frame * frameStep);
                while (sampleIndex < samples.size() - 1
                        && samples.time(sampleIndex + 1) < localTime) {
                    sampleIndex++;
                }
                Vector3 ball = interpolateBall(samples, sampleIndex, localTime);
                double progress = flight == 0.0 ? 1.0 : localTime / flight;
                Vector3 receiverPosition = lerp(shot.receiverStart(), shot.receiverEnd(), progress);
                Vector3 home =
                        shot.hitterSide() == PlayerSide.HOME ? hitterPosition : receiverPosition;
                Vector3 away =
                        shot.hitterSide() == PlayerSide.AWAY ? hitterPosition : receiverPosition;
                frames.add(
                        new ReplayFrame(
                                round(cursor + localTime),
                                round(ball),
                                round(home),
                                round(away),
                                point.sequence(),
                                shot.shotIndex(),
                                shot.shotType()));
            }
            cursor += flight;
        }
        return frames;
    }

    public List<ShotSummaryResponse> shotSummaries(PointTrajectory point) {
        List<ShotSummaryResponse> summaries = new ArrayList<>(point.shots().size());
        for (ShotTrajectory shot : point.shots()) {
            summaries.add(
                    new ShotSummaryResponse(
                            point.sequence(),
                            shot.shotIndex(),
                            shot.shotType(),
                            shot.hitterSide(),
                            shot.spinType(),
                            round(shot.contactPoint()),
                            round(shot.landingPoint()),
                            round(
                                    shot.launchSpeedMetresPerSecond()
                                            * KILOMETRES_PER_HOUR_PER_METRE_PER_SECOND),
                            round(shot.apexHeightMetres()),
                            round(shot.flightSeconds())));
        }
        return summaries;
    }

    private Vector3 interpolateBall(BallPathBuffer samples, int sampleIndex, double localTime) {
        if (sampleIndex >= samples.size() - 1) {
            int last = samples.size() - 1;
            return new Vector3(samples.x(last), samples.y(last), samples.z(last));
        }
        double beforeTime = samples.time(sampleIndex);
        double afterTime = samples.time(sampleIndex + 1);
        double span = afterTime - beforeTime;
        double fraction = span <= 0.0 ? 0.0 : (localTime - beforeTime) / span;
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        return new Vector3(
                samples.x(sampleIndex)
                        + (samples.x(sampleIndex + 1) - samples.x(sampleIndex)) * clamped,
                samples.y(sampleIndex)
                        + (samples.y(sampleIndex + 1) - samples.y(sampleIndex)) * clamped,
                samples.z(sampleIndex)
                        + (samples.z(sampleIndex + 1) - samples.z(sampleIndex)) * clamped);
    }

    private Vector3 lerp(Vector3 from, Vector3 to, double fraction) {
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        return new Vector3(
                from.x() + (to.x() - from.x()) * clamped,
                from.y() + (to.y() - from.y()) * clamped,
                from.z() + (to.z() - from.z()) * clamped);
    }

    private Vector3 round(Vector3 value) {
        return new Vector3(round(value.x()), round(value.y()), round(value.z()));
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
