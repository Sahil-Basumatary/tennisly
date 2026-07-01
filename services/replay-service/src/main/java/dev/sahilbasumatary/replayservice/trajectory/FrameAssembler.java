package dev.sahilbasumatary.replayservice.trajectory;

import dev.sahilbasumatary.replayservice.domain.PlayerSide;
import dev.sahilbasumatary.replayservice.dto.response.ReplayFrame;
import dev.sahilbasumatary.replayservice.dto.response.ShotSummaryResponse;
import dev.sahilbasumatary.replayservice.physics.BallState;
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
        List<ReplayFrame> frames = new ArrayList<>();
        double cursor = startTimeSeconds;
        double frameStep = 1.0 / framesPerSecond;
        for (ShotTrajectory shot : point.shots()) {
            double flight = shot.flightSeconds();
            int frameCount = Math.max(1, (int) Math.round(flight * framesPerSecond));
            Vector3 hitterPosition = new Vector3(shot.contactPoint().x(), shot.contactPoint().y(), 0);
            int sampleIndex = 0;
            List<BallState> samples = shot.samples();
            for (int frame = 0; frame <= frameCount; frame++) {
                double localTime = Math.min(flight, frame * frameStep);
                while (sampleIndex < samples.size() - 1
                        && samples.get(sampleIndex + 1).timeSeconds() < localTime) {
                    sampleIndex++;
                }
                Vector3 ball = interpolateBall(samples, sampleIndex, localTime);
                double progress = flight == 0.0 ? 1.0 : localTime / flight;
                Vector3 receiverPosition =
                        lerp(shot.receiverStart(), shot.receiverEnd(), progress);
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

    private Vector3 interpolateBall(List<BallState> samples, int sampleIndex, double localTime) {
        if (sampleIndex >= samples.size() - 1) {
            return samples.get(samples.size() - 1).position();
        }
        BallState before = samples.get(sampleIndex);
        BallState after = samples.get(sampleIndex + 1);
        double span = after.timeSeconds() - before.timeSeconds();
        double fraction = span <= 0.0 ? 0.0 : (localTime - before.timeSeconds()) / span;
        return lerp(before.position(), after.position(), fraction);
    }

    private Vector3 lerp(Vector3 from, Vector3 to, double fraction) {
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        return from.add(to.subtract(from).scale(clamped));
    }

    private Vector3 round(Vector3 value) {
        return new Vector3(round(value.x()), round(value.y()), round(value.z()));
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
