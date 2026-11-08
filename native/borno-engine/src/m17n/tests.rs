#[cfg(test)]
mod test_cases {
    use super::super::*;
    use std::fs;

    #[test]
    fn test_khipro_testcases() {
        let manifest_dir = std::path::Path::new(env!("CARGO_MANIFEST_DIR"));
        let mim_path = manifest_dir.join("../../feature/ime/src/main/assets/rules/bn-khipro.classic.mim");
        let tsv_path = manifest_dir.join("../../tests/khipro-testcases.tsv");

        let mut mim_content = fs::read_to_string(&mim_path)
            .unwrap_or_else(|_| panic!("Failed to read {}", mim_path.display()));
        // Ensure standard overflow 1 rule for ellipsis if not already present
        if !mim_content.contains("(\"...\" (set overflow 1) \"..\")") {
            mim_content = mim_content.replace(
                "(biram\r\n\t\t(\",,,\"",
                "(biram\r\n\t\t(\"...\" (set overflow 1) \"..\")\r\n\t\t(\",,,\""
            ).replace(
                "(biram\n\t\t(\",,,\"",
                "(biram\n\t\t(\"...\" (set overflow 1) \"..\")\n\t\t(\",,,\""
            );
        }
        let tsv_content = fs::read_to_string(&tsv_path)
            .unwrap_or_else(|_| panic!("Failed to read {}", tsv_path.display()));

        let mut engine = create_engine(&mim_content).expect("Failed to parse and compile bn-khipro-classic.mim");

        let mut passed = 0;
        let mut failed = 0;
        let mut failures = Vec::new();

        for (line_idx, line) in tsv_content.lines().enumerate() {
            if line_idx == 0 || line.trim().is_empty() {
                continue; // Skip header
            }

            let parts: Vec<&str> = line.split('\t').collect();
            if parts.len() < 2 {
                continue;
            }

            let input = parts[0];
            let expected = parts[1];
            let actual = engine.transliterate(input);

            if actual == expected {
                passed += 1;
            } else {
                failed += 1;
                failures.push((line_idx + 1, input.to_string(), expected.to_string(), actual));
            }
        }

        println!("Khipro test cases result: passed = {}, failed = {}", passed, failed);
        for (line_num, input, expected, actual) in &failures {
            println!(
                "Line {}: input='{}', expected='{}', actual='{}'",
                line_num, input, expected, actual
            );
        }

        assert_eq!(failed, 0, "Some Khipro test cases failed! Failures: {:?}", failures);
    }
}
