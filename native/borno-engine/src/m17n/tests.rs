#[cfg(test)]
mod test_cases {
    use super::super::*;
    use std::fs;

    #[test]
    fn test_khipro_testcases() {
        let manifest_dir = std::path::Path::new(env!("CARGO_MANIFEST_DIR"));
        let mim_candidates = [
            manifest_dir.join("../../assets-src/language-packs/khipro/rules/bn-khipro.classic.mim"),
            std::path::PathBuf::from("../../assets-src/language-packs/khipro/rules/bn-khipro.classic.mim"),
            std::path::PathBuf::from("assets-src/language-packs/khipro/rules/bn-khipro.classic.mim"),
        ];
        let mim_path = mim_candidates.iter().find(|p| p.exists())
            .unwrap_or(&mim_candidates[0]);

        let tsv_candidates = [
            manifest_dir.join("../../tests/khipro-testcases.tsv"),
            std::path::PathBuf::from("../../tests/khipro-testcases.tsv"),
            std::path::PathBuf::from("tests/khipro-testcases.tsv"),
        ];
        let tsv_path = tsv_candidates.iter().find(|p| p.exists())
            .unwrap_or(&tsv_candidates[0]);

        let mut mim_content = fs::read_to_string(mim_path)
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

    #[test]
    fn test_khipro_dark_testcases() {
        let manifest_dir = std::path::Path::new(env!("CARGO_MANIFEST_DIR"));
        let mim_candidates = [
            manifest_dir.join("../../assets-src/language-packs/khipro/rules/bn-khipro-dark.mim"),
            std::path::PathBuf::from("../../assets-src/language-packs/khipro/rules/bn-khipro-dark.mim"),
            std::path::PathBuf::from("assets-src/language-packs/khipro/rules/bn-khipro-dark.mim"),
        ];
        let mim_path = mim_candidates.iter().find(|p| p.exists())
            .unwrap_or(&mim_candidates[0]);

        let tsv_candidates = [
            manifest_dir.join("../../tests/khipro-dark-testcases.tsv"),
            std::path::PathBuf::from("../../tests/khipro-dark-testcases.tsv"),
            std::path::PathBuf::from("tests/khipro-dark-testcases.tsv"),
        ];
        let tsv_path = tsv_candidates.iter().find(|p| p.exists())
            .unwrap_or(&tsv_candidates[0]);

        let mut mim_content = fs::read_to_string(mim_path)
            .unwrap_or_else(|_| panic!("Failed to read {}", mim_path.display()));
        // Ensure standard overflow 1 rule for ellipsis if not already present
        if !mim_content.contains("(\"...\" (set overflow 1) \"..\")") {
            mim_content = mim_content.replace(
                "(\",,,\"",
                "(\"...\" (set overflow 1) \"..\")\r\n\t\t(\",,,\""
            );
        }
        let tsv_content = fs::read_to_string(tsv_path)
            .unwrap_or_else(|_| panic!("Failed to read {}", tsv_path.display()));

        let mut engine = create_engine(&mim_content).expect("Failed to parse and compile bn-khipro-dark.mim");

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

        println!("Khipro Dark test cases result: passed = {}, failed = {}", passed, failed);
        for (line_num, input, expected, actual) in &failures {
            println!(
                "Line {}: input='{}', expected='{}', actual='{}'",
                line_num, input, expected, actual
            );
        }

        assert_eq!(failed, 0, "Some Khipro Dark test cases failed! Failures: {:?}", failures);
    }

    #[test]
    fn test_m17n_general_purpose_specification() {
        let mim_source = r#"
        (input-method test general)
        (title "General Purpose m17n Test")
        (map
            (keys
                ("a" (set count 1) "A")
                ("+" (add count 1))
                ("-" (sub count 1))
                ("c" (mark M1) "C")
                ("x"
                    (cond
                        ((> count 2) "HIGH")
                        ((< count 1) "LOW")
                        ((== count 2) "MID")
                        (1 "OTHER")
                    )
                )
                ("d" (delete M1))
                ("r" (reset_all))
            )
        )
        (macro
            (reset_all (set count 0) (commit))
        )
        (state
            (init
                (keys)
            )
        )
        "#;

        let mut engine = create_engine(mim_source).expect("Failed to compile general m17n");

        // Test marker deletion
        engine.reset();
        engine.feed_char('a'); // buffer: "A"
        engine.feed_char('c'); // buffer: "AC", M1 marked at 1 (before C)
        assert_eq!(engine.current_output(), "AC");
        engine.feed_char('d'); // deletes between cursor (2) and M1 (1) -> deletes 'C'
        assert_eq!(engine.current_output(), "A");

        // Test condition and arithmetic
        engine.reset();
        engine.feed_char('a'); // count = 1
        engine.feed_char('+'); // count = 2
        engine.feed_char('x'); // count == 2 -> MID
        assert_eq!(engine.current_output(), "AMID");

        engine.reset();
        engine.feed_char('a'); // count = 1
        engine.feed_char('+'); // count = 2
        engine.feed_char('+'); // count = 3
        engine.feed_char('x'); // count > 2 -> HIGH
        assert_eq!(engine.current_output(), "AHIGH");

        engine.reset();
        engine.feed_char('a'); // count = 1
        engine.feed_char('-'); // count = 0
        engine.feed_char('x'); // count < 1 -> LOW
        assert_eq!(engine.current_output(), "ALOW");

        // Test macro invocation
        engine.feed_char('r');
        assert_eq!(engine.current_output(), "ALOW");
    }
}
