#[derive(Debug, Clone, PartialEq)]
pub struct GesturePoint {
    pub x: f32,
    pub y: f32,
    pub timestamp_ms: i64,
}

pub struct GestureDecoder;

impl GestureDecoder {
    /// Resamples an arbitrary length touch trajectory polyline into exactly `target_count` equidistant points
    pub fn resample_polyline(points: &[GesturePoint], target_count: usize) -> Vec<GesturePoint> {
        if points.is_empty() {
            return Vec::new();
        }
        if points.len() == 1 || target_count <= 1 {
            return vec![points[0].clone(); target_count.max(1)];
        }

        let mut total_length = 0.0f32;
        for i in 1..points.len() {
            let dx = points[i].x - points[i - 1].x;
            let dy = points[i].y - points[i - 1].y;
            total_length += (dx * dx + dy * dy).sqrt();
        }

        if total_length == 0.0 {
            return vec![points[0].clone(); target_count];
        }

        let interval = total_length / (target_count - 1) as f32;
        let mut resampled = Vec::with_capacity(target_count);
        resampled.push(points[0].clone());

        let mut accumulated = 0.0f32;
        let mut current_pt = points[0].clone();
        let mut i = 1;

        while i < points.len() && resampled.len() < target_count {
            let dx = points[i].x - current_pt.x;
            let dy = points[i].y - current_pt.y;
            let dist = (dx * dx + dy * dy).sqrt();

            if accumulated + dist >= interval {
                let t = (interval - accumulated) / dist;
                let next_pt = GesturePoint {
                    x: current_pt.x + t * dx,
                    y: current_pt.y + t * dy,
                    timestamp_ms: current_pt.timestamp_ms
                        + (t * (points[i].timestamp_ms - current_pt.timestamp_ms) as f32) as i64,
                };
                resampled.push(next_pt.clone());
                current_pt = next_pt;
                accumulated = 0.0;
            } else {
                accumulated += dist;
                current_pt = points[i].clone();
                i += 1;
            }
        }

        while resampled.len() < target_count {
            resampled.push(points.last().unwrap().clone());
        }

        resampled
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_resample_polyline() {
        let pts = vec![
            GesturePoint { x: 0.0, y: 0.0, timestamp_ms: 0 },
            GesturePoint { x: 100.0, y: 0.0, timestamp_ms: 100 },
        ];
        let resampled = GestureDecoder::resample_polyline(&pts, 5);
        assert_eq!(resampled.len(), 5);
        assert!((resampled[0].x - 0.0).abs() < 1e-3);
        assert!((resampled[1].x - 25.0).abs() < 1e-3);
        assert!((resampled[2].x - 50.0).abs() < 1e-3);
        assert!((resampled[3].x - 75.0).abs() < 1e-3);
        assert!((resampled[4].x - 100.0).abs() < 1e-3);
    }
}
