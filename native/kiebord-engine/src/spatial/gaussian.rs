#[derive(Debug, Clone)]
pub struct BivariateGaussian {
    pub mean_x: f32,
    pub mean_y: f32,
    pub sigma_x: f32,
    pub sigma_y: f32,
}

impl BivariateGaussian {
    pub fn new(mean_x: f32, mean_y: f32, sigma_x: f32, sigma_y: f32) -> Self {
        Self {
            mean_x,
            mean_y,
            sigma_x: if sigma_x <= 0.0 { 1.0 } else { sigma_x },
            sigma_y: if sigma_y <= 0.0 { 1.0 } else { sigma_y },
        }
    }

    /// Evaluates the normalized probability density of point (x, y) under this 2D Gaussian.
    pub fn score(&self, x: f32, y: f32) -> f32 {
        let dx = (x - self.mean_x) / self.sigma_x;
        let dy = (y - self.mean_y) / self.sigma_y;
        (-0.5 * (dx * dx + dy * dy)).exp()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_gaussian_center_and_decay() {
        let g = BivariateGaussian::new(100.0, 50.0, 20.0, 20.0);
        let center_score = g.score(100.0, 50.0);
        assert!((center_score - 1.0).abs() < 1e-4);

        let decay_score = g.score(120.0, 50.0);
        assert!(decay_score < center_score);
        assert!(decay_score > 0.0);
    }
}
